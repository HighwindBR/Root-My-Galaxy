package dev.busung.s25uroot

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class InstallPhase {
    Checking,
    Ready,
    Downloading,
    Exploiting,
    LoadingKernelSu,
    Installed,
    Failed,
}

private enum class PayloadSource { Remote, Local }

data class InstallUiState(
    val phase: InstallPhase = InstallPhase.Checking,
    val message: String = "",
    val probeOutput: String = "",
    val log: String = "",
) {
    val busy: Boolean
        get() = phase in setOf(
            InstallPhase.Checking,
            InstallPhase.Downloading,
            InstallPhase.Exploiting,
            InstallPhase.LoadingKernelSu,
        )

}

data class TargetCatalogUiState(
    val loading: Boolean = false,
    val profiles: List<TargetProfile> = emptyList(),
    val error: String? = null,
)

private data class CommandResult(val code: Int, val output: String)

/**
 * Payloads are truncated to a fixed release size, so a rebuild of a target --
 * or a different target padded to the same size -- has exactly the length of
 * whatever is already staged, and would keep running in its place.
 */
internal fun stagedFileIsCurrent(staged: File, source: File): Boolean {
    if (!staged.exists()) return false
    val stagedDigest = sha256OrNull(staged) ?: return false
    return stagedDigest == sha256OrNull(source)
}

private fun sha256OrNull(file: File): String? = runCatching {
    file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}.getOrNull()

class InstallViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PayloadRepository(application)
    private val historyStore = InstallHistoryStore(application)
    private val mutableState = MutableStateFlow(InstallUiState())
    private val mutableHistory = MutableStateFlow(historyStore.closeInterruptedRuns())
    private val mutableTargetCatalog = MutableStateFlow(TargetCatalogUiState())
    private var discoveryJob: Job? = null
    private var installJob: Job? = null
    private var activeHistoryEntry: InstallHistoryEntry? = null
    private var localPayloadUris: Map<String, Uri> = emptyMap()

    @Volatile
    private var activeRunShizuku: Boolean? = null

    /** Per-attempt reboot-userspace override; null means fall back to the global pref. */
    @Volatile
    private var activeRunRebootUserspace: Boolean? = null

    val state: StateFlow<InstallUiState> = mutableState.asStateFlow()
    val history: StateFlow<List<InstallHistoryEntry>> = mutableHistory.asStateFlow()
    val targetCatalog: StateFlow<TargetCatalogUiState> = mutableTargetCatalog.asStateFlow()

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = viewModelScope.launch(Dispatchers.IO) {
            mutableTargetCatalog.value = TargetCatalogUiState(loading = true)
            mutableTargetCatalog.value = try {
                val profiles = repository.loadTargets()
                TargetCatalogUiState(profiles = profiles)
            } catch (e: Exception) {
                TargetCatalogUiState(error = e.message ?: "Unknown error")
            }
        }
    }

    fun cancelDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        mutableTargetCatalog.value = TargetCatalogUiState()
    }

    fun setLocalPayloadUris(uris: Map<String, Uri>) {
        localPayloadUris = uris
    }

    /**
     * Convenience entry-point used by [InstallActivity].
     * Resolves a [TargetProfile] from [profileId] (if not null and not the
     * local-payload sentinel) then delegates to [install].
     */
    fun installByProfileId(profileId: String?, rebootUserspace: Boolean? = null) {
        if (installJob?.isActive == true) return
        installJob = viewModelScope.launch(Dispatchers.IO) {
            val profile: TargetProfile? = when {
                profileId == null || profileId == LOCAL_PROFILE_ID -> null
                else -> try {
                    repository.resolveTarget(profileId)
                } catch (e: Exception) {
                    mutableState.value = mutableState.value.copy(
                        phase = InstallPhase.Failed,
                        message = e.message ?: "Unknown error",
                    )
                    return@launch
                }
            }
            install(profile, rebootUserspace)
        }
    }

    fun install(
        profile: TargetProfile? = null,
        rebootUserspace: Boolean? = null,
    ) {
        if (installJob?.isActive == true) return
        installJob = viewModelScope.launch(Dispatchers.IO) {
            startHistory()
            try {
                activeRunRebootUserspace = rebootUserspace
                // Snapshot the Shizuku preference at the start of the run so the
                // change cannot mix Shizuku and standalone execution between steps.
                activeRunShizuku = AppPreferences.shizukuMode(app)

                setPhase(InstallPhase.Checking, app.getString(R.string.install_preparing))
                if (shizukuEnabled()) {
                    appendLog(app.getString(R.string.log_shizuku_prepare))
                    if (!ShizukuController.isRunning() && !ShizukuController.pingUntilRunning()) {
                        error(app.getString(R.string.error_shizuku_unavailable))
                    }
                    if (!ShizukuController.isGranted() && !ShizukuController.requestPermission()) {
                        error(app.getString(R.string.error_shizuku_permission))
                    }
                    appendLog(app.getString(R.string.log_shizuku_permission))
                }
                val localExploitUri = localPayloadUris[PAYLOAD_EXPLOIT]
                val syntheticProfile: TargetProfile? = if (localExploitUri != null) {
                    TargetProfile(
                        profileId = LOCAL_PROFILE_ID,
                        displayName = "",
                        models = setOf(DeviceSnapshot.current().model),
                        kernelVersions = setOf(DeviceSnapshot.current().kernelVersion),
                        exploit = RemoteArtifact("", -1L),
                        kernelSu = RemoteArtifact("", -1L),
                        requiresFreshP0Session = false,
                    )
                } else {
                    null
                }
                val verified: VerifiedPayloads = when {
                    syntheticProfile != null -> {
                        val exploitUri = localExploitUri!!
                        setPhase(InstallPhase.Downloading, app.getString(R.string.install_preparing))
                        repository.stageLocalExploit(syntheticProfile, exploitUri) {
                            appendLog("[*] $it")
                        }
                    }

                    profile != null -> {
                        setPhase(InstallPhase.Downloading, app.getString(R.string.install_preparing))
                        updateHistoryProfile(profile.profileId)
                        repository.download(profile) { appendLog("[*] $it") }
                    }

                    else -> {
                        setPhase(InstallPhase.Checking, app.getString(R.string.install_preparing))
                        val snapshot = DeviceSnapshot.current()
                        val resolved = try {
                            repository.resolveTarget(snapshot)
                        } catch (_: Exception) {
                            mutableState.value = mutableState.value.copy(
                                phase = InstallPhase.Ready,
                                message = app.getString(R.string.install_preparing),
                            )
                            return@launch
                        }
                        updateHistoryProfile(resolved.profileId)
                        repository.download(resolved) { appendLog("[*] $it") }
                    }
                }
                setPhase(InstallPhase.Exploiting, app.getString(R.string.install_preparing))
                executeExploit(verified.exploit, verified.profile.requiresFreshP0Session)

                val kernelSuUri = localPayloadUris[PAYLOAD_KERNELSU]
                if (kernelSuUri != null) {
                    repository.stageLocalKernelSu(verified, kernelSuUri) { appendLog("[*] $it") }
                }

                setPhase(InstallPhase.LoadingKernelSu, app.getString(R.string.install_preparing))
                installKernelSu(verified)

                if (activeRunRebootUserspace == true ||
                    (activeRunRebootUserspace == null && AppPreferences.rebootAfterInstall(app))
                ) {
                    appendLog(app.getString(R.string.log_reboot_userspace))
                    runHelper("--reboot-userspace")
                }

                setPhase(InstallPhase.Installed, app.getString(R.string.log_install_complete))
                finishHistory(InstallRunResult.Succeeded)
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                mutableState.value = mutableState.value.copy(
                    phase = InstallPhase.Failed,
                    message = msg,
                )
                appendLog("[!] Installation failed: $msg")
                finishHistory(InstallRunResult.Failed)
            } finally {
                activeRunShizuku = null
                activeRunRebootUserspace = null
            }
        }
    }

    private suspend fun executeExploit(payload: File, requiresFreshP0Session: Boolean) {
        val shizuku = shizukuEnabled()
        val logFile = if (shizuku) File(SHIZUKU_LOG_PATH) else File(app.filesDir, "exploit.log")
        if (shizuku) {
            ShizukuController.exec(arrayOf("rm", "-f", SHIZUKU_LOG_PATH)).waitFor()
        } else {
            logFile.delete()
        }
        val helper = helperFile()
        if (!shizuku) {
            require(helper.canExecute()) { app.getString(R.string.error_helper_unavailable) }
        }
        val logPrefix = mutableState.value.log
        val bootToken = currentBootToken()
        val process = if (shizuku) {
            val stagedHelper = shizukuStage(nativeHelperFile(), SHIZUKU_HELPER_PATH, "755")
            val stagedPayload = shizukuStage(payload, SHIZUKU_PAYLOAD_PATH, "755")
            ShizukuController.exec(
                arrayOf(
                    stagedHelper.absolutePath,
                    "--run-payload",
                    stagedPayload.absolutePath,
                    stagedHelper.absolutePath,
                    SHIZUKU_LOG_PATH,
                ),
                shizukuEnvironment(
                    bootToken,
                    stagedPayload.absolutePath,
                    stagedHelper.absolutePath,
                ),
            )
        } else {
            ProcessBuilder(
                listOf(
                    helper.absolutePath,
                    "--run-payload",
                    payload.absolutePath,
                    helper.absolutePath,
                    logFile.absolutePath,
                ),
            )
                .redirectErrorStream(true)
                .apply {
                    environment().putAll(
                        shizukuEnvironment(
                            bootToken,
                            payload.absolutePath,
                            helper.absolutePath,
                        ).associate { entry ->
                            val eq = entry.indexOf('=')
                            if (eq >= 0) entry.substring(0, eq) to entry.substring(eq + 1)
                            else entry to ""
                        },
                    )
                }
                .start()
        }
        var lastLogSize = -1L
        var lastLogChangeAt = SystemClock.elapsedRealtime()
        val startedAt = lastLogChangeAt
        val earlyOutput = StringBuilder()
        try {
            while (process.isAlive) {
                drainProcessOutput(process, earlyOutput)
                val logSize = logFile.length()
                val now = SystemClock.elapsedRealtime()
                if (logSize != lastLogSize) {
                    lastLogSize = logSize
                    lastLogChangeAt = now
                }
                val rawLog = logFile.readTextIfPresent()
                publishExploitLog(logPrefix, rawLog)
                require(now - lastLogChangeAt < EXPLOIT_STALL_MILLIS) {
                    app.getString(R.string.error_exploit_stalled)
                }
                require(now - startedAt < EXPLOIT_TOTAL_MILLIS) {
                    app.getString(R.string.error_exploit_timeout)
                }
                delay(LOG_POLL_INTERVAL)
            }
            drainProcessOutput(process, earlyOutput)
            val rawLog = if (shizuku) {
                File(SHIZUKU_LOG_PATH).readTextIfPresent()
            } else {
                logFile.readTextIfPresent()
            }
            publishExploitLog(logPrefix, rawLog)
            val exitCode = process.waitFor()
            require(exitCode == 0) {
                app.getString(
                    R.string.error_payload_exit,
                    exitCode,
                    earlyOutput.toString().trim().takeIf(String::isNotBlank)?.let { ": $it" } ?: "",
                )
            }
            cacheP0Offset(bootToken, rawLog)
            appendLog(app.getString(R.string.log_bootstrap_root))
            require(detectInstalled()) { app.getString(R.string.error_success_marker) }
        } finally {
            if (process.isAlive) {
                process.destroy()
                delay(500.milliseconds)
                if (process.isAlive) process.destroyForcibly()
            }
        }
    }

    private fun drainProcessOutput(process: Process, buffer: StringBuilder): String {
        drainStream(process.inputStream, buffer)
        return buffer.toString()
    }

    private fun drainStream(stream: InputStream, buffer: StringBuilder) {
        if (stream.available() > 0) {
            val bytes = stream.readNBytes(minOf(stream.available(), MAX_DRAIN_BYTES))
            buffer.append(bytes.toString(Charsets.UTF_8))
        }
    }

    private fun publishExploitLog(logPrefix: String, rawLog: String) {
        val clean = stripAnsi(rawLog)
        val lines = clean.lines().takeLast(MAX_LOG_LINES)
        mutableState.value = mutableState.value.copy(log = logPrefix + lines.joinToString("\n"))
    }

    private suspend fun installKernelSu(payloads: VerifiedPayloads) {
        val ksud = payloads.kernelSu
        appendLog(app.getString(R.string.log_kernelsu_source, ksud.absolutePath))

        val stagedKsud: File
        if (shizukuEnabled()) {
            stagedKsud = shizukuStage(ksud, SHIZUKU_KSUD_PATH, "755")
            val stagedHelper = shizukuStage(nativeHelperFile(), SHIZUKU_HELPER_PATH, "755")
            val stageCommand = "${shellQuote(stagedKsud.absolutePath)} install --path ${shellQuote(SHIZUKU_KSUD_STAGE_PATH)}"
            val stage = runHelper("-c", stageCommand)
            require(stage.code == 0) {
                app.getString(R.string.error_ksu_stage, stage.output.takeIf(String::isNotBlank) ?: stage.code.toString())
            }
        } else {
            stagedKsud = ksud
            val stageCommand = "${shellQuote(stagedKsud.absolutePath)} install --path ${shellQuote(app.filesDir.absolutePath + "/ksud-stage")}"
            val stage = runHelper("-c", stageCommand)
            require(stage.code == 0) {
                app.getString(R.string.error_ksu_stage, stage.output.takeIf(String::isNotBlank) ?: stage.code.toString())
            }
        }
        appendLog(app.getString(R.string.log_ksu_staged))

        val lateLoad = runHelper("--late-load")
        require(lateLoad.code == 0) {
            app.getString(
                R.string.error_ksu_verify,
                lateLoad.code,
                lateLoad.output.takeIf(String::isNotBlank) ?: "",
            )
        }
        val command = "\"${shellQuote(app.applicationInfo.nativeLibraryDir + "/libksud.so")}\""
        val result = runHelper("-c", command)
        require(result.code == 0) {
            app.getString(
                R.string.error_ksu_verify,
                result.code,
                result.output.takeIf(String::isNotBlank) ?: "",
            )
        }
        appendLog(app.getString(R.string.log_ksu_control_verified))
    }

    private fun detectInstalled(): Boolean {
        val receipt = File(app.filesDir, INSTALL_RECEIPT)
        if (!receipt.exists()) return false
        return try {
            receipt.readText(Charsets.UTF_8).contains(RECEIPT_VERIFIED)
        } catch (_: Exception) {
            false
        }
    }

    private fun storeInstallReceipt() {
        val receipt = File(app.filesDir, INSTALL_RECEIPT)
        try {
            val bootToken = currentBootToken() ?: return
            receipt.writeText("$RECEIPT_BOOT_TOKEN=$bootToken\n$RECEIPT_VERIFIED=true", Charsets.UTF_8)
        } catch (_: Exception) {
            error(app.getString(R.string.error_receipt))
        }
    }

    private fun currentBootToken(): String? = runCatching {
        File("/proc/sys/kernel/random/boot_id").readText(Charsets.UTF_8).trim()
    }.getOrElse {
        null
    }

    private fun cachedP0Offset(bootToken: String?): String? {
        if (bootToken == null) return null
        val prefs = app.getSharedPreferences(P0_OFFSET_PREFS, android.content.Context.MODE_PRIVATE)
        val stored = prefs.getString(bootToken, null) ?: return null
        return stored.takeIf { it.matches(Regex("[0-9a-fx]+")) }
    }

    private fun cacheP0Offset(bootToken: String?, rawLog: String) {
        if (bootToken == null) return
        val match = P0_OFFSET_PATTERN.find(rawLog) ?: return
        val offset = match.groupValues[1]
        app.getSharedPreferences(P0_OFFSET_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(bootToken, offset)
            .apply()
    }

    private fun startHistory() {
        val entry = historyStore.create()
        activeHistoryEntry = entry
        publishHistory(entry)
    }

    private fun updateHistory(transform: (InstallHistoryEntry) -> InstallHistoryEntry) {
        val entry = activeHistoryEntry ?: return
        val updated = transform(entry)
        activeHistoryEntry = updated
        historyStore.save(updated)
        publishHistory(updated)
    }

    private fun updateHistoryLog() =
        updateHistory { it.copy(log = mutableState.value.log) }

    private fun updateHistoryProfile(profileId: String) =
        updateHistory { it.copy(profileId = profileId) }

    private fun finishHistory(result: InstallRunResult) =
        updateHistory { it.copy(completedAtMillis = System.currentTimeMillis(), result = result, log = mutableState.value.log) }

    private fun publishHistory(entry: InstallHistoryEntry) {
        mutableHistory.value = buildList {
            add(entry)
            addAll(mutableHistory.value.filter { it.id != entry.id })
        }
    }

    private fun setPhase(phase: InstallPhase, message: String) {
        mutableState.value = mutableState.value.copy(phase = phase, message = message)
        updateHistoryLog()
    }

    private fun appendLog(line: String) {
        val current = mutableState.value.log
        val lines = current.lines()
        val trimmed = if (lines.size >= MAX_LOG_LINES) {
            lines.takeLast(MAX_LOG_LINES - 1).joinToString("\n")
        } else {
            current
        }
        mutableState.value = mutableState.value.copy(
            log = if (trimmed.isEmpty()) line else "$trimmed\n$line",
        )
        updateHistoryLog()
    }

    private fun error(message: String): Nothing {
        throw IllegalStateException(message)
    }

    private fun helperFile(): File =
        File(app.applicationInfo.nativeLibraryDir, "libcve43499root.so").also {
            require(it.exists()) { app.getString(R.string.error_helper_unavailable) }
        }

    private fun nativeHelperFile() = File(app.applicationInfo.nativeLibraryDir, "libcve43499root.so")

    private fun shizukuEnabled(): Boolean = activeRunShizuku ?: AppPreferences.shizukuMode(app)

    private fun shizukuStage(source: File, target: String, mode: String): File {
        val result = ShizukuController.exec(arrayOf("cp", source.absolutePath, target)).waitFor()
        require(result == 0) {
            app.getString(R.string.error_shizuku_stage, source.name, "cp exited $result")
        }
        ShizukuController.exec(arrayOf("chmod", mode, target)).waitFor()
        return File(target)
    }

    private fun shizukuEnvironment(
        bootToken: String?,
        payloadPath: String,
        helperPath: String,
    ) = buildList {
        add("EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS")
        add("P0_ATTEMPT_TIMEOUT_SEC=$P0_ATTEMPT_TIMEOUT_SEC")
        add("EXPLOIT_ATTEMPT_TIMEOUT_SEC=$EXPLOIT_ATTEMPT_TIMEOUT_SEC")
        add("CVE43499_ROOT_HELPER=$helperPath")
        add("LD_PRELOAD=$payloadPath")
        if (bootToken != null) {
            cachedP0Offset(bootToken)?.let { add("$P0_OFFSET_ENV=$it") }
        }
    }.toTypedArray()

    /**
     * Runs the bootstrap helper for a short management command. Unlike the
     * exploit run there is no log file to poll, so output is drained inline
     * and a hard deadline guards against a helper that never exits — without
     * this, a hung `--late-load` leaves the install stuck in LoadingKernelSu
     * indefinitely.
     */
    private suspend fun runHelper(vararg arguments: String): CommandResult {
        val helper = helperFile()
        val process = if (shizukuEnabled()) {
            ShizukuController.exec(arrayOf(helper.absolutePath) + arguments)
        } else {
            ProcessBuilder(listOf(helper.absolutePath) + arguments)
                .redirectErrorStream(true)
                .start()
        }
        val captured = StringBuilder()
        val startedAt = SystemClock.elapsedRealtime()
        try {
            while (process.isAlive) {
                drainProcessOutput(process, captured)
                require(SystemClock.elapsedRealtime() - startedAt < HELPER_TIMEOUT_MILLIS) {
                    app.getString(
                        R.string.error_helper_timeout,
                        captured.toString().trim().takeIf(String::isNotBlank)
                            ?.let { ": $it" } ?: "",
                    )
                }
                delay(HELPER_POLL_INTERVAL)
            }
            drainProcessOutput(process, captured)
            val exitCode = process.waitFor()
            return CommandResult(exitCode, stripAnsi(captured.toString().trim()))
        } finally {
            if (process.isAlive) {
                process.destroy()
                delay(500.milliseconds)
                if (process.isAlive) process.destroyForcibly()
            }
        }
    }

    private fun shellQuote(value: String) = "'${value.replace("'", "'\\''")}'"

    fun refresh() {
        mutableHistory.value = historyStore.closeInterruptedRuns()
    }

    fun loadTargetCatalog() {
        startDiscovery()
    }

    fun deleteHistoryEntries(ids: Set<String>) {
        ids.forEach { historyStore.delete(it) }
        mutableHistory.value = historyStore.load()
    }

    fun cancel() {
        installJob?.cancel()
        installJob = null
        if (mutableState.value.busy) {
            mutableState.value = mutableState.value.copy(
                phase = InstallPhase.Failed,
                message = "Installation cancelled",
            )
        }
    }

    fun clearError() {
        if (mutableState.value.phase == InstallPhase.Failed) {
            mutableState.value = InstallUiState(phase = InstallPhase.Ready)
        }
    }

    private fun stripAnsi(text: String): String =
        text.replace(Regex("\u001B\\[[0-9;]*[mGKHF]"), "")

    private fun File.readTextIfPresent(): String =
        if (exists()) runCatching { readText(Charsets.UTF_8) }.getOrDefault("") else ""

    companion object {
        internal const val PAYLOAD_EXPLOIT = "exploit"
        internal const val PAYLOAD_KERNELSU = "kernelsu"
        internal const val LOCAL_PROFILE_ID = "__local__"

        private const val INSTALL_RECEIPT = "install_receipt"
        private const val RECEIPT_BOOT_TOKEN = "boot_token"
        private const val RECEIPT_VERIFIED = "verified"

        private const val P0_OFFSET_PREFS = "p0_offset_cache"
        private const val P0_OFFSET_ENV = "CVE43499_P0_OFFSET"
        private val P0_OFFSET_PATTERN = Regex("""p0_offset=([0-9a-fx]+)""")

        private const val SHIZUKU_LOG_PATH = "/data/local/tmp/ksu-exploit.log"
        private const val SHIZUKU_HELPER_PATH = "/data/local/tmp/ksu-helper"
        private const val SHIZUKU_PAYLOAD_PATH = "/data/local/tmp/ksu-payload"
        private const val SHIZUKU_KSUD_PATH = "/data/local/tmp/ksud-s25u-kdp"
        private const val SHIZUKU_KSUD_STAGE_PATH = "/data/local/tmp/.ksud-stage"

        private const val EXPLOIT_ATTEMPTS = "6"
        private const val P0_ATTEMPT_TIMEOUT_SEC = "18"
        private const val EXPLOIT_ATTEMPT_TIMEOUT_SEC = "90"

        private val EXPLOIT_STALL_MILLIS = 30.seconds.inWholeMilliseconds
        private val EXPLOIT_TOTAL_MILLIS = 10.minutes.inWholeMilliseconds
        private val HELPER_TIMEOUT_MILLIS = 60.seconds.inWholeMilliseconds
        private val SHIZUKU_LOG_POLL_INTERVAL = 250.milliseconds.inWholeMilliseconds
        private val LOG_POLL_INTERVAL = 500.milliseconds.inWholeMilliseconds
        private val HELPER_POLL_INTERVAL = 100.milliseconds.inWholeMilliseconds

        private const val MAX_LOG_LINES = 200
        private const val MAX_DRAIN_BYTES = 65_536
        private const val MAX_EARLY_OUTPUT_BYTES = 4_096
    }
}

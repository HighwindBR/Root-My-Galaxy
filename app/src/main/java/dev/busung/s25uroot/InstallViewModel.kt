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
                val profiles = repository.discoverTargets()
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

                setPhase(InstallPhase.Checking, app.getString(R.string.log_checking))
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
                    TargetProfile.synthetic()
                } else {
                    null
                }
                val verified: VerifiedPayloads = when {
                    syntheticProfile != null -> {
                        val exploitUri = localExploitUri!!
                        setPhase(InstallPhase.Downloading, app.getString(R.string.log_staging_local))
                        repository.stageLocalExploit(syntheticProfile, exploitUri) {
                            appendLog("[*] $it")
                        }
                    }

                    profile != null -> {
                        setPhase(InstallPhase.Downloading, app.getString(R.string.log_downloading))
                        updateHistoryProfile(profile.id)
                        repository.downloadAndVerify(profile) { appendLog("[*] $it") }
                    }

                    else -> {
                        setPhase(InstallPhase.Checking, app.getString(R.string.log_checking))
                        repository.verifyExisting() ?: run {
                            mutableState.value = mutableState.value.copy(
                                phase = InstallPhase.Ready,
                                message = app.getString(R.string.log_ready),
                            )
                            return@launch
                        }
                    }
                }
                setPhase(InstallPhase.Exploiting, app.getString(R.string.log_exploiting))
                executeExploit(verified.exploit, verified.profile.requiresFreshP0Session)

                val kernelSuUri = localPayloadUris[PAYLOAD_KERNELSU]
                if (kernelSuUri != null) {
                    repository.stageLocalKernelSu(verified, kernelSuUri) { appendLog("[*] $it") }
                }

                setPhase(InstallPhase.LoadingKernelSu, app.getString(R.string.log_ksu_loading))
                installKernelSu(verified)

                if (activeRunRebootUserspace == true ||
                    (activeRunRebootUserspace == null && AppPreferences.rebootAfterInstall(app))
                ) {
                    appendLog(app.getString(R.string.log_userspace_reboot))
                    runHelper("--reboot-userspace")
                }

                setPhase(InstallPhase.Installed, app.getString(R.string.log_done))
                finishHistory(InstallRunResult.Success)
            } catch (e: Exception) {
                val msg = e.message ?: app.getString(R.string.error_unknown)
                mutableState.value = mutableState.value.copy(
                    phase = InstallPhase.Failed,
                    message = msg,
                )
                appendLog(app.getString(R.string.log_failed, msg))
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
                    requiresFreshP0Session,
                ),
            )
        } else {
            val processBuilder = ProcessBuilder(
                helper.absolutePath,
                "--run-payload",
                payload.absolutePath,
                helper.absolutePath,
                logFile.absolutePath,
            ).redirectErrorStream(true)
            processBuilder.environment().apply {
                put("EXPLOIT_ATTEMPTS", EXPLOIT_ATTEMPTS)
                put("P0_ATTEMPT_TIMEOUT_SEC", P0_ATTEMPT_TIMEOUT_SEC)
                put("EXPLOIT_ATTEMPT_TIMEOUT_SEC", EXPLOIT_ATTEMPT_TIMEOUT_SEC)
                // Fresh-P0 payloads refuse a forced/retained cross-process slide, so
                // a cached offset only guarantees a failed run. Don't feed it.
                if (!requiresFreshP0Session) {
                    cachedP0Offset(bootToken)?.let { put(P0_OFFSET_ENV, it) }
                }
            }
            processBuilder.start()
        }
        val captured = StringBuilder()
        val readLog: () -> String = if (shizuku) {
            { drainProcessOutput(process, captured) }
        } else {
            // Keep draining stdout while polling: if the helper fills the OS
            // pipe buffer it blocks on write and stops making log progress,
            // which would trip the stall detector spuriously.
            { drainProcessOutput(process, captured); logFile.readTextIfPresent() }
        }

        // Drain stdout continuously: the helper relays the full exploit log to
        // its stdout pipe, which would otherwise fill (~64KB) and block the
        // helper from exiting even after a successful exploit.
        val earlyOutputBuf = StringBuilder()
        val drainThread: Thread? = if (shizuku) null else Thread {
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    if (earlyOutputBuf.length < MAX_EARLY_OUTPUT_BYTES) {
                        earlyOutputBuf.append(line).append('\n')
                    }
                }
            } catch (_: Exception) {
            }
        }
        drainThread?.isDaemon = true
        drainThread?.start()

        try {
            val startedAt = SystemClock.elapsedRealtime()
            var lastProgressAt = startedAt
            var lastRawLog = ""
            while (process.isAlive) {
                val rawLog = readLog()
                if (rawLog != lastRawLog) {
                    if (!requiresFreshP0Session) cacheP0Offset(bootToken, rawLog)
                    publishExploitLog(logPrefix, rawLog)
                    lastRawLog = rawLog
                    lastProgressAt = SystemClock.elapsedRealtime()
                }
                val now = SystemClock.elapsedRealtime()
                require(now - lastProgressAt < EXPLOIT_STALL_MILLIS) {
                    app.getString(R.string.error_exploit_stalled)
                }
                require(now - startedAt < EXPLOIT_TOTAL_MILLIS) {
                    app.getString(R.string.error_exploit_timeout)
                }
                delay(if (shizuku) SHIZUKU_LOG_POLL_INTERVAL else LOG_POLL_INTERVAL)
            }

            val exitCode = process.waitFor()
            drainThread?.join(2_000)
            val rawLog = readLog()
            if (!requiresFreshP0Session) cacheP0Offset(bootToken, rawLog)
            publishExploitLog(logPrefix, rawLog)
            val earlyOutput = if (shizuku) {
                drainProcessOutput(process, captured).trim()
            } else {
                earlyOutputBuf.toString().trim()
            }
            require(exitCode == 0) {
                app.getString(
                    R.string.error_payload_exit,
                    exitCode,
                    earlyOutput.takeIf(String::isNotBlank)?.let { " ($it)" } ?: "",
                )
            }
            require(rawLog.contains("exploit completed") && rawLog.contains("done=1 root=1")) {
                app.getString(R.string.error_success_marker)
            }
        } finally {
            if (process.isAlive) {
                process.destroy()
                delay(500.milliseconds)
                if (process.isAlive) process.destroyForcibly()
            }
        }
        appendLog(app.getString(R.string.log_bootstrap_root))
    }

    private fun drainProcessOutput(process: Process, buffer: StringBuilder): String {
        return try {
            drainStream(process.inputStream, buffer)
            drainStream(process.errorStream, buffer)
            buffer.toString()
        } catch (_: Throwable) {
            buffer.toString()
        }
    }

    private fun drainStream(stream: InputStream, buffer: StringBuilder) {
        val available = stream.available()
        if (available <= 0) return
        val bytes = ByteArray(available.coerceAtMost(MAX_DRAIN_BYTES))
        val read = stream.read(bytes)
        if (read > 0) buffer.append(String(bytes, 0, read, Charsets.UTF_8))
    }

    private fun publishExploitLog(logPrefix: String, rawLog: String) {
        val lines = rawLog
            .lines()
            .dropLastWhile(String::isBlank)
            .takeLast(MAX_LOG_LINES)
            .joinToString("\n")
        mutableState.value = mutableState.value.copy(
            log = buildString {
                if (logPrefix.isNotBlank()) {
                    append(logPrefix)
                    append("\n")
                }
                append(lines)
            }.trim(),
        )
        updateHistoryLog()
    }

    private suspend fun installKernelSu(payloads: VerifiedPayloads) {
        startKernelLogCapture()
        if (shizukuEnabled()) {
            shizukuStage(payloads.kernelSu, SHIZUKU_KSUD_PATH, "755")
            shizukuStage(payloads.kernelSu, SHIZUKU_KSUD_STAGE_PATH, "755")
            appendLog(app.getString(R.string.log_ksu_staged))
        } else {
            val source = shellQuote(payloads.kernelSu.absolutePath)
            val stageCommand =
                "/system/bin/cp $source $SHIZUKU_KSUD_PATH && " +
                    "/system/bin/cp $source $SHIZUKU_KSUD_STAGE_PATH && " +
                    "/system/bin/chmod 755 $SHIZUKU_KSUD_PATH $SHIZUKU_KSUD_STAGE_PATH"
            val stage = runHelper("-c", stageCommand)
            require(stage.code == 0) { app.getString(R.string.error_ksu_stage, stage.output) }
            appendLog(app.getString(R.string.log_ksu_staged))
        }

        val lateLoad = runHelper("--late-load")
        require(lateLoad.code == 0) {
            app.getString(R.string.error_ksu_verify, lateLoad.code, lateLoad.output)
        }
        if (lateLoad.output.isNotBlank()) appendLog(lateLoad.output)
        storeInstallReceipt()
        appendLog(app.getString(R.string.log_ksu_control_verified))
    }

    private suspend fun startKernelLogCapture() {
        val command =
            "rm -f /data/local/tmp/dmesg-capture.log /data/local/tmp/dmesg-capture.meta; " +
                "(dmesg -w > /data/local/tmp/dmesg-capture.log 2>&1 &); " +
                "{ date; id; cat /proc/self/attr/current; } > /data/local/tmp/dmesg-capture.meta 2>&1"
        val result = runHelper("-c", command)
        appendLog(
            if (result.code == 0) "kernel log capture started"
            else "kernel log capture failed code=${result.code} ${result.output.takeLast(120)}",
        )
    }

    private fun detectInstalled(): Boolean {
        if (NativeProbe.isKernelSuActive()) return true
        val bootToken = currentBootToken() ?: return false
        val receipt = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
        return receipt.getString(RECEIPT_BOOT_TOKEN, null) == bootToken &&
            receipt.getBoolean(RECEIPT_VERIFIED, false)
    }

    private fun storeInstallReceipt() {
        val bootToken = currentBootToken() ?: error(app.getString(R.string.error_boot_id))
        val stored = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
            .edit()
            .putString(RECEIPT_BOOT_TOKEN, bootToken)
            .putBoolean(RECEIPT_VERIFIED, true)
            .commit()
        require(stored) { app.getString(R.string.error_receipt) }
    }

    private fun currentBootToken(): String? = runCatching {
        File("/proc/sys/kernel/random/boot_id")
            .readText(Charsets.US_ASCII)
            .trim()
            .takeIf(String::isNotBlank)
    }.getOrNull()

    private fun cachedP0Offset(bootToken: String?): String? {
        bootToken ?: return null
        return app.getSharedPreferences(P0_OFFSET_PREFS, Application.MODE_PRIVATE)
            .getString(bootToken, null)
            ?.takeIf(String::isNotBlank)
    }

    private fun cacheP0Offset(bootToken: String?, rawLog: String) {
        bootToken ?: return
        val match = P0_OFFSET_PATTERN.find(rawLog) ?: return
        val offset = match.groupValues[1].takeIf(String::isNotBlank) ?: return
        app.getSharedPreferences(P0_OFFSET_PREFS, Application.MODE_PRIVATE)
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
        updateHistory { it.copy(result = result) }

    private fun publishHistory(entry: InstallHistoryEntry) {
        val current = mutableHistory.value.toMutableList()
        val idx = current.indexOfFirst { it.id == entry.id }
        if (idx >= 0) current[idx] = entry else current.add(0, entry)
        mutableHistory.value = current
    }

    private fun setPhase(phase: InstallPhase, message: String) {
        mutableState.value = mutableState.value.copy(phase = phase, message = message)
        appendLog("[*] $message")
    }

    private fun appendLog(line: String) {
        val cleanLine = stripAnsi(line).trim()
        if (cleanLine.isBlank()) return
        mutableState.value = mutableState.value.copy(
            log = (mutableState.value.log + "\n" + cleanLine).trim(),
        )
        updateHistoryLog()
    }

    private fun error(message: String): Nothing {
        throw IllegalStateException(message)
    }

    private fun helperFile(): File =
        if (shizukuEnabled()) {
            shizukuStage(nativeHelperFile(), SHIZUKU_HELPER_PATH, "755")
        } else {
            nativeHelperFile()
        }

    private fun nativeHelperFile() = File(app.applicationInfo.nativeLibraryDir, "libcve43499root.so")

    private fun shizukuEnabled(): Boolean = activeRunShizuku ?: AppPreferences.shizukuMode(app)

    private fun shizukuStage(source: File, target: String, mode: String): File {
        val staged = File(target)
        if (stagedFileIsCurrent(staged, source)) return staged
        runCatching {
            ShizukuController.writeFile(target, mode, source.inputStream())
        }.onFailure { error ->
            error(
                app.getString(R.string.error_shizuku_stage, target, error.message.orEmpty()),
            )
        }
        return staged
    }

    private fun shizukuEnvironment(
        bootToken: String?,
        payloadPath: String,
        helperPath: String,
        requiresFreshP0Session: Boolean,
    ): Array<String> = buildList {
        add("EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS")
        add("P0_ATTEMPT_TIMEOUT_SEC=$P0_ATTEMPT_TIMEOUT_SEC")
        add("EXPLOIT_ATTEMPT_TIMEOUT_SEC=$EXPLOIT_ATTEMPT_TIMEOUT_SEC")
        add("CVE43499_ROOT_HELPER=$helperPath")
        add("LD_PRELOAD=$payloadPath")
        if (!requiresFreshP0Session) {
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

    fun cancel() {
        installJob?.cancel()
        installJob = null
        if (mutableState.value.busy) {
            mutableState.value = mutableState.value.copy(
                phase = InstallPhase.Failed,
                message = app.getString(R.string.error_cancelled),
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
        private const val PAYLOAD_EXPLOIT = "exploit"
        private const val PAYLOAD_KERNELSU = "kernelsu"

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

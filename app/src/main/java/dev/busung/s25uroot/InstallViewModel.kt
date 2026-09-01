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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// ---------------------------------------------------------------------------
// Phase enum
// ---------------------------------------------------------------------------

enum class InstallPhase {
    Checking,
    Ready,
    Downloading,
    Exploiting,
    LoadingKernelSu,
    Installed,
    Done,
    Failed,
}

private enum class PayloadSource { Remote, Local }

// ---------------------------------------------------------------------------
// UI state – now contains all fields expected by MainActivity
// ---------------------------------------------------------------------------

data class InstallUiState(
    val phase: InstallPhase = InstallPhase.Checking,
    /** Short one-line status shown in the status card. */
    val statusMessage: String = "",
    /** Float in 0f..1f while a determinate step is running, null otherwise. */
    val progress: Float? = null,
    val probeOutput: String = "",
    val log: String = "",
    /** Device snapshot captured at start-up / on refresh. */
    val device: DeviceSnapshot? = null,
    val isRooted: Boolean = false,
    val kernelSuVersion: String? = null,
    val androidVersion: String? = null,
    val securityPatch: String? = null,
    /** The profile the user explicitly selected in the target sheet; null = auto. */
    val selectedProfile: TargetProfile? = null,
    /**
     * Non-null while a deep-link / notification intent carrying a profile ID
     * is waiting to be confirmed before the install starts.
     */
    val pendingInstallRequest: String? = null,
) {
    val busy: Boolean
        get() = phase in setOf(
            InstallPhase.Checking,
            InstallPhase.Downloading,
            InstallPhase.Exploiting,
            InstallPhase.LoadingKernelSu,
        )

    // Legacy alias – used in some internal call-sites
    val message: String get() = statusMessage
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

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class InstallViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PayloadRepository(application)
    private val historyStore = InstallHistoryStore(application)

    // -----------------------------------------------------------------------
    // Internal mutable state
    // -----------------------------------------------------------------------

    private val mutableUiState = MutableStateFlow(InstallUiState())
    private val mutableHistory = MutableStateFlow(historyStore.closeInterruptedRuns())
    private val mutableTargetCatalog = MutableStateFlow(TargetCatalogUiState())

    // Settings-derived StateFlows – store the enum internally, expose String externally
    private val mutableAccentColorEnum = MutableStateFlow(AppPreferences.accentColor(app))
    private val mutableThemeModeEnum = MutableStateFlow(AppPreferences.themeMode(app))
    private val mutableAdvancedMode = MutableStateFlow(AppPreferences.advancedMode(app))
    private val mutableShizukuMode = MutableStateFlow(AppPreferences.shizukuMode(app))
    private val mutableAutoReroot = MutableStateFlow(AppPreferences.autoReroot(app))
    private val mutableLocalPayloadMode = MutableStateFlow(AppPreferences.localPayloadMode(app))

    private var discoveryJob: Job? = null
    private var installJob: Job? = null
    private var activeHistoryEntry: InstallHistoryEntry? = null
    private var localPayloadUris: Map<String, Uri> = emptyMap()

    @Volatile
    private var activeRunShizuku: Boolean? = null

    /** Per-attempt reboot-userspace override; null means fall back to the global pref. */
    @Volatile
    private var activeRunRebootUserspace: Boolean? = null

    // -----------------------------------------------------------------------
    // Public StateFlow API expected by MainActivity
    // -----------------------------------------------------------------------

    /** Full install UI state (phase, status, progress, device info, …). */
    val uiState: StateFlow<InstallUiState> = mutableUiState.asStateFlow()

    /** Flat history list. */
    val installHistory: StateFlow<List<InstallHistoryEntry>> = mutableHistory.asStateFlow()

    /**
     * Currently selected accent-color token as a stored-value string
     * (e.g. "dynamic", "blue", …).  MainActivity and SettingsScreen consume String.
     */
    val accentColor: StateFlow<String> = mutableAccentColorEnum
        .map { it.storedValue }
        .stateIn(viewModelScope, SharingStarted.Eagerly, mutableAccentColorEnum.value.storedValue)

    /**
     * Currently selected theme mode as a stored-value string
     * ("system" / "light" / "dark").  MainActivity and SettingsScreen consume String.
     */
    val themeMode: StateFlow<String> = mutableThemeModeEnum
        .map { it.storedValue }
        .stateIn(viewModelScope, SharingStarted.Eagerly, mutableThemeModeEnum.value.storedValue)

    /** Whether advanced (manual payload selection) mode is on. */
    val advancedMode: StateFlow<Boolean> = mutableAdvancedMode.asStateFlow()

    /** Whether Shizuku execution is enabled. */
    val shizukuMode: StateFlow<Boolean> = mutableShizukuMode.asStateFlow()

    /** Whether auto-reroot on boot is enabled. */
    val autoReroot: StateFlow<Boolean> = mutableAutoReroot.asStateFlow()

    /** Whether a local payload file should be used instead of downloading. */
    val localPayloadMode: StateFlow<Boolean> = mutableLocalPayloadMode.asStateFlow()

    // Legacy aliases retained for call-sites that haven't been migrated
    val state: StateFlow<InstallUiState> = uiState
    val history: StateFlow<List<InstallHistoryEntry>> = installHistory
    val targetCatalog: StateFlow<TargetCatalogUiState> = mutableTargetCatalog.asStateFlow()

    // -----------------------------------------------------------------------
    // Init – populate device snapshot
    // -----------------------------------------------------------------------

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val snap = runCatching { DeviceSnapshot.current() }.getOrNull()
            val rooted = runCatching { NativeProbe.isKernelSuActive() }.getOrDefault(false)
            mutableUiState.value = mutableUiState.value.copy(
                device = snap,
                isRooted = rooted,
                kernelSuVersion = null,
                androidVersion = snap?.androidVersion,
                securityPatch = snap?.securityPatch,
                phase = InstallPhase.Ready,
                statusMessage = app.getString(R.string.install_preparing),
            )
        }
    }

    // -----------------------------------------------------------------------
    // Actions expected by MainActivity
    // -----------------------------------------------------------------------

    /** Begin the root installation (optionally for a specific profile). */
    fun startRoot(profile: TargetProfile? = null) {
        install(profile)
    }

    /** Cancel the running session. */
    fun stopSession() {
        cancel()
    }

    /**
     * Delete a single history entry by id.
     * MainActivity references this as `deleteHistoryEntry(id)`.
     */
    fun deleteHistoryEntry(id: String) {
        deleteHistoryEntries(setOf(id))
    }

    /** Delete all history entries. */
    fun deleteAllHistoryEntries() {
        val ids = mutableHistory.value.map { it.id }.toSet()
        deleteHistoryEntries(ids)
    }

    /**
     * Called when the user picks a profile in the target-selection sheet.
     * Persists as [InstallUiState.selectedProfile].
     */
    fun selectProfile(profile: TargetProfile?) {
        mutableUiState.value = mutableUiState.value.copy(selectedProfile = profile)
    }

    /**
     * Stores a pending deep-link / notification install request so MainActivity
     * can show a confirmation dialog before the install starts.
     */
    fun setPendingInstallRequest(profileId: String?) {
        mutableUiState.value = mutableUiState.value.copy(pendingInstallRequest = profileId)
    }

    /** Clears the pending install request after the user has accepted/rejected it. */
    fun consumePendingInstallRequest() {
        mutableUiState.value = mutableUiState.value.copy(pendingInstallRequest = null)
    }

    // Settings setters ---------------------------------------------------------

    fun setAdvancedMode(enabled: Boolean) {
        AppPreferences.setAdvancedMode(app, enabled)
        mutableAdvancedMode.value = enabled
    }

    fun setShizukuMode(enabled: Boolean) {
        AppPreferences.setShizukuMode(app, enabled)
        mutableShizukuMode.value = enabled
    }

    fun setAutoReroot(enabled: Boolean) {
        AppPreferences.setAutoReroot(app, enabled)
        mutableAutoReroot.value = enabled
    }

    fun setLocalPayloadMode(enabled: Boolean) {
        AppPreferences.setLocalPayloadMode(app, enabled)
        mutableLocalPayloadMode.value = enabled
    }

    /** Accepts the stored-value string (e.g. "blue", "dynamic") from the UI. */
    fun setAccentColor(color: String) {
        val enum = AccentColor.fromStoredValue(color)
        AppPreferences.setAccentColor(app, enum)
        mutableAccentColorEnum.value = enum
    }

    /** Accepts the stored-value string ("system", "light", "dark") from the UI. */
    fun setThemeMode(mode: String) {
        val enum = AppThemeMode.fromStoredValue(mode)
        AppPreferences.setThemeMode(app, enum)
        mutableThemeModeEnum.value = enum
    }

    // -----------------------------------------------------------------------
    // Discovery / catalog
    // -----------------------------------------------------------------------

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
                    mutableUiState.value = mutableUiState.value.copy(
                        phase = InstallPhase.Failed,
                        statusMessage = e.message ?: "Unknown error",
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
                            mutableUiState.value = mutableUiState.value.copy(
                                phase = InstallPhase.Ready,
                                statusMessage = app.getString(R.string.install_preparing),
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

                // Refresh root status after successful install
                val nowRooted = runCatching { NativeProbe.isKernelSuActive() }.getOrDefault(true)
                mutableUiState.value = mutableUiState.value.copy(
                    isRooted = nowRooted,
                    kernelSuVersion = null,
                )

                setPhase(InstallPhase.Installed, app.getString(R.string.log_install_complete))
                finishHistory(InstallRunResult.Succeeded)
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                mutableUiState.value = mutableUiState.value.copy(
                    phase = InstallPhase.Failed,
                    statusMessage = msg,
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
        val logPrefix = mutableUiState.value.log
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
        mutableUiState.value = mutableUiState.value.copy(log = logPrefix + lines.joinToString("\n"))
    }

    private suspend fun installKernelSu(payloads: VerifiedPayloads) {
        val ksud = payloads.kernelSu
        appendLog(app.getString(R.string.log_kernelsu_source, ksud.absolutePath))

        val stagedKsud: File
        if (shizukuEnabled()) {
            stagedKsud = shizukuStage(ksud, SHIZUKU_KSUD_PATH, "755")
            shizukuStage(nativeHelperFile(), SHIZUKU_HELPER_PATH, "755")
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

    @Suppress("unused")
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
    }.getOrElse { null }

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

    // -----------------------------------------------------------------------
    // History helpers
    // -----------------------------------------------------------------------

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
        updateHistory { it.copy(log = mutableUiState.value.log) }

    private fun updateHistoryProfile(profileId: String) =
        updateHistory { it.copy(profileId = profileId) }

    private fun finishHistory(result: InstallRunResult) =
        updateHistory { it.copy(completedAtMillis = System.currentTimeMillis(), result = result, log = mutableUiState.value.log) }

    private fun publishHistory(entry: InstallHistoryEntry) {
        mutableHistory.value = buildList {
            add(entry)
            addAll(mutableHistory.value.filter { it.id != entry.id })
        }
    }

    // -----------------------------------------------------------------------
    // State helpers
    // -----------------------------------------------------------------------

    private fun setPhase(phase: InstallPhase, message: String) {
        mutableUiState.value = mutableUiState.value.copy(phase = phase, statusMessage = message)
        updateHistoryLog()
    }

    private fun appendLog(line: String) {
        val current = mutableUiState.value.log
        val lines = current.lines()
        val trimmed = if (lines.size >= MAX_LOG_LINES) {
            lines.takeLast(MAX_LOG_LINES - 1).joinToString("\n")
        } else {
            current
        }
        mutableUiState.value = mutableUiState.value.copy(
            log = if (trimmed.isEmpty()) line else "$trimmed\n$line",
        )
        updateHistoryLog()
    }

    private fun error(message: String): Nothing {
        throw IllegalStateException(message)
    }

    // -----------------------------------------------------------------------
    // Native helpers
    // -----------------------------------------------------------------------

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
     * Runs the bootstrap helper for a short management command.
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

    // -----------------------------------------------------------------------
    // Public misc
    // -----------------------------------------------------------------------

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
        if (mutableUiState.value.busy) {
            mutableUiState.value = mutableUiState.value.copy(
                phase = InstallPhase.Failed,
                statusMessage = "Installation cancelled",
            )
        }
    }

    fun clearError() {
        if (mutableUiState.value.phase == InstallPhase.Failed) {
            mutableUiState.value = mutableUiState.value.copy(
                phase = InstallPhase.Ready,
                statusMessage = "",
            )
        }
    }

    private fun stripAnsi(text: String): String =
        text.replace(Regex("\u001B\\[[0-9;]*[mGKHF]"), "")

    private fun File.readTextIfPresent(): String =
        if (exists()) runCatching { readText(Charsets.UTF_8) }.getOrDefault("") else ""

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

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
        private val LOG_POLL_INTERVAL = 500.milliseconds.inWholeMilliseconds
        private val HELPER_POLL_INTERVAL = 100.milliseconds.inWholeMilliseconds

        private const val MAX_LOG_LINES = 200
        private const val MAX_DRAIN_BYTES = 65_536
    }
}

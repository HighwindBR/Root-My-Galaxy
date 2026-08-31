package dev.busung.s25uroot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.busung.s25uroot.ui.theme.RootMyGalaxyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val installViewModel by viewModels<InstallViewModel>()
    private var resumedOnce = false
    private var accentColor by mutableStateOf(AccentColor.Dynamic)
    private var themeMode by mutableStateOf(AppThemeMode.System)
    private var advancedMode by mutableStateOf(false)
    private var shizukuMode by mutableStateOf(false)
    private var localPayloadMode by mutableStateOf(false)
    private var rebootAfterInstall by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        accentColor = AppPreferences.accentColor(this)
        themeMode = AppPreferences.themeMode(this)
        advancedMode = AppPreferences.advancedMode(this)
        shizukuMode = AppPreferences.shizukuMode(this)
        localPayloadMode = AppPreferences.localPayloadMode(this)
        rebootAfterInstall = AppPreferences.rebootAfterInstall(this)
        setContent {
            RootMyGalaxyTheme(accentColor = accentColor, themeMode = themeMode) {
                RootApp(
                    installViewModel = installViewModel,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    advancedMode = advancedMode,
                    shizukuMode = shizukuMode,
                    localPayloadMode = localPayloadMode,
                    rebootAfterInstall = rebootAfterInstall,
                    onAccentColorChanged = { color ->
                        AppPreferences.setAccentColor(this, color)
                        accentColor = color
                    },
                    onThemeModeChanged = { mode ->
                        AppPreferences.setThemeMode(this, mode)
                        themeMode = mode
                    },
                    onAdvancedModeChanged = { enabled ->
                        AppPreferences.setAdvancedMode(this, enabled)
                        advancedMode = enabled
                    },
                    onShizukuModeChanged = { enabled ->
                        AppPreferences.setShizukuMode(this, enabled)
                        shizukuMode = enabled
                    },
                    onLocalPayloadModeChanged = { enabled ->
                        AppPreferences.setLocalPayloadMode(this, enabled)
                        localPayloadMode = enabled
                    },
                    onRebootAfterInstallChanged = { enabled ->
                        AppPreferences.setRebootAfterInstall(this, enabled)
                        rebootAfterInstall = enabled
                    },
                    openInstaller = { profileId, payloadUris, rebootUserspace ->
                        val installer = Intent(this, InstallActivity::class.java)
                            .putExtra(InstallActivity.EXTRA_INSTALL_REQUEST_ID, UUID.randomUUID().toString())
                        if (profileId != null) {
                            installer.putExtra(InstallActivity.EXTRA_PROFILE_ID, profileId)
                        }
                        payloadUris.forEach { (key, uri) ->
                            installer.putExtra(
                                InstallActivity.EXTRA_LOCAL_PAYLOAD_PREFIX + key,
                                uri.toString(),
                            )
                        }
                        if (rebootUserspace != null) {
                            installer.putExtra(InstallActivity.EXTRA_REBOOT_USERSPACE, rebootUserspace)
                        }
                        startActivity(installer)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (resumedOnce) installViewModel.refresh() else resumedOnce = true
    }
}

private enum class AppPage(@StringRes val label: Int, val icon: ImageVector) {
    Overview(R.string.nav_overview, Icons.Rounded.Home),
    History(R.string.nav_history, Icons.Rounded.History),
    Settings(R.string.nav_settings, Icons.Rounded.Settings),
}

private data class LanguageOption(@StringRes val label: Int, val tag: String)

private enum class CompatibilityWarning {
    Device,
    KernelVersion,
}

private val languageOptions = listOf(
    LanguageOption(R.string.language_system, ""),
    LanguageOption(R.string.language_korean, "ko"),
    LanguageOption(R.string.language_english, "en"),
    LanguageOption(R.string.language_japanese, "ja"),
    LanguageOption(R.string.language_chinese, "zh-CN"),
    LanguageOption(R.string.language_chinese_traditional, "zh-TW"),
    LanguageOption(R.string.language_turkish, "tr"),
    LanguageOption(R.string.language_brazillian_portuguese, "pt-BR"),
    LanguageOption(R.string.language_russian, "ru"),
    LanguageOption(R.string.language_vietnamese, "vi"),
    LanguageOption(R.string.language_uzbek, "uz"),
)

private const val KERNEL_SU_MANAGER_URL =
    "https://github.com/tiann/KernelSU/releases/latest"
private const val KERNEL_SU_MANAGER_PACKAGE = "me.weishu.kernelsu"
private const val KERNEL_SU_HOME_URL = "https://kernelsu.org/"
private const val SHIZUKU_MANAGER_PACKAGE = "moe.shizuku.manager"
private const val SHIZUKU_MANAGER_URL = "https://github.com/thedjchi/Shizuku/releases/"
private const val KERNEL_SU_MANAGER_API_URL =
    "https://api.github.com/repos/tiann/KernelSU/releases/latest"

private fun isKernelSuManagerInstalled(context: Context): Boolean =
    context.packageManager.getLaunchIntentForPackage(KERNEL_SU_MANAGER_PACKAGE) != null

private fun openKernelSuManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(KERNEL_SU_MANAGER_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val connection = java.net.URL(KERNEL_SU_MANAGER_API_URL).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", context.packageName)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val release = org.json.JSONObject(body)
                val assets = release.getJSONArray("assets")

                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }

                val targetUrl = apkUrl ?: KERNEL_SU_MANAGER_URL
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                }
            } catch (_: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(KERNEL_SU_MANAGER_URL)))
                }
            }
        }
    }
}

private fun openShizukuManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_MANAGER_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_MANAGER_URL)))
    }
}

@Composable
private fun RootApp(
    installViewModel: InstallViewModel,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    advancedMode: Boolean,
    shizukuMode: Boolean,
    localPayloadMode: Boolean,
    rebootAfterInstall: Boolean,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAdvancedModeChanged: (Boolean) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
    onLocalPayloadModeChanged: (Boolean) -> Unit,
    onRebootAfterInstallChanged: (Boolean) -> Unit,
    openInstaller: (String?, Map<String, Uri>, Boolean?) -> Unit,
) {
    val installState by installViewModel.state.collectAsStateWithLifecycle()
    val history by installViewModel.history.collectAsStateWithLifecycle()
    val targetCatalog by installViewModel.targetCatalog.collectAsStateWithLifecycle()
    var selectedPage by remember { mutableStateOf(AppPage.Overview) }
    var showInstallConfirmation by remember { mutableStateOf(false) }
    var confirmRebootUserspace by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<TargetProfile?>(null) }
    var compatibilityWarning by remember { mutableStateOf<CompatibilityWarning?>(null) }
    var showLocalPayloadPicker by remember { mutableStateOf<TargetProfile?>(null) }
    val device = remember { DeviceSnapshot.current() }
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var updateCardDismissed by remember { mutableStateOf(false) }
    val checkForUpdate: () -> Unit = {
        if (!updateStatus.busy) {
            updateStatus = UpdateStatus.Checking
            scope.launch {
                val info = AppUpdater.fetchLatestRelease()
                updateStatus = when {
                    info == null -> UpdateStatus.Failed
                    AppUpdater.isUpdateAvailable(info.versionName, BuildConfig.VERSION_NAME) ->
                        UpdateStatus.Available(info)
                    else -> UpdateStatus.UpToDate
                }
            }
        }
    }
    val startDownload: (UpdateInfo) -> Unit = { info ->
        val apkUrl = info.apkUrl
        if (apkUrl == null) {
            AppUpdater.openReleasesPage(context)
        } else {
            updateStatus = UpdateStatus.Downloading(info, 0f)
            scope.launch {
                val apk = AppUpdater.downloadApk(context, apkUrl) { progress ->
                    updateStatus = UpdateStatus.Downloading(info, progress)
                }
                if (apk == null || !AppUpdater.installApk(context, apk)) {
                    Toast.makeText(context, context.getString(R.string.updater_download_failed), Toast.LENGTH_SHORT).show()
                    AppUpdater.openReleasesPage(context)
                }
                updateStatus = UpdateStatus.Available(info)
            }
        }
    }
    LaunchedEffect(Unit) { checkForUpdate() }

    val openLocalPayloadPicker: (String?) -> Unit = { profileId ->
        val profile = targetCatalog.profiles.firstOrNull { it.profileId == profileId }
        if (profile == null) {
            showLocalPayloadPicker = TargetProfile(
                profileId = InstallViewModel.LOCAL_PROFILE_ID,
                displayName = "",
                models = setOf(device.model),
                kernelVersions = setOf(device.kernelVersion),
                exploit = RemoteArtifact("", -1L),
                kernelSu = RemoteArtifact("", -1L),
                requiresFreshP0Session = false,
            )
        } else {
            showLocalPayloadPicker = profile
        }
    }


    if (showTargetPicker) {
        TargetSelectionSheet(
            device = device,
            catalog = targetCatalog,
            onDismiss = { showTargetPicker = false },
            onRetry = installViewModel::loadTargetCatalog,
            onNext = { profile ->
                selectedProfile = profile
                showTargetPicker = false
                compatibilityWarning = when {
                    !profile.matchesDevice(device) -> CompatibilityWarning.Device
                    !profile.matchesKernelVersion(device) -> CompatibilityWarning.KernelVersion
                    else -> null
                }
                if (compatibilityWarning == null) {
                    confirmRebootUserspace = rebootAfterInstall
                    showInstallConfirmation = true
                }
            },
        )
    }

    compatibilityWarning?.let { warning ->
        val profile = selectedProfile ?: return@let
        AlertDialog(
            onDismissRequest = {
                compatibilityWarning = null
                showTargetPicker = true
            },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(
                    stringResource(when (warning) {
                        CompatibilityWarning.Device -> R.string.device_mismatch_title
                        CompatibilityWarning.KernelVersion -> R.string.kernel_version_mismatch_title
                    }),
                )
            },
            text = {
                Text(
                    when (warning) {
                        CompatibilityWarning.Device -> stringResource(
                            R.string.device_mismatch_body,
                            device.model,
                            profile.supportedModels,
                        )
                        CompatibilityWarning.KernelVersion -> stringResource(
                            R.string.kernel_version_mismatch_body,
                            device.kernelVersion,
                            profile.supportedKernelVersions,
                        )
                    },
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        clickHaptic(view)
                        compatibilityWarning = when (warning) {
                            CompatibilityWarning.Device -> if (!profile.matchesKernelVersion(device)) {
                                CompatibilityWarning.KernelVersion
                            } else {
                                null
                            }
                            CompatibilityWarning.KernelVersion -> null
                        }
                        if (compatibilityWarning == null) {
                            confirmRebootUserspace = rebootAfterInstall
                            showInstallConfirmation = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clickHaptic(view)
                        compatibilityWarning = null
                        showTargetPicker = true
                    },
                ) {
                    Text(stringResource(R.string.action_back))
                }
            },
        )
    }

    if (showInstallConfirmation) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmation = false },
            icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(stringResource(R.string.install_confirm_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.install_confirm_body))
                    Text(
                        stringResource(R.string.install_confirm_source),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    // Per-attempt reboot userspace toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = confirmRebootUserspace,
                                role = Role.Switch,
                                onValueChange = {
                                    clickHaptic(view)
                                    confirmRebootUserspace = it
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.reboot_userspace_this_attempt),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.reboot_userspace_this_attempt_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = confirmRebootUserspace,
                            onCheckedChange = null,
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    clickHaptic(view)
                    showInstallConfirmation = false
                    openInstaller(selectedProfile?.profileId, emptyMap(), confirmRebootUserspace)
                    selectedProfile = null
                }) {
                    Text(stringResource(R.string.action_use_online_payload))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clickHaptic(view)
                    showInstallConfirmation = false
                    openLocalPayloadPicker(selectedProfile?.profileId)
                    selectedProfile = null
                }) {
                    Text(stringResource(R.string.action_use_local_payload))
                }
            },
        )
    }

    showLocalPayloadPicker?.let { profile ->
        LocalPayloadPicker(
            profileId = profile.profileId,
            globalRebootUserspace = rebootAfterInstall,
            onDismiss = { showLocalPayloadPicker = null },
            onConfirm = { exploitUri: Uri, kernelSuUri: Uri?, rebootUserspace: Boolean ->
                showLocalPayloadPicker = null
                openInstaller(
                    profile.profileId,
                    buildMap {
                        put(InstallViewModel.PAYLOAD_EXPLOIT, exploitUri)
                        kernelSuUri?.let { put(InstallViewModel.PAYLOAD_KERNELSU, it) }
                    },
                    rebootUserspace,
                )
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                AppPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = {
                            clickHaptic(view)
                            selectedPage = page
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        icon = { Icon(page.icon, contentDescription = null) },
                        label = { Text(stringResource(page.label)) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        AnimatedContent(targetState = selectedPage, label = "page") { page ->
            when (page) {
                AppPage.Overview -> OverviewPage(
                    padding = padding,
                    device = device,
                    installState = installState,
                    updateStatus = updateStatus,
                    updateCardDismissed = updateCardDismissed,
                    localPayloadMode = localPayloadMode,
                    onDismissUpdateCard = { updateCardDismissed = true },
                    onStartDownload = startDownload,
                    onInstall = {
                        selectedProfile = null
                        if (localPayloadMode) {
                            selectedProfile = null
                            showLocalPayloadPicker = TargetProfile(
                                profileId = InstallViewModel.LOCAL_PROFILE_ID,
                                displayName = "",
                                models = setOf(device.model),
                                kernelVersions = setOf(device.kernelVersion),
                                exploit = RemoteArtifact("", -1L),
                                kernelSu = RemoteArtifact("", -1L),
                                requiresFreshP0Session = false,
                            )
                        } else if (advancedMode) {
                            showTargetPicker = true
                            installViewModel.loadTargetCatalog()
                        } else {
                            confirmRebootUserspace = rebootAfterInstall
                            showInstallConfirmation = true
                        }
                    },
                    onLocalPayload = {
                        selectedProfile = null
                        showLocalPayloadPicker = TargetProfile(
                            profileId = InstallViewModel.LOCAL_PROFILE_ID,
                            displayName = "",
                            models = setOf(device.model),
                            kernelVersions = setOf(device.kernelVersion),
                            exploit = RemoteArtifact("", -1L),
                            kernelSu = RemoteArtifact("", -1L),
                            requiresFreshP0Session = false,
                        )
                    },
                )
                AppPage.History -> HistoryPage(
                    padding,
                    history,
                    onDeleteEntries = installViewModel::deleteHistoryEntries,
                )
                AppPage.Settings -> SettingsPage(
                    padding = padding,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    advancedMode = advancedMode,
                    shizukuMode = shizukuMode,
                    localPayloadMode = localPayloadMode,
                    rebootAfterInstall = rebootAfterInstall,
                    updateStatus = updateStatus,
                    onCheckForUpdate = checkForUpdate,
                    onStartDownload = startDownload,
                    onAccentColorChanged = onAccentColorChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    onAdvancedModeChanged = onAdvancedModeChanged,
                    onShizukuModeChanged = onShizukuModeChanged,
                    onLocalPayloadModeChanged = onLocalPayloadModeChanged,
                    onRebootAfterInstallChanged = onRebootAfterInstallChanged,
                )
            }
        }
    }
}

@Composable
private fun AppVersionText(
    style: TextStyle,
    color: Color,
) {
    Text(
        text = stringResource(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
        ),
        style = style,
        color = color,
    )
}

private fun clickHaptic(view: View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        },
    )
}

@Composable
private fun DialogDimAmount(amount: Float) {
    val window = (LocalView.current.parent as DialogWindowProvider).window
    SideEffect { window.setDimAmount(amount) }
}

@Composable
private fun OverviewPage(
    padding: PaddingValues,
    device: DeviceSnapshot,
    installState: InstallUiState,
    updateStatus: UpdateStatus,
    updateCardDismissed: Boolean,
    localPayloadMode: Boolean,
    onDismissUpdateCard: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
    onInstall: () -> Unit,
    onLocalPayload: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                AppVersionText(
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
        if (
            !updateCardDismissed &&
            updateStatus.info != null
        ) {
            item {
                UpdateCard(
                    status = updateStatus,
                    onDismiss = onDismissUpdateCard,
                    onStartDownload = onStartDownload,
                )
            }
        }
        if (!localPayloadMode) {
            item { InstallStatusCard(installState, onInstall) }
        }
        if (localPayloadMode) {
            item { LocalPayloadCard(onLocalPayload) }
        }
        item { DeviceCard(device) }
        item { HowItWorksCard() }
    }
}

private sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class Available(val info: UpdateInfo) : UpdateStatus
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateStatus
    data object UpToDate : UpdateStatus
    data object Failed : UpdateStatus
}

private val UpdateStatus.busy: Boolean
    get() = this is UpdateStatus.Checking || this is UpdateStatus.Downloading

private val UpdateStatus.info: UpdateInfo?
    get() = when (this) {
        is UpdateStatus.Available -> this.info
        is UpdateStatus.Downloading -> this.info
        else -> null
    }

@Composable
private fun UpdateCard(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
) {
    val view = LocalView.current
    val info = status.info
    if (info == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.updater_available_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        clickHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.updater_available_body, info.versionName),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (status) {
                is UpdateStatus.Downloading -> {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = LocalContentColor.current,
                        trackColor = LocalContentColor.current.copy(alpha = 0.2f),
                        drawStopIndicator = {},
                    )
                    Text(
                        text = stringResource(R.string.updater_downloading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.78f),
                    )
                }
                else -> {
                    FilledTonalButton(onClick = {
                        clickHaptic(view)
                        onStartDownload(info)
                    }) {
                        Text(stringResource(R.string.updater_button_download))
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleMedium)
            installerSteps.forEach { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(step.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(step.title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(step.detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallStatusCard(installState: InstallUiState, onInstall: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val uriHandler = LocalUriHandler.current
    val managerInstalled = remember(installState) { isKernelSuManagerInstalled(context) }
    Card(
        onClick = {
            clickHaptic(view)
            when {
                installState.busy -> Unit
                installState.phase == InstallPhase.Installed -> {
                    if (managerInstalled) {
                        openKernelSuManager(context)
                    } else {
                        uriHandler.openUri(KERNEL_SU_MANAGER_URL)
                    }
                }
                else -> onInstall()
            }
        },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                installState.busy -> LoadingIndicator(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                installState.phase == InstallPhase.Installed -> Icon(
                    Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(44.dp),
                )
                installState.phase == InstallPhase.Failed -> Icon(
                    Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
                )
                else -> Icon(
                    Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (installState.phase == InstallPhase.Installed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_kernelsu),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.status_ksu_active),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    Text(
                        text = when (installState.phase) {
                            InstallPhase.Ready -> stringResource(R.string.status_not_installed)
                            else -> installState.message
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = when (installState.phase) {
                        InstallPhase.Installed -> stringResource(
                            if (managerInstalled) {
                                R.string.install_tap_open_manager
                            } else {
                                R.string.install_tap_manager
                            },
                        )
                        InstallPhase.Failed -> stringResource(R.string.install_tap_retry)
                        else -> stringResource(R.string.install_tap_start)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LocalPayloadCard(onClick: () -> Unit) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = {
            clickHaptic(view)
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Rounded.Code,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.local_payload_card_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.local_payload_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.86f),
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot) {
    val view = LocalView.current
    var kernelExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow(Icons.Rounded.Memory, stringResource(R.string.device), "${device.manufacturer} ${device.model} (${device.device})")
            InfoRow(Icons.Rounded.Code, stringResource(R.string.firmware), device.buildId)
            InfoRow(Icons.Rounded.Info, stringResource(R.string.system), "Android ${device.androidRelease} (API ${device.sdk})")
            InfoRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.kernel),
                value = if (kernelExpanded) device.kernelVersionFull else device.kernelRelease,
                onClick = {
                    clickHaptic(view)
                    kernelExpanded = !kernelExpanded
                },
            )
            InfoRow(Icons.Rounded.Security, stringResource(R.string.system_abi), "${device.abi} (${device.pageSize / 1024}K)")
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
        } else {
            Modifier
        },
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class InstallerStep(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val detail: Int,
)

val installerSteps = listOf(
    InstallerStep(
        icon = Icons.Rounded.Security,
        title = R.string.step_exploit_title,
        detail = R.string.step_exploit_detail,
    ),
    InstallerStep(
        icon = Icons.Rounded.VerifiedUser,
        title = R.string.step_ksu_title,
        detail = R.string.step_ksu_detail,
    ),
    InstallerStep(
        icon = Icons.Rounded.RestartAlt,
        title = R.string.step_reboot_title,
        detail = R.string.step_reboot_detail,
    ),
)

@Composable
private fun TargetSelectionSheet(
    device: DeviceSnapshot,
    catalog: TargetCatalog,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onNext: (TargetProfile) -> Unit,
) {
    val view = LocalView.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.select_target_title),
                style = MaterialTheme.typography.titleLarge,
            )
            when (catalog.state) {
                TargetCatalogState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                TargetCatalogState.Error -> {
                    Text(
                        stringResource(R.string.target_load_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    FilledTonalButton(onClick = {
                        clickHaptic(view)
                        onRetry()
                    }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                TargetCatalogState.Loaded -> {
                    if (catalog.profiles.isEmpty()) {
                        Text(stringResource(R.string.target_none_available))
                    } else {
                        catalog.profiles.forEach { profile ->
                            val isCompatible = profile.matchesDevice(device) && profile.matchesKernelVersion(device)
                            Card(
                                onClick = {
                                    clickHaptic(view)
                                    onNext(profile)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompatible) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                ),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        profile.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        profile.supportedModels,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (!isCompatible) {
                                        Text(
                                            stringResource(R.string.target_incompatible),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalPayloadPicker(
    profileId: String,
    globalRebootUserspace: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (exploitUri: Uri, kernelSuUri: Uri?, rebootUserspace: Boolean) -> Unit,
) {
    val view = LocalView.current
    var exploitUri by remember { mutableStateOf<Uri?>(null) }
    var kernelSuUri by remember { mutableStateOf<Uri?>(null) }
    var rebootUserspace by remember { mutableStateOf(globalRebootUserspace) }
    val exploitLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) exploitUri = uri
    }
    val kernelSuLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        kernelSuUri = uri
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Code, contentDescription = null) },
        title = {
            DialogDimAmount(0.34f)
            Text(stringResource(R.string.local_payload_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Exploit picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.local_payload_exploit_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = exploitUri?.lastPathSegment ?: stringResource(R.string.local_payload_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        FilledTonalButton(
                            onClick = {
                                clickHaptic(view)
                                exploitLauncher.launch(arrayOf("*/*"))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(stringResource(R.string.action_browse), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                // KernelSU picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.local_payload_kernelsu_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = kernelSuUri?.lastPathSegment ?: stringResource(R.string.local_payload_optional),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        FilledTonalButton(
                            onClick = {
                                clickHaptic(view)
                                kernelSuLauncher.launch(arrayOf("*/*"))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(stringResource(R.string.action_browse), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                // Per-attempt reboot userspace toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = rebootUserspace,
                            role = Role.Switch,
                            onValueChange = {
                                clickHaptic(view)
                                rebootUserspace = it
                            },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.reboot_userspace_this_attempt),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.reboot_userspace_this_attempt_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = rebootUserspace,
                        onCheckedChange = null,
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    clickHaptic(view)
                    val exploit = exploitUri ?: return@FilledTonalButton
                    onConfirm(exploit, kernelSuUri, rebootUserspace)
                },
                enabled = exploitUri != null,
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clickHaptic(view)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun HistoryPage(
    padding: PaddingValues,
    history: List<InstallHistoryEntry>,
    onDeleteEntries: (Set<String>) -> Unit,
) {
    val view = LocalView.current
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectionMode && selectedIds.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        clickHaptic(view)
                        onDeleteEntries(selectedIds)
                        selectedIds = emptySet()
                        selectionMode = false
                    },
                    icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    text = { Text(pluralStringResource(R.plurals.history_delete_n, selectedIds.size, selectedIds.size)) },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 54.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.nav_history),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.weight(1f),
                    )
                    AnimatedVisibility(visible = history.isNotEmpty()) {
                        IconButton(onClick = {
                            clickHaptic(view)
                            if (selectionMode) {
                                if (selectedIds.size == history.size) {
                                    selectedIds = emptySet()
                                } else {
                                    selectedIds = history.map { it.id }.toSet()
                                }
                            } else {
                                selectionMode = true
                                selectedIds = history.map { it.id }.toSet()
                            }
                        }) {
                            Crossfade(
                                targetState = selectionMode && selectedIds.size == history.size,
                                label = "select_all_icon",
                            ) { allSelected ->
                                if (allSelected) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_deselect_all))
                                } else {
                                    Icon(Icons.Rounded.SelectAll, contentDescription = stringResource(R.string.action_select_all))
                                }
                            }
                        }
                    }
                }
            }
            if (history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.history_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            itemsIndexed(history, key = { _, entry -> entry.id }) { index, entry ->
                val isSelected = entry.id in selectedIds
                val cardElevation by animateDpAsState(
                    targetValue = if (isSelected) 4.dp else 0.dp,
                    label = "card_elevation_$index",
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                clickHaptic(view)
                                if (selectionMode) {
                                    selectedIds = if (isSelected) selectedIds - entry.id else selectedIds + entry.id
                                    if (selectedIds.isEmpty()) selectionMode = false
                                }
                            },
                            onLongClick = {
                                clickHaptic(view)
                                selectionMode = true
                                selectedIds = selectedIds + entry.id
                            },
                        ),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (entry.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                            contentDescription = null,
                            tint = if (entry.success) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.profileId ?: stringResource(R.string.history_local_payload),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                formatHistoryDate(entry.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AnimatedVisibility(visible = selectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    padding: PaddingValues,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    advancedMode: Boolean,
    shizukuMode: Boolean,
    localPayloadMode: Boolean,
    rebootAfterInstall: Boolean,
    updateStatus: UpdateStatus,
    onCheckForUpdate: () -> Unit,
    onStartDownload: (UpdateInfo) -> Unit,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAdvancedModeChanged: (Boolean) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
    onLocalPayloadModeChanged: (Boolean) -> Unit,
    onRebootAfterInstallChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 54.dp, bottom = 6.dp),
            )
        }
        // Appearance section
        item {
            SettingsSectionCard {
                SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                // Theme mode
                SettingsNavigateCard(
                    icon = when (themeMode) {
                        AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                        AppThemeMode.Light -> Icons.Rounded.LightMode
                        AppThemeMode.Dark -> Icons.Rounded.DarkMode
                    },
                    title = stringResource(R.string.settings_theme),
                    value = themeModeLabel(themeMode),
                    onClick = {
                        clickHaptic(view)
                        showColorPicker = true
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                // Accent color
                SettingsNavigateCard(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_accent_color),
                    value = accentColorLabel(accentColor),
                    onClick = {
                        clickHaptic(view)
                        showColorPicker = true
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                // Language
                SettingsNavigateCard(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    value = currentLanguageLabel(),
                    onClick = {
                        clickHaptic(view)
                        showLanguagePicker = true
                    },
                )
            }
        }
        // Advanced section
        item {
            SettingsSectionCard {
                SettingsSectionHeader(stringResource(R.string.settings_section_advanced))
                SettingsSwitchCard(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.settings_advanced_mode),
                    description = stringResource(R.string.settings_advanced_mode_description),
                    checked = advancedMode,
                    onCheckedChange = {
                        clickHaptic(view)
                        onAdvancedModeChanged(it)
                    },
                    enabled = !localPayloadMode,
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                SettingsSwitchCard(
                    icon = Icons.Rounded.Save,
                    title = stringResource(R.string.settings_shizuku_mode),
                    description = stringResource(R.string.settings_shizuku_mode_description),
                    checked = shizukuMode,
                    onCheckedChange = {
                        clickHaptic(view)
                        onShizukuModeChanged(it)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                SettingsSwitchCard(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_local_payload_mode),
                    description = stringResource(R.string.settings_local_payload_mode_description),
                    checked = localPayloadMode,
                    onCheckedChange = {
                        clickHaptic(view)
                        onLocalPayloadModeChanged(it)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                SettingsSwitchCard(
                    icon = Icons.Rounded.RestartAlt,
                    title = stringResource(R.string.settings_reboot_after_install),
                    description = stringResource(R.string.settings_reboot_after_install_description),
                    checked = rebootAfterInstall,
                    onCheckedChange = {
                        clickHaptic(view)
                        onRebootAfterInstallChanged(it)
                    },
                )
            }
        }
        // About section
        item {
            SettingsSectionCard {
                SettingsSectionHeader(stringResource(R.string.settings_section_about))
                SettingsNavigateCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version),
                    value = "",
                    valueContent = {
                        AppVersionText(
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        clickHaptic(view)
                        onCheckForUpdate()
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                when (val status = updateStatus) {
                    is UpdateStatus.Checking -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.updater_checking), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is UpdateStatus.UpToDate -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(stringResource(R.string.updater_up_to_date), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is UpdateStatus.Failed -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(stringResource(R.string.updater_failed), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is UpdateStatus.Available -> {
                        SettingsNavigateCard(
                            icon = Icons.Rounded.SystemUpdate,
                            title = stringResource(R.string.updater_available_title),
                            value = status.info.versionName,
                            onClick = {
                                clickHaptic(view)
                                onStartDownload(status.info)
                            },
                        )
                    }
                    is UpdateStatus.Downloading -> {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(stringResource(R.string.updater_downloading), style = MaterialTheme.typography.bodyMedium)
                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier.fillMaxWidth(),
                                drawStopIndicator = {},
                            )
                        }
                    }
                    else -> {}
                }
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                SettingsNavigateCard(
                    icon = Icons.Rounded.Link,
                    title = stringResource(R.string.settings_source_code),
                    value = "",
                    onClick = {
                        clickHaptic(view)
                        uriHandler.openUri("https://github.com/TheFlood424K/Root-My-Galaxy")
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                SettingsNavigateCard(
                    icon = Icons.Rounded.Link,
                    title = "KernelSU",
                    value = "",
                    onClick = {
                        clickHaptic(view)
                        uriHandler.openUri(KERNEL_SU_HOME_URL)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                SettingsNavigateCard(
                    icon = Icons.Rounded.Link,
                    title = "Shizuku",
                    value = "",
                    onClick = {
                        clickHaptic(view)
                        openShizukuManager(context)
                    },
                )
            }
        }
    }

    if (showColorPicker) {
        ThemePickerSheet(
            accentColor = accentColor,
            themeMode = themeMode,
            onAccentColorChanged = onAccentColorChanged,
            onThemeModeChanged = onThemeModeChanged,
            onDismiss = { showColorPicker = false },
        )
    }

    if (showLanguagePicker) {
        LanguagePickerPopup(
            onDismiss = { showLanguagePicker = false },
        )
    }
}

@Composable
private fun ThemePickerSheet(
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Theme mode selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        ToggleButton(
                            checked = selected,
                            onCheckedChange = {
                                clickHaptic(view)
                                onThemeModeChanged(mode)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(
                                when (mode) {
                                    AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                                    AppThemeMode.Light -> Icons.Rounded.LightMode
                                    AppThemeMode.Dark -> Icons.Rounded.DarkMode
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                themeModeLabel(mode),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            // Accent color selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleMedium)
                AccentColorGrid(
                    accentColor = accentColor,
                    onAccentColorChanged = onAccentColorChanged,
                )
            }
        }
    }
}

@Composable
private fun AccentColorGrid(
    accentColor: AccentColor,
    onAccentColorChanged: (AccentColor) -> Unit,
) {
    val view = LocalView.current
    val columns = 5
    val entries = AccentColor.entries
    val rows = (entries.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(columns) { col ->
                    val index = row * columns + col
                    if (index < entries.size) {
                        val entry = entries[index]
                        val selected = accentColor == entry
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accentColorSwatch(entry))
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        clickHaptic(view)
                                        onAccentColorChanged(entry)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedVisibility(
                                visible = selected,
                                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                                exit = scaleOut() + fadeOut(),
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePickerPopup(
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var anchorOffset by remember { mutableStateOf(0f) }
    val menuWidth = 220.dp
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    var alignRight by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.0f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
    ) {
        Popup(
            alignment = if (alignRight) Alignment.TopEnd else Alignment.TopStart,
            offset = androidx.compose.ui.unit.IntOffset(0, anchorOffset.toInt()),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                    transformOrigin = TransformOrigin(if (alignRight) 1f else 0f, 0f),
                ) + fadeIn(tween(120)),
            ) {
                Surface(
                    modifier = Modifier.width(menuWidth),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        languageOptions.forEach { option ->
                            val current = currentLanguageTag(context)
                            val selected = option.tag == current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            clickHaptic(view)
                                            setAppLanguage(context, option.tag)
                                            onDismiss()
                                        },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    stringResource(option.label),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                enabled = enabled,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingsNavigateCard(
    icon: ImageVector,
    title: String,
    value: String,
    valueContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (valueContent != null) {
            valueContent()
        } else {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun expressiveClickableCardShape(interactionSource: MutableInteractionSource): RoundedCornerShape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "card_corner_radius",
    )
    return RoundedCornerShape(cornerRadius)
}

private fun formatHistoryDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val tag = currentLanguageTag(context)
    return languageOptions.firstOrNull { it.tag == tag }?.let { stringResource(it.label) }
        ?: stringResource(R.string.language_system)
}

private fun currentLanguageTag(context: Context): String {
    return androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        .toLanguageTags()
        .takeIf { it.isNotBlank() && it != "und" } ?: ""
}

private fun setAppLanguage(context: Context, tag: String) {
    val localeList = if (tag.isBlank()) {
        androidx.core.os.LocaleListCompat.getEmptyLocaleList()
    } else {
        androidx.core.os.LocaleListCompat.forLanguageTags(tag)
    }
    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
}

@Composable
private fun accentColorLabel(accentColor: AccentColor): String = when (accentColor) {
    AccentColor.Dynamic -> stringResource(R.string.accent_dynamic)
    AccentColor.Blue -> stringResource(R.string.accent_blue)
    AccentColor.Green -> stringResource(R.string.accent_green)
    AccentColor.Purple -> stringResource(R.string.accent_purple)
    AccentColor.Red -> stringResource(R.string.accent_red)
    AccentColor.Orange -> stringResource(R.string.accent_orange)
    AccentColor.Pink -> stringResource(R.string.accent_pink)
    AccentColor.Teal -> stringResource(R.string.accent_teal)
    AccentColor.Yellow -> stringResource(R.string.accent_yellow)
    AccentColor.Monochrome -> stringResource(R.string.accent_monochrome)
}

@Composable
private fun accentColorSwatch(accentColor: AccentColor): Color = when (accentColor) {
    AccentColor.Dynamic -> MaterialTheme.colorScheme.primary
    AccentColor.Blue -> Color(0xFF1976D2)
    AccentColor.Green -> Color(0xFF388E3C)
    AccentColor.Purple -> Color(0xFF7B1FA2)
    AccentColor.Red -> Color(0xFFD32F2F)
    AccentColor.Orange -> Color(0xFFF57C00)
    AccentColor.Pink -> Color(0xFFC2185B)
    AccentColor.Teal -> Color(0xFF00796B)
    AccentColor.Yellow -> Color(0xFFF9A825)
    AccentColor.Monochrome -> Color(0xFF616161)
}

@Composable
private fun themeModeLabel(themeMode: AppThemeMode): String = when (themeMode) {
    AppThemeMode.System -> stringResource(R.string.theme_system)
    AppThemeMode.Light -> stringResource(R.string.theme_light)
    AppThemeMode.Dark -> stringResource(R.string.theme_dark)
}

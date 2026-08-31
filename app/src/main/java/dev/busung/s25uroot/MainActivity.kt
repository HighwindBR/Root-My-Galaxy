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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.busung.s25uroot.ui.theme.RootMyGalaxyTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val installViewModel: InstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val accentColor by installViewModel.accentColor.collectAsStateWithLifecycle()
            val themeMode by installViewModel.themeMode.collectAsStateWithLifecycle()
            RootMyGalaxyTheme(
                accentColor = accentColor,
                themeMode = themeMode,
            ) {
                RootMyGalaxyApp(installViewModel)
            }
        }
    }
}

@Composable
private fun RootMyGalaxyApp(installViewModel: InstallViewModel) {
    val context = LocalContext.current
    val uiState by installViewModel.uiState.collectAsStateWithLifecycle()
    val targetCatalog by installViewModel.targetCatalog.collectAsStateWithLifecycle()
    val installHistory by installViewModel.installHistory.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showHistorySheet by rememberSaveable { mutableStateOf(false) }
    var showTargetSheet by rememberSaveable { mutableStateOf(false) }
    var rebootUserspace by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.pendingInstallRequest) {
        if (uiState.pendingInstallRequest != null) {
            installViewModel.consumePendingInstallRequest()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_home)) },
                    selected = currentTab == AppTab.Home,
                    onClick = { currentTab = AppTab.Home },
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_history)) },
                    selected = currentTab == AppTab.History,
                    onClick = { currentTab = AppTab.History },
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    selected = currentTab == AppTab.Settings,
                    onClick = { currentTab = AppTab.Settings },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                AppTab.Home -> HomeScreen(
                    uiState = uiState,
                    rebootUserspace = rebootUserspace,
                    onRebootUserspaceChanged = { rebootUserspace = it },
                    onSelectPayload = {
                        installViewModel.loadTargetCatalog()
                        showTargetSheet = true
                    },
                    onStartRoot = {
                        installViewModel.startRoot(rebootUserspace = rebootUserspace)
                        rebootUserspace = false
                    },
                    onStopSession = installViewModel::stopSession,
                )
                AppTab.History -> HistoryScreen(
                    entries = installHistory,
                    onDelete = installViewModel::deleteHistoryEntry,
                    onDeleteAll = installViewModel::deleteAllHistoryEntries,
                )
                AppTab.Settings -> SettingsScreen(
                    installViewModel = installViewModel,
                )
            }
        }
    }

    if (showTargetSheet) {
        TargetSelectionSheet(
            device = uiState.device,
            catalog = targetCatalog,
            onDismiss = { showTargetSheet = false },
            onRetry = installViewModel::loadTargetCatalog,
            onNext = { profile ->
                showTargetSheet = false
                installViewModel.selectProfile(profile)
            },
        )
    }
}

enum class AppTab { Home, History, Settings }

@Composable
private fun HomeScreen(
    uiState: InstallUiState,
    rebootUserspace: Boolean,
    onRebootUserspaceChanged: (Boolean) -> Unit,
    onSelectPayload: () -> Unit,
    onStartRoot: () -> Unit,
    onStopSession: () -> Unit,
) {
    val view = LocalView.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DeviceCard(uiState.device)
        }
        item {
            RootStatusCard(uiState)
        }
        item {
            PayloadCard(
                uiState = uiState,
                rebootUserspace = rebootUserspace,
                onRebootUserspaceChanged = onRebootUserspaceChanged,
                onSelectPayload = {
                    clickHaptic(view)
                    onSelectPayload()
                },
                onStartRoot = {
                    clickHaptic(view)
                    onStartRoot()
                },
                onStopSession = {
                    clickHaptic(view)
                    onStopSession()
                },
            )
        }
        item {
            StepsCard()
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.card_device_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            DeviceInfoRow(stringResource(R.string.device_model), device.model)
            DeviceInfoRow(stringResource(R.string.device_kernel), device.kernelVersion)
            DeviceInfoRow(stringResource(R.string.device_android), device.androidVersion)
            DeviceInfoRow(stringResource(R.string.device_security_patch), device.securityPatch)
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RootStatusCard(uiState: InstallUiState) {
    val isRooted = uiState.isRooted
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRooted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (isRooted) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
                contentDescription = null,
                tint = if (isRooted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    stringResource(if (isRooted) R.string.root_status_rooted else R.string.root_status_not_rooted),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (isRooted && uiState.kernelSuVersion != null) {
                    Text(
                        stringResource(R.string.root_ksu_version, uiState.kernelSuVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PayloadCard(
    uiState: InstallUiState,
    rebootUserspace: Boolean,
    onRebootUserspaceChanged: (Boolean) -> Unit,
    onSelectPayload: () -> Unit,
    onStartRoot: () -> Unit,
    onStopSession: () -> Unit,
) {
    val selectedProfile = uiState.selectedProfile
    val isRunning = uiState.phase != null && uiState.phase != InstallPhase.Done

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.card_payload_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            if (selectedProfile != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        selectedProfile.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        selectedProfile.supportedModels,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Reboot userspace toggle — only visible when a payload is selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = rebootUserspace,
                            role = Role.Switch,
                            onValueChange = onRebootUserspaceChanged,
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.option_reboot_userspace),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(R.string.option_reboot_userspace_detail),
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

            if (uiState.phase != null) {
                InstallProgressSection(uiState)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onSelectPayload,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(
                        if (selectedProfile == null) R.string.action_select_payload
                        else R.string.action_change_payload
                    ))
                }
                if (selectedProfile != null) {
                    Button(
                        onClick = if (isRunning) onStopSession else onStartRoot,
                        modifier = Modifier.weight(1f),
                        colors = if (isRunning) {
                            androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.buttonColors()
                        },
                    ) {
                        Text(stringResource(
                            if (isRunning) R.string.action_stop
                            else R.string.action_root
                        ))
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallProgressSection(uiState: InstallUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (uiState.progress != null) {
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            uiState.statusMessage ?: stringResource(R.string.install_running),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    catalog: TargetCatalogUiState,
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
            if (catalog.loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (catalog.error != null) {
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
                } else {
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

@Composable
private fun StepsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.card_steps_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            installerSteps.forEachIndexed { index, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        step.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            stringResource(step.title),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(step.detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < installerSteps.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// History Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryScreen(
    entries: List<InstallHistoryEntry>,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelectedDialog by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

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
                    text = { Text(stringResource(R.string.action_delete_selected, selectedIds.size)) },
                    icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = { showDeleteSelectedDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectionMode) {
                            TextButton(onClick = {
                                selectedIds = entries.map { it.id }.toSet()
                            }) {
                                Icon(Icons.Rounded.SelectAll, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_select_all))
                            }
                            TextButton(onClick = {
                                selectionMode = false
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_cancel))
                            }
                        } else {
                            Text(
                                stringResource(R.string.history_count, entries.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_clear_all))
                            }
                        }
                    }
                }
                itemsIndexed(entries, key = { _, entry -> entry.id }) { _, entry ->
                    val isSelected = entry.id in selectedIds
                    val cardElevation by animateDpAsState(
                        if (isSelected) 4.dp else 0.dp,
                        label = "cardElevation",
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - entry.id
                                        } else {
                                            selectedIds + entry.id
                                        }
                                        if (selectedIds.isEmpty()) selectionMode = false
                                    }
                                },
                                onLongClick = {
                                    clickHaptic(view)
                                    selectionMode = true
                                    selectedIds = selectedIds + entry.id
                                },
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
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
                                if (entry.result == InstallRunResult.Succeeded) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (entry.result == InstallRunResult.Succeeded) {
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
                                    formatHistoryDate(entry.startedAtMillis),
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

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_history_title)) },
            text = { Text(stringResource(R.string.dialog_clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    showDeleteAllDialog = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_selected_title)) },
            text = { Text(stringResource(R.string.dialog_delete_selected_body, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    selectedIds.forEach(onDelete)
                    selectedIds = emptySet()
                    selectionMode = false
                    showDeleteSelectedDialog = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun formatHistoryDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(millis))

// ─────────────────────────────────────────────────────────────────────────────
// Settings Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsScreen(installViewModel: InstallViewModel) {
    val context = LocalContext.current
    val accentColor by installViewModel.accentColor.collectAsStateWithLifecycle()
    val themeMode by installViewModel.themeMode.collectAsStateWithLifecycle()
    val advancedMode by installViewModel.advancedMode.collectAsStateWithLifecycle()
    val shizukuMode by installViewModel.shizukuMode.collectAsStateWithLifecycle()
    val autoReroot by installViewModel.autoReroot.collectAsStateWithLifecycle()
    val localPayloadMode by installViewModel.localPayloadMode.collectAsStateWithLifecycle()

    var showAccentPicker by rememberSaveable { mutableStateOf(false) }
    var showThemePicker by rememberSaveable { mutableStateOf(false) }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
        }
        item {
            SettingsItem(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_accent_color),
                subtitle = accentColorLabel(accentColor),
                onClick = { showAccentPicker = true },
            )
        }
        item {
            SettingsItem(
                icon = Icons.Rounded.BrightnessAuto,
                title = stringResource(R.string.settings_theme_mode),
                subtitle = themeModeLabel(themeMode),
                onClick = { showThemePicker = true },
            )
        }
        item {
            SettingsItem(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.settings_language),
                subtitle = currentLanguageLabel(context),
                onClick = { showLanguagePicker = true },
            )
        }
        item { SettingsSectionHeader(stringResource(R.string.settings_section_behavior)) }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.settings_advanced_mode),
                subtitle = stringResource(R.string.settings_advanced_mode_detail),
                checked = advancedMode,
                onCheckedChange = { installViewModel.setAdvancedMode(it) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Save,
                title = stringResource(R.string.settings_shizuku),
                subtitle = stringResource(R.string.settings_shizuku_detail),
                checked = shizukuMode,
                onCheckedChange = { installViewModel.setShizukuMode(it) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Schedule,
                title = stringResource(R.string.settings_auto_reroot),
                subtitle = stringResource(R.string.settings_auto_reroot_detail),
                checked = autoReroot,
                onCheckedChange = { installViewModel.setAutoReroot(it) },
            )
        }
        item {
            SettingsToggleItem(
                icon = Icons.Rounded.Link,
                title = stringResource(R.string.settings_local_payload),
                subtitle = stringResource(R.string.settings_local_payload_detail),
                checked = localPayloadMode,
                onCheckedChange = { installViewModel.setLocalPayloadMode(it) },
            )
        }
        item { SettingsSectionHeader(stringResource(R.string.settings_section_about)) }
        item {
            SettingsItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_about),
                onClick = { showAbout = true },
            )
        }
    }

    if (showAccentPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAccentPicker = false },
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
                    stringResource(R.string.settings_accent_color),
                    style = MaterialTheme.typography.titleLarge,
                )
                AccentColorGrid(
                    accentColor = accentColor,
                    onAccentColorChanged = {
                        installViewModel.setAccentColor(it)
                        showAccentPicker = false
                    },
                )
            }
        }
    }

    if (showThemePicker) {
        ModalBottomSheet(
            onDismissRequest = { showThemePicker = false },
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
                    stringResource(R.string.settings_theme_mode),
                    style = MaterialTheme.typography.titleLarge,
                )
                Column(modifier = Modifier.selectableGroup()) {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = themeMode == mode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        installViewModel.setThemeMode(mode)
                                        showThemePicker = false
                                    },
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = null,
                            )
                            Text(themeModeLabel(mode))
                        }
                    }
                }
            }
        }
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            context = context,
            onDismiss = { showLanguagePicker = false },
        )
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        onClick = {
            clickHaptic(view)
            onClick()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
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
                            androidx.compose.animation.AnimatedVisibility(
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
private fun LanguagePickerSheet(
    context: Context,
    onDismiss: () -> Unit,
) {
    val currentTag = remember { currentLanguageTag(context) }
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
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(modifier = Modifier.selectableGroup()) {
                languageOptions.forEach { option ->
                    val isSelected = currentTag == option.tag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = {
                                    setAppLanguage(context, option.tag)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(stringResource(option.label))
                    }
                }
            }
        }
    }
}

data class LanguageOption(@StringRes val label: Int, val tag: String)

val languageOptions = listOf(
    LanguageOption(R.string.language_system, ""),
    LanguageOption(R.string.language_english, "en"),
    LanguageOption(R.string.language_korean, "ko"),
    LanguageOption(R.string.language_spanish, "es"),
    LanguageOption(R.string.language_french, "fr"),
    LanguageOption(R.string.language_german, "de"),
    LanguageOption(R.string.language_japanese, "ja"),
    LanguageOption(R.string.language_chinese_simplified, "zh-Hans"),
)

@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheFlood424K/Root-My-Galaxy"))
                    context.startActivity(intent)
                },
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.about_github))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun clickHaptic(view: View) {
    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

@Composable
private fun currentLanguageLabel(context: Context): String {
    val tag = remember { currentLanguageTag(context) }
    return languageOptions.firstOrNull { it.tag == tag }?.let { stringResource(it.label) }
        ?: stringResource(R.string.language_system)
}

private fun currentLanguageTag(context: Context): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val lm = context.getSystemService(android.app.LocaleManager::class.java)
        lm?.applicationLocales?.toLanguageTags()
            ?.takeIf { it.isNotBlank() && it != "und" } ?: ""
    } else {
        java.util.Locale.getDefault().toLanguageTag()
            .takeIf { it.isNotBlank() && it != "und" } ?: ""
    }
}

private fun setAppLanguage(context: Context, tag: String) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val lm = context.getSystemService(android.app.LocaleManager::class.java)
        lm?.applicationLocales = if (tag.isBlank()) {
            android.os.LocaleList.getEmptyLocaleList()
        } else {
            android.os.LocaleList.forLanguageTags(tag)
        }
    }
}

@Composable
private fun themeModeLabel(themeMode: AppThemeMode): String = when (themeMode) {
    AppThemeMode.System -> stringResource(R.string.theme_system)
    AppThemeMode.Light -> stringResource(R.string.theme_light)
    AppThemeMode.Dark -> stringResource(R.string.theme_dark)
}

@Composable
private fun accentColorLabel(accentColor: AccentColor): String = when (accentColor) {
    AccentColor.Dynamic -> stringResource(R.string.accent_dynamic)
    AccentColor.Blue -> stringResource(R.string.accent_blue)
    AccentColor.Violet -> stringResource(R.string.accent_violet)
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
    AccentColor.Violet -> Color(0xFF6200EE)
    AccentColor.Green -> Color(0xFF388E3C)
    AccentColor.Purple -> Color(0xFF7B1FA2)
    AccentColor.Red -> Color(0xFFD32F2F)
    AccentColor.Orange -> Color(0xFFF57C00)
    AccentColor.Pink -> Color(0xFFC2185B)
    AccentColor.Teal -> Color(0xFF00796B)
    AccentColor.Yellow -> Color(0xFFF9A825)
    AccentColor.Monochrome -> Color(0xFF616161)
}

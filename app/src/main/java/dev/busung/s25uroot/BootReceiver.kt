package dev.busung.s25uroot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-establishes root after a reboot: on these targets KernelSU is loaded as
 * a runtime module and nothing persists across power cycles, so the install
 * has to run again every boot. Skips when the module is already active.
 *
 * The user can disable this behaviour via the "Re-root on reboot" toggle in
 * Settings, which sets [AppPreferences.autoReroot] to false.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!AppPreferences.autoReroot(context)) return
        if (NativeProbe.isKernelSuActive()) return
        BootInstallService.start(context)
    }
}

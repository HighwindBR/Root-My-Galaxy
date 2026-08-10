# Project Notes

## What this is

One-click root installer for supported Samsung Galaxy models ("Root My Galaxy").
The APK verifies a support profile, downloads commit-pinned exploit/KernelSU
payloads from the [Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads)
feed, runs a native exploit (`libcve43499root.so`) to acquire bootstrap root,
then late-loads KernelSU.

Payload offsets, native exploits, and KernelSU artifacts live in the separate
payloads repo; this repo is the app only.

## Architecture

- `MainActivity` — Compose app shell: Overview / History / Settings pages, theme
  and language preferences, advanced-mode target picker.
- `InstallActivity` — full-screen installer with live log and per-step progress.
- `InstallViewModel` — owns install runs, exploit execution, P0 offset caching,
  and the history store.
- `PayloadRepository` — support manifest + artifact download/verify against the
  feed base URL (editable in Settings).
- `NativeProbe` / `DeviceSnapshot` / `SupportManifest` — device identity, kernel
  version matching, profile catalog.
- `InstallHistory` — JSON history store with atomic writes.
- Native helper: `app/src/main/cpp` (CVE-2024-43499 bootstrap helper).

## Current status

- Working: profile resolution, payload download+verify, exploit run, KernelSU
  late-load with control-channel verification, install receipt, history, feed
  URL override, advanced target picker, theme/accent/language settings.

## Backlog / ideas

- [ ] Re-check the P0 offset cache strategy for fresh-P0 payloads.
- [ ] Retry-until-succeed convenience flow from the Install screen.
- [ ] Notifications when an install succeeds or fails in the background.

## Build

See `README.md`. Build/deploy: `make install`. Debug APK:
`app/build/outputs/apk/debug/app-debug.apk`.

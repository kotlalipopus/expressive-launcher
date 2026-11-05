# Expressive Launcher

An Android home screen with three looks — Liquid Glass, Material 3 Expressive and Material You Lite —
built with Kotlin and Jetpack Compose. Every panel, icon and animation follows the theme you pick.

<!-- Add 2–3 screenshots here, for example:
<p>
  <img src="screenshots/liquid-glass.png" width="250">
  <img src="screenshots/expressive.png" width="250">
  <img src="screenshots/lite.png" width="250">
</p>
-->

## Download

Get the latest APK from [Releases](https://github.com/kotlalipopus/expressive-launcher/releases/latest).

Requires Android 8.1 or newer. Liquid Glass refraction needs Android 13+.

## Features

- **Themes:** Liquid Glass (iOS 26–style liquid glass), Material 3 Expressive, Material You Lite — one tap, with a cross-fade
- **Liquid Glass:** real lens refraction of what is under the glass, rainbow edges, corner glints on icons, widgets and panels,
  buttons that stretch like liquid when you drag them (powered by [liquid](https://github.com/FletchMcKee/liquid))
- **Wallpaper:** your own launcher wallpaper with dim, blur and parallax, or set it as the system wallpaper too
- **Icons:** themed icons in Material You colors, icon packs, 13 icon shapes, glass icon styles (normal, clear, tinted)
- **Per-app customization:** picture, shape, size, custom name and label visibility for every app
- **Home screen:** grid size, dock, folders, widgets with resizing, page transitions, multi-select
- **App launch:** the app window grows out of its icon and shrinks back into it, interruptible at any moment
- **Search bar:** Google, DuckDuckGo, Bing, Yandex, Brave or Perplexity, custom buttons (voice, Lens, AI mode, any app)
- **App drawer:** alphabet scroller, search, hidden apps, recently installed
- **Gestures:** swipe up, swipe down, double tap — each with its own action
- **Look:** Material You or a custom accent color, palettes, contrast, AMOLED black, custom fonts
- **Onboarding:** first-run setup with theme, layout, gestures and permissions
- **App language:** Russian or English

## Installation

1. Download and install the APK from [Releases](https://github.com/kotlalipopus/expressive-launcher/releases/latest).
2. Open the app and follow the first-run setup.
3. Choose Expressive Launcher as your default home app when asked (or in **Settings → Apps → Default apps → Home app**).

## Permissions

| Permission | Why |
|---|---|
| Query all packages | Show every installed app in the drawer and on the home screen |
| All files access | Read the system wallpaper image so the glass can show it (optional) |
| Set wallpaper | "Also set as system wallpaper" button in the wallpaper menu |
| Notification access | Unread badges on icons (optional, granted in system settings) |
| Accessibility service | Gestures that lock the screen or open notifications (optional, granted in system settings) |
| Expand status bar | Swipe-down gesture opens notifications |
| Request delete packages | "Uninstall" in the icon menu |
| Vibration | Haptic feedback |

The launcher has no internet permission: nothing you do on the home screen leaves your phone.

## Building from source

You need Android Studio (or JDK 17 and Android SDK 37).

```bash
git clone https://github.com/kotlalipopus/expressive-launcher.git
cd expressive-launcher
./gradlew assembleRelease
```

The APK will be in `app/build/outputs/apk/release/`. The release build is signed with the debug key,
so it installs over a debug build without losing your layout.

## License

[MIT](LICENSE)

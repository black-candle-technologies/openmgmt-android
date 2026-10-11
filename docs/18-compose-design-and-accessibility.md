# Compose UI engineering, theme, and accessibility

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## UI composition pattern

MainActivity supplies the application theme, shared SnackbarHostState, route state, AppScaffold, and TaskActionsHost. Individual screens render content rather than owning separate activities. Shared task cards call composition-local actions to open the editor or change completion. This is why every task list has the same completion/Undo semantics. [Root composition](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L71-L112) [Shared task actions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L77-L89)

The navigation dependency is present in Gradle, but current navigation is direct saved-route dispatch. A new screen should follow that structure unless maintainers intentionally migrate to Navigation Compose. Do not assume deep links exist for task IDs or project detail pages; the only deep-link filter in the manifest is for OAuth. [Dependency](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L63-L65) [Destination dispatch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/nav/Destinations.kt#L14-L27)

## Shared component catalog

| Component/helper | Role | Detail worth preserving |
|---|---|---|
| AppScaffold | Drawer, top bar, shared FAB, snackbar, safe content area | Remembered current/sync state prevents stale subcomposed titles |
| safeInsets | System bars union display cutout | Protects landscape cutouts/side navigation bars |
| ScreenFab / FabController / FabSpec | Per-screen action registration | DisposableEffect removes only the spec it owns |
| LocalContentGutter / screenPadding | Responsive page margins | Min 16 dp; centered 720 dp reading width; 96 dp bottom with FAB |
| Section / SectionHeader | Header, optional count/action | Count badge only when supplied |
| MetricCard | Summary metric card | Optional click; label allows two lines |
| TaskCard | Title, done checkbox, badges, due label | Identity-aware suppression of redundant Inbox badge |
| Badge / StatusBadge | Compact semantic labels | Different tones for progress, warning, completion |
| EmptyState | Shared empty presentation | Centered title and hint |
| TextInputDialog | Name-entry creation dialog | Trim/nonblank guard; Done keyboard action |
| TaskActionsHost | One shared task editor and actions | Custom saveable task snapshot |
| taskSection | Lazy list header/items | Empty-title null omits an empty section |
| InfoCard / StatusRow | Sync/settings facts | Shared status-dot and trailing value presentation |

[Scaffold helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L83-L119) [Reusable cards](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L76-L247) [Task section](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt#L15-L58) [Info/status](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt#L134-L173)

## Responsive layout and system bars

The Scaffold unions system bars with display-cutout insets. The drawer is capped at 300 dp. Its inset selection includes vertical/start edges. Content computes the side gutter as `max(16.dp, (maxWidth - 720.dp) / 2)`. The FAB and snackbar add horizontal safe-inset padding because their standard placement does not cover every landscape case. [Scaffold layout](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L153-L176) [FAB/content insets](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L224-L251)

MainActivity uses transparent navigation-bar scrims and, on API 29+, disables enforced navigation-bar contrast. DrawerStatusBarIcons switches icon brightness for the charcoal drawer and restores the system theme behavior after it closes. Source comments identify prior landscape camera-cutout and three-button-navigation overlaps as the motivation; this review did not reproduce those devices. [Activity bars](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L45-L57) [Drawer bar icons](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L257-L274)

The task editor scrolls and adds IME padding. It skips partial sheet expansion, focuses Title for new tasks, and animates dismissal before invoking its completion callback. On windows shorter than 560 dp, the date picker omits title/headline and mode toggle to preserve its grid area. These are explicit responsive behaviors. [Sheet layout](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L194-L219) [Short-window date picker](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L328-L359)

## Theme system

`OpenMgmtTheme` follows `isSystemInDarkTheme()` by default and supplies Material3 color, shape, typography, and separate caution-status colors. There is no dynamic wallpaper color opt-in or user theme preference. The drawer remains charcoal in both schemes. [Theme entry](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt#L180-L195) [Drawer tokens](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt#L169-L178)

| Design role | Light source value | Dark source value |
|---|---|---|
| Background | Paper `#F3F2ED` | Board `#0D100E` |
| Surface | `#FAFAF7` | `#151916` |
| Primary | Deep green `#294F37` | Lime `#C9EF6A` |
| Primary text | `#EEF1E9` | Lime ink `#182017` |
| Main text | Ink `#20251F` | `#E6EBE3` |
| Drawer background | `#171C17` | Same |
| Drawer active surface | `#20271F` | Same |
| Drawer brand mark | `#C9EF6A` | Same |

[Palette definitions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt#L28-L121)

Shapes use 6 dp for extraSmall/small, 9 dp for medium/large, and 16 dp for extraLarge. Typography customizes titleLarge as 20 sp semibold SansSerif and labelSmall as 11 sp semibold with 1.2 sp letter spacing; the rest comes from the Material Typography defaults. Blocked and waiting use separate caution containers. [Shapes/typography](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt#L123-L167) [Status tones](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L265-L279)

## Resource files

The XML light/night themes set an appropriate launch-window background and transparent system bars to reduce a launch flash before Compose draws. The adaptive launcher icons share a green background and the vector foreground, including a monochrome entry. Only `app_name` is extracted into strings.xml. All other inspected visible labels are Kotlin literals, so supportsRtl=true in the manifest is not a localization implementation. [Light window theme](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values/themes.xml#L1-L9) [Night window theme](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values-night/themes.xml#L1-L8) [String resource](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values/strings.xml#L1-L4) [Launcher icon](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml#L1-L6)

## Saved state and list identity

The route, task editor task snapshot/is-new pair, editor fields, task filter, board selection, creation dialog flags, and structure editing IDs use saved state. The TaskEntity Saver stores every current property including remoteJson in an ordered ArrayList and reconstructs it by positional casts. When changing the entity fields, update this saver deliberately; a data-model change can otherwise break rotation/restoration even if Room and serialization compile. [Task Saver](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L104-L134)

Lazy task items use entity IDs, while sectioned lists prefix IDs with the section key. Cards use animateItem for placement changes. TaskSections keeps each task as a lazy item instead of rendering a long nested Column. This improves presentation behavior but does not implement database paging. [Section keys](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt#L34-L55) [Tasks keys](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskListScreen.kt#L85-L95)

## Accessibility and localization checks

The code provides text labels, standard Material controls, menu/sync content descriptions, minimum drawer row height 48 dp, and several wrapping/ellipsis accommodations. A colored status is normally accompanied by text. Nevertheless no Compose semantics test, TalkBack run, contrast measurement, keyboard-navigation test, pseudolocale test, or screen-reader acceptance result is present in this review. [Drawer touch target](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L376-L405) [Task card labels](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L174-L246)

**Recommended acceptance matrix:** light/dark; portrait/landscape; gesture/three-button navigation; display cutout; narrow and wide windows; enlarged font/display size; TalkBack completion and date-picker actions; right-to-left layout; long project names; and restored editor state. Verify task checkboxes announce enough context, error messages remain readable, and the date grid remains operable in short windows. This is proposed validation, not a certification.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)

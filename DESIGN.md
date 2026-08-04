# clamit — DESIGN.md (Material 3 Expressive, anti-slop)

Locked visual system. Future design work in this repo defers to this file.

## Direction contract

- **THESIS** — The day is one continuous rail, not a grid of floating cards. Time is the
  typographic anchor (bold HH:MM numerals on a vertical spine); blocks are stops on that
  spine with a status node. Refuses the default M3 pastel-card layout every model ships.
- **OWN-WORLD** — Warm bone paper (`#F6F4EF`) under deep teal ink (`#0E5C52`). One signal
  color: amber (`#D97706`) for "in progress". Teal owns completion. Hairline outlines
  (`outlineVariant`) instead of stacked tonal cards. Real Material icons only — no emoji
  as iconography, no emoji in empty states, no raw icon-string text rendered on cards.
- **STORY** — Open the app: the day reads top-to-bottom as one timeline. Active stop glows
  amber; finished stops fill teal; subtasks sit under their stop with hairline checks.
- **FIRST VIEWPORT** — Compact wordmark + template chip + today control. Date band:
  prev arrow — big day numeral with month/year + weekday — next arrow (both arrows always
  visible). Below: the rail with time ticks and block stops.
- **FORM** — Operate mode. System sans (Roboto/Inter), roman type only, weight steps carry hierarchy. Radius and surface language come from `MaterialExpressiveTheme` component defaults (expressive shapes, MotionScheme). One elevation language: hairline border + elevation only on the active stop.

## Tokens (Compose)

Renk sistemi: **M3 Expressive** — `MaterialExpressiveTheme` + `expressiveLightColorScheme`/`expressiveDarkColorScheme`; Android 12+ cihazlarda dynamic color (wallpaper). Expressive kimlik üç override ile taşınır:

- **`ClamitTypography`** — sistem sans (Roboto/Inter), sadece roman type, hiyerarşi weight ile kurulur. Display skalası büyük ve sıkıdır; gün rakamı rail'in tipografik çapasıdır (`displayMedium` 52sp ExtraBold, tracking −1.5sp).
- **`ClamitShapes`** — tek yuvarlak köşe ailesi (`Theme.kt`), expressive yarıçaplar (medium 16dp, large 24dp, extraExtraLarge 40dp). Kartlar/sheet'ler rail'de bağlı durak gibi okunur.
- **`MotionScheme.expressive()`** — tüm M3 bileşenlerinde Material expressive motion; aktif durak node'u nefes alır (pulse), gün değişimi crossfade.

Status renkleri (kural 2) `ClamitStatusColors`'tan (anlamsal sabitler, expressive scheme'de karşılığı yok): completed teal `#1B7A5E`/`#7BD8AE` (dark), in_progress amber `#C2641B`/`#FFB77C` (dark), pending teal `#4C7A72`/`#8FC0B7` (dark); container'lar sırasıyla `#DCEFE6`, `#FBE3C8`, `#E4ECE9` (dark: `#16382C`, `#4A2C12`, `#1E2E2B`).

## Rules

1. **No emoji as icons.** `block.icon` strings map via `ScheduleIcons` to Material
   `ImageVector`s. Emoji is banned in cards, empty states, drawer, dialogs.
2. **Status is a dot + label**, not an emoji badge: `Bekliyor` / `Devam ediyor` /
   `Tamamlandı`. Filled amber dot = in_progress, filled teal = completed, hollow = pending.
3. **Timeline rail:** continuous 2dp vertical spine per stop column; time in bold numerals
   at the rail; the stop node sits on the spine. The active node pulses on a soft amber glow.
4. **Both day arrows always visible** in the date band; never let TopAppBar actions squeeze
   them out.
5. **Roman type only** — no italic headings anywhere.
6. **Empty states teach:** real icon + one action hint; no giant emoji glyph.
7. **One card language:** hairline border (`outlineVariant`) + `surfaceContainerLow`;
   elevation (2dp) reserved for the active stop only. Corner radii come from `ClamitShapes`
   (cards use `shapes.medium`, the hero date band uses `shapes.large`).
8. **Hero date band:** month/year small-caps over a big day numeral (`displayMedium`
   ExtraBold) over the weekday in primary; today carries an amber dot.

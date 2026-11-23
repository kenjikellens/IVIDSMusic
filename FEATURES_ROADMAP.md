# IVIDS Music — Feature & Improvement Roadmap (Gesorteerd op Noodzaak)

Overzicht van ontbrekende basisfeatures en uitbreidingen, strikt gesorteerd van **P0 (essentiële basis)** tot **P3 (extra's voor later)**.

---

## P0 — Essentiële Basisfeatures (Must-Have) *(Geïmplementeerd)*

Dingen die gebruikers direct verwachten van elke dagelijkse muziekspeler.

| # | Feature | Status | Huidige situatie |
| :-: | :--- | :--- | :--- |
| 1 | **Volume & mute opslaan tussen sessies** | Gereed | Opgeslagen in DataStore + hersteld naar `AudioEngine` bij opstart. |
| 2 | **Laatste wachtrij & nummer onthouden (Session Restore)** | Gereed | Opgeslagen in `session_state.json`, hervat gepauzeerd bij opstart zonder ongevraagd autoplay. |
| 3 | **Next-Track Prefetching** | Gereed | Volgende track wordt na 2,5s afspelen op de achtergrond geresolved (+ MP3-cache op Desktop). |
| 4 | **Duidelijke foutstatus + *Opnieuw proberen*-knop in playerbalk** | Gereed | Foutstatus in `DangerCoral` + directe Retry- en Sluitknop in `DesktopPlayerDeck` en `MiniPlayerBar`. |
| 5 | **Keyboard-shortcut guard in invoervelden (Desktop)** | Gereed | `TextInputShortcutGuard` via `InterceptPlatformTextInput` blokkeert `Space`, `M`, `←`/`→` tijdens typen. |
| 6 | **Portable `ffmpeg.exe` + automatische `yt-dlp.exe` update-check** | Gereed | `DesktopNativeToolsManager` beheert portable `ffmpeg` + wekelijkse `yt-dlp`-check in `tools/`. |

---

## P1 — Dagelijkse Gebruikservaring (Core UX)

Basisfuncties die dagelijks browsen, zoeken en wachtrijbeheer soepel maken.

| # | Feature | Waarom nodig | Huidige situatie |
| :-: | :--- | :--- | :--- |
| 7 | **Recente zoekopdrachten (Search History)** | Snel eerder gezochte artiesten/nummers opnieuw openen + wissen. | Zoekbalk heeft nog geen zoekgeschiedenis. |
| 8 | **Sorteren & filteren in Bibliotheek en Playlists** | Nummers sorteren op titel, artiest, duur of datum + filter *Alleen offline*. | Lijsten hebben één vaste volgorde en alleen tekstfilter. |
| 9 | **Wachtrij herschikken (Omhoog/Omlaag of Drag-and-Drop)** | Volgorde van aankomende nummers aanpassen zonder opnieuw toe te voegen. | `QueueSideSheet` en `NowPlayingSheet` ondersteunen nu alleen springen/verwijderen. |
| 10 | **Windows Systeem-mediatoetsen (SMTC)** | Play/Pause/Next/Prev via toetsenbord-mediaknoppen terwijl de app op de achtergrond staat. | Werkt nu alleen wanneer het app-venster focus heeft. |
| 11 | **Rechtermuisknop-menu op tracks en kaarten (Desktop)** | Snelle desktop-acties (*Play Next*, *Add to Queue*, *Add to Playlist*, *Download*, *View Artist*). | Acties zitten nu achter het `⋮`-icoon of klikken op detailpagina. |
| 12 | **Live audiocache-grootte in Settings** | Inzicht in hoeveel schijfruimte `fx_audio_cache` gebruikt vóór *Clear Cache*. | Settings toont nog geen MB-teller voor cache. |

---

## P2 — Comfort & Kwaliteit (Quality of Life)

Functies die de app completer en prettiger maken zodra P0 en P1 staan.

| # | Feature | Waarom nodig |
| :-: | :--- | :--- |
| 13 | **Infinite Autoplay / Track Radio** | Automatisch vergelijkbare nummers toevoegen via `RecommendationEngine` wanneer de wachtrij eindigt. |
| 14 | **Offline-modus & netwerkstatus-indicator** | Duidelijke melding zonder internet + direct schakelen naar lokaal gedownloade tracks. |
| 15 | **YouTube-playlist importeren als IVIDS-playlist** | Een hele YouTube-playlist als afspeellijst opslaan zonder alles verplicht te hoeven downloaden. |
| 16 | **Bibliotheek Back-up (Export / Import JSON of M3U)** | Likes, playlists en voorkeuren veilig kunnen overzetten naar een ander apparaat. |
| 17 | **Minimaliseren naar System Tray (Windows)** | Muziek laten doorspelen zonder dat het venster op de taakbalk blijft staan. |
| 18 | **Venstergrootte & -positie onthouden (Desktop)** | App opent weer precies waar en hoe groot je hem afsloot i.p.v. vast `1280x820`. |

---

## P3 — Extra's / Nice-to-Have (Later)

Uitbreidingen die pas relevant zijn nadat de volledige basis op orde is.

| # | Feature | Omschrijving |
| :-: | :--- | :--- |
| 19 | **Sleep Timer** | Playback automatisch stoppen na 15/30/60 min of na het huidige nummer. |
| 20 | **Synced Lyrics (LRCLIB)** | Live meescrollende songteksten in `NowPlayingSheet`. |
| 21 | **Equalizer & Crossfade** | Audio-EQ presets en vloeiende overgangen tussen tracks. |
| 22 | **Compact Mini-Player (Always-on-Top)** | Klein zwevend desktop-venster voor tijdens gamen of werken. |
| 23 | **Discord Rich Presence** | Optioneel huidige track + albumhoes tonen op Discord. |

# IVIDS Music - Feature & Improvement Roadmap (Gesorteerd op Noodzaak)

Overzicht van ontbrekende basisfeatures en uitbreidingen, strikt gesorteerd van **P0 (essentiele basis)** tot **P3 (extra's voor later)**.

---

## P0 - Essentiele Basisfeatures (Must-Have) *(Geimplementeerd)*

Dingen die gebruikers direct verwachten van elke dagelijkse muziekspeler.

| # | Feature | Status | Huidige situatie |
| :-: | :--- | :--- | :--- |
| 1 | **Volume & mute opslaan tussen sessies** | Gereed | Opgeslagen in DataStore + hersteld naar `AudioEngine` bij opstart. |
| 2 | **Laatste wachtrij & nummer onthouden (Session Restore)** | Gereed | Opgeslagen in `session_state.json`, hervat gepauzeerd bij opstart zonder ongevraagd autoplay. |
| 3 | **Next-Track Prefetching** | Gereed | Volgende track wordt na 2,5s afspelen op de achtergrond geresolved (+ MP3-cache op Desktop). |
| 4 | **Duidelijke foutstatus + *Opnieuw proberen*-knop in playerbalk** | Gereed | Foutstatus in `DangerCoral` + directe Retry- en Sluitknop in `DesktopPlayerDeck` en `MiniPlayerBar`. |
| 5 | **Keyboard-shortcut guard in invoervelden (Desktop)** | Gereed | `TextInputShortcutGuard` via `InterceptPlatformTextInput` blokkeert `Space`, `M`, `<-`/`->` tijdens typen. |
| 6 | **Portable `ffmpeg.exe` + automatische `yt-dlp.exe` update-check** | Gereed | `DesktopNativeToolsManager` beheert portable `ffmpeg` + wekelijkse `yt-dlp`-check in `tools/`. |

---

## P1 - Dagelijkse Gebruikservaring (Core UX)

Basisfuncties die dagelijks browsen, zoeken en wachtrijbeheer soepel maken.

| # | Feature | Status | Huidige situatie |
| :-: | :--- | :--- | :--- |
| 7 | **Recente zoekopdrachten (Search History)** | Gereed | Tot 10 zoekopdrachten opgeslagen in DataStore met quick-replay, verwijderen en alles wissen in `SearchScreen`. |
| 8 | **Sorteren & filteren in Bibliotheek en Playlists** | Gereed | Sorteren op Recent, Titel, Artiest en Duur + *Alleen offline*-chip en Desktop tabelkoppen in `LibraryScreen` en `PlaylistDetailScreen`. |
| 9 | **Wachtrij herschikken (Omhoog/Omlaag)** | Gereed | *Now Playing* / *Next Up*-secties met cover art en omhoog/omlaag-knoppen in `QueueSideSheet` en `NowPlayingSheet`. |
| 10 | **Windows Systeem-mediatoetsen (SMTC)** | Open | Werkt nu wanneer het app-venster focus heeft; globale achtergrond-SMTC staat nog open. |
| 11 | **Rechtermuisknop-menu op tracks en kaarten (Desktop)** | Gereed | `onSecondaryClick` contextmenu op `TrackRowItem` en `MusicCard` (*Play Next*, *Add to Queue*, *Add to Playlist*, *Download*, *View Artist/Album*). |
| 12 | **Live audiocache-grootte in Settings** | Gereed | Live MB-teller van `fx_audio_cache` in `SettingsScreen`, bijgewerkt bij openen en na *Clear Cache*. |

---

## P2 - Comfort & Kwaliteit (Quality of Life)

Functies die de app completer en prettiger maken zodra P0 en P1 staan.

| # | Feature | Status | Huidige situatie |
| :-: | :--- | :--- | :--- |
| 13 | **Infinite Autoplay / Track Radio** | Gereed | `PlayerViewModel` voegt automatisch gerelateerde tracks toe aan het einde van de wachtrij, met toggle in Queue, Now Playing en Settings. |
| 14 | **Offline-modus & netwerkstatus-indicator** | Open | Duidelijke melding zonder internet + direct schakelen naar lokaal gedownloade tracks. |
| 15 | **YouTube-playlist importeren als IVIDS-playlist** | Open | Een hele YouTube-playlist als afspeellijst opslaan zonder alles verplicht te hoeven downloaden. |
| 16 | **Bibliotheek Back-up (Export / Import JSON of M3U)** | Open | Likes, playlists en voorkeuren veilig kunnen overzetten naar een ander apparaat. |
| 17 | **Minimaliseren naar System Tray (Windows)** | Open | Muziek laten doorspelen zonder dat het venster op de taakbalk blijft staan. |
| 18 | **Venstergrootte & -positie onthouden (Desktop)** | Gereed | Desktop-venster slaat grootte en positie op in `window_bounds.json` met monitorvalidatie bij herstart. |

---

## P3 - Extra's / Nice-to-Have (Later)

Uitbreidingen die pas relevant zijn nadat de volledige basis op orde is.

| # | Feature | Omschrijving |
| :-: | :--- | :--- |
| 19 | **Sleep Timer** | Playback automatisch stoppen na 15/30/60 min of na het huidige nummer. |
| 20 | **Synced Lyrics (LRCLIB)** | Live meescrollende songteksten in `NowPlayingSheet`. |
| 21 | **Equalizer & Crossfade** | Audio-EQ presets en vloeiende overgangen tussen tracks. |
| 22 | **Compact Mini-Player (Always-on-Top)** | Klein zwevend desktop-venster voor tijdens gamen of werken. |
| 23 | **Discord Rich Presence** | Optioneel huidige track + albumhoes tonen op Discord. |

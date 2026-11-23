# IVIDS Music — Feature & Improvement Roadmap (Gesorteerd op Noodzaak)

Overzicht van ontbrekende basisfeatures en uitbreidingen, strikt gesorteerd van **P0 (essentiële basis)** tot **P3 (extra's voor later)**.

---

## P0 — Essentiële Basisfeatures (Must-Have)

Dingen die gebruikers direct verwachten van elke dagelijkse muziekspeler en die nu nog ontbreken.

| # | Feature | Waarom nodig | Huidige situatie |
| :-: | :--- | :--- | :--- |
| 1 | **Volume & mute opslaan tussen sessies** | Niemand wil dat de app bij elke start op 100% volume schiet. | `PlayerViewModel` start altijd op `volume = 1.0f`. |
| 2 | **Laatste wachtrij & nummer onthouden (Session Restore)** | Direct verder kunnen luisteren na herstarten van de app. | Playerbalk is bij elke start leeg (`Nothing is playing`). |
| 3 | **Next-Track Prefetching** | Voorkomt 3–5 seconden stilte tussen twee nummers in een playlist/album. | Volgende track wordt pas geresolved en getranscodeerd nadat de vorige klaar is. |
| 4 | **Duidelijke foutstatus + *Opnieuw proberen*-knop in playerbalk** | Als een stream faalt, moet je direct met 1 klik kunnen herstarten. | Fout verschijnt alleen kort als toast. |
| 5 | **Keyboard-shortcut guard in invoervelden (Desktop)** | `Space`, `M`, `←`/`→` mogen playback niet triggeren terwijl je typt. | Globale `onKeyEvent` in `Main.kt` heeft nog geen focus-guard voor tekstvelden. |
| 6 | **Portable `ffmpeg.exe` + automatische `yt-dlp.exe` update-check** | Zorgt dat afspelen werkt op elke Windows-PC en niet breekt na YouTube-updates. | `ffmpeg.exe` leunt op WinGet/PATH; `yt-dlp.exe` moet handmatig worden vernieuwd. |

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

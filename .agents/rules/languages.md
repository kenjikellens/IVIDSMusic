---
trigger: always_on
---

# Translation and Language Rules

- NEVER hardcode user-facing text in HTML or JS.
- Any time new text is added to the user interface, it must be translated.
- Use the translation JSON files under the `app/src/main/assets/gui/lang/` directory.
- Update ALL 16 supported language translation files (`en.json`, `nl.json`, `de.json`, `fr.json`, `es.json`, `it.json`, `pt.json`, `ro.json`, `ru.json`, `zh.json`, `hi.json`, `ar.json`, `ja.json`, `ko.json`, `pl.json`, `tr.json`).
- **ALWAYS USE A SCRIPT FOR BATCH TRANSLATION UPDATES**: Never edit or update translation JSON files one by one manually. Always write/run a script (e.g. Node.js or Python) to process and update all language translation files in `app/src/main/assets/gui/lang/` simultaneously in a single automated step.

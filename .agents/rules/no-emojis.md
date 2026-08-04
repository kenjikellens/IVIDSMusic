---
trigger: always_on
---

# No Emojis in UI Rule

Every AI agent working on IVIDS Music MUST strictly follow this UI icon rule:

1. **NO EMOJIS IN UI**: Never use raw Unicode emojis (e.g. 🎧, 📂, ⚙️, 🎬, 🎵) in HTML templates, JavaScript rendered strings, or CSS content.
2. **USE SVG ICONS ONLY**: All icons in the user interface must be SVG files loaded from `gui/svg/` (or inline SVG definitions matching existing component standards).

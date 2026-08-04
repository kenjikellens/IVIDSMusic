# CSS Utility Class & DRY Styling Rules

Every AI agent working on IVIDS Music MUST strictly follow these CSS utility and class rules:

1. **Single Utility Class for Shared Component Behaviors**:
   When multiple UI elements (such as cards, sidebar buttons, navigation items, or poster buttons) share a visual behavior (like hover overlays, animations, or border effects), NEVER chain element selectors in CSS (`.card::before, .music-card::before, .nav-links a::before`). Instead, define **ONE single, clean utility class** (e.g., `.container-hover-effect`) in `index.css`.
2. **Apply Utility Class in HTML and JS Templates**:
   Apply that single utility class directly to the target elements in HTML (`class="container-hover-effect ..."`) and in JS template strings (`renderCard()`, `renderNavItem()`).
3. **Inherited Property Scaling**:
   Utility classes for container overlays must use `border-radius: inherit` and relative positioning so they seamlessly adapt to any container shape without duplicating styles.
4. **Base Container Composition over Standalone Classes**:
   When creating or updating pages, sections, or views, ALWAYS compose with pre-existing base container & layout classes (e.g. `class="page-container foryou-container"`, `class="page-header foryou-header"`, `class="empty-state-box foryou-empty-state"`). Never create standalone custom containers (`<div class="foryou-container">`) without attaching base CSS classes. Modifier classes should only supply page-specific tweaks.


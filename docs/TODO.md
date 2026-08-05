# IVIDS Music — TODO

- [ ] **Search Page Breakdown**: Investigate and fix why the search page is not working at all.
- [ ] **Home Page Missing Sections**: Add dedicated Artists and Albums sections to the Home page.
- [ ] **Library Layout Spacing**: Correct the spacing, margins, and padding on the "Je Bibliotheek" (Your Library) page.
- [ ] **CSS `content-visibility: auto` Layout Skip**: Apply `content-visibility: auto` and `contain-intrinsic-size: 200px` to `.row-container` in `index.css` so Chromium skips layout & paint calculations for off-screen rows.
- [ ] **DOM Virtualization for Long Scroll Lists**: Implement DOM element recycling for large search/library lists to keep max 30 active DOM card nodes in memory.
- [ ] **GPU Scroll Layer Acceleration**: Add GPU layer promotion (`transform: translateZ(0)` / `will-change: scroll-position`) to `.scroll-row` containers to prevent paint invalidation during horizontal scrolling.
- [ ] **Debounced Window Resize Scaling**: Debounce window resize handlers recalculating `--ui-base-scale` to prevent layout thrashing during window resize events.

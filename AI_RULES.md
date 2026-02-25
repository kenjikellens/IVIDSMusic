# AI Rules for IVIDS Music

When working on this project, the AI must strictly adhere to the following rules:

## 1. Responsive Design & Breakpoints
The AI MUST maintain, respect, and design for the 4 core layout breakpoints to ensure a consistent experience across all devices:
- **Desktop (Default)**: Standard wide-screen layout.
- **Tablet / Large Mobile (≤ 1200px)**: UI scales to maintain density.
- **Mobile / Portrait (Orientation: Portrait)**: Layout shifts to a bottom navigation bar, optimized for vertical screens and touch interactions.
- **Small Mobile / Compact (≤ 600px)**: UI scales down to 66% to maintain accessibility on ultra-small displays.

## 2. Cross-Environment Compatibility
The AI MUST ensure that all features, APIs, and overall systems work flawlessly in BOTH primary environments without requiring user configuration:
- **Web Browsers**: The application must function as a standalone web app (handling standard fetch requests and using public CORS proxies when outside the native shell) For example: using index.html as starting point for a live server, or putting this app on a github website.
- **Native Android (Mobile & TV)**: The application must function natively, utilizing the Kotlin backend interception (`shouldInterceptRequest`) for API requests, CORS bypassing, and audio stream extraction.

Any new features, data fetching logic, or architectural changes MUST be designed to support both environments seamlessly.

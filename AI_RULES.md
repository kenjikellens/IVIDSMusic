# AI Rules for IVIDS Music

When working on this project, the AI must strictly adhere to the following rules:

## 1. Responsive Design & Breakpoints
The AI MUST maintain, respect, and design for the granular layout hierarchy to ensure a consistent experience across all devices:
- **Standard Desktop (> 1200px)**: Default 100% scale.
- **Laptop (≤ 1200px)**: Scale to 90%.
- **Large Tablet (≤ 768px)**: Scale to 80%.
- **Phablet (≤ 720px)**: Scale to 75%.
- **Mobile (≤ 600px)**: Scale to 66%.
- **Compact Mobile (≤ 520px)**: Scale down to 60%.
- **Portrait Mode**: Mandatory layout shift (sidebar to bottom nav) for vertical screens, independent of width scale.

## 2. Cross-Environment Compatibility
The AI MUST ensure that all features, APIs, and overall systems work flawlessly in BOTH primary environments without requiring user configuration:
- **Web Browsers**: The application must function as a standalone web app (handling standard fetch requests and using public CORS proxies when outside the native shell) For example: using index.html as starting point for a live server, or putting this app on a github website.
- **Native Android (Mobile & TV)**: The application must function natively, utilizing the Kotlin backend interception (`shouldInterceptRequest`) for API requests, CORS bypassing, and audio stream extraction.

Any new features, data fetching logic, or architectural changes MUST be designed to support both environments seamlessly.

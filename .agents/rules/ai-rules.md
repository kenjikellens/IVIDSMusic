---
trigger: always_on
---

# AI Rules for IVIDS Music

When working on this project, you must strictly follow these rules to maintain the integrity and quality of the application:

## 1. Responsive Design & Breakpoints
You must maintain, respect, and design for the granular layout hierarchy. This system ensures the UI scales correctly from small phones to large monitors.
- **The 5 Width Breakpoints**: You must design for 1200px, 1024px, 768px, 700px, and 600px. The `--ui-base-scale` variable changes at each of these points.
- **Portrait Mode**: You must ensure the layout shifts to a vertical orientation (sidebar disappears, bottom navigation appears) when the device is in portrait mode. This is critical for the mobile experience and is independent of the width scale.

## 2. Cross-Environment Compatibility
You must ensure that your code works flawlessly in both primary environments:
- **Web Browsers**: The application must function as a standalone web app (e.g., via VS Code Live Server or GitHub Pages). Use public CORS proxies when necessary.
- **Native Android (Mobile & TV)**: The application must function natively within an Android WebView, taking into account the Kotlin backend that intercepts requests via `shouldInterceptRequest`.
- **Universal Code**: Do not write code that only works in one environment. Data fetching, API requests, and audio extraction logic must support both seamlessly.

## 3. Git & GitHub Protocol
- **No Autonomous Pushing**: You must NEVER push changes to the GitHub repository on your own initiative.
- **Explicit Permission Required**: You must always ask for explicit permission from the user before using `git push`. Keep your changes in the local workspace until you are instructed to push them to the cloud.

Always follow these rules for every task you perform.
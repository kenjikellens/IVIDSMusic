---
trigger: always_on
---

# MANDATORY CHANGELOG PROTOCOL

Every single file modification (create, edit, or delete) MUST be logged in `CHANGELOG.md` immediately!

1. **TRIGGER**: This is not a task-end requirement; it is a "Post-Edit Hook." As soon as a file is successfully modified, your VERY NEXT action must be to update the changelog before proceeding to the next file or responding to the user.
2. **GRANULARITY**: You must add exactly one line for every file changed. Do not group multiple files into one line.
3. **FORMAT BINDING**: Every entry must follow this exact template: 
   `[YYYY-MM-DD] [ACTION] <filename>: <brief description of what was changed/added/deleted>`
   - **ACTION** must be one of: `EDITED`, `ADDED`, or `DELETED`.
   - Description must be concise but specific enough to understand what changed and why.
4. **CHRONOLOGICAL ORDER**: Every new entry MUST be appended to the **bottom** of the file, meaning the newest log is always at the bottom.
5. **RESET ON RELEASE**: The `CHANGELOG.md` file must ONLY contain the latest changes since the last release. As soon as a new release version is tagged, compiled, or pushed, you MUST clear `CHANGELOG.md` (start it fresh) and push this cleared state to `main` immediately to prepare for the next release cycle.
6. **LOCATION**: Update the file located at [CHANGELOG.md](file:///c:/Users/kenji/Documents/PROJECTS/IVIDS%20Music/documentation/CHANGELOG.md).
7. **SYNC REQUIREMENT**: If committing or pushing code, the changelog update must be included in the same commit.
8. **EXCLUDED FILES**: You must NEVER log modifications to agent-internal files (e.g., `.agents/` folder, workflows, system rules) or documentation files (e.g., `documentation/` folder, `CHANGELOG.md` itself) in `CHANGELOG.md`. The changelog tracks codebase/application changes only.

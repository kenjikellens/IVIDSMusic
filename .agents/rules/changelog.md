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
4. **LOCATION**: Update the file located at [CHANGELOG.md](file:///c:/Users/kenji/AndroidStudioProjects/IVIDSMusic/documentation/CHANGELOG.md).
5. **SYNC REQUIREMENT**: If committing or pushing code, the changelog update must be included in the same commit.

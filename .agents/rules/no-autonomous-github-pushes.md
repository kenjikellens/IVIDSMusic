---
trigger: always_on
---

# NO AUTONOMOUS GITHUB RELEASES OR PUSHES

Every AI agent working on IVIDS Music MUST strictly adhere to this constraint:

1. **NO AUTONOMOUS COMMITS OR PUSHES**: You are strictly prohibited from performing any Git commits (`git commit`) or remote Git pushes (`git push`) autonomously. All edits must remain in the working tree so the user can easily review the changes.
2. **EXPLICIT PERMISSION REQUIRED**: You must always ask for explicit permission from the user before using `git push`. Keep your changes in the local workspace until you are instructed to push them to the cloud.
   - If the user says **"push to main"**, you may directly push ALL changes to main.
   - If the user says **"push [file] to main"**, you only push this specific file.
   - **SINGLE-TURN AUTHORIZATION**: Authorization to commit and push applies ONLY to the active changes discussed in that specific turn. If you make any subsequent or additional edits in a later turn, you MUST obtain fresh, explicit authorization before performing another commit and push.
3. **TAG AND PUSH PROTOCOL**: When instructed to tag and push:
   - You MUST ensure the commit message and release details exactly follow the target version's release description.
   - You must never perform Git push actions automatically as part of a background execution loop without explicit developer oversight.
4. **CODE PUSH VS. RELEASE SEPARATION**: If the user instructs you to 'push to main', this authorizes ONLY pushing the active commit history of the branch (`git push origin main`). It does NOT authorize creating a Git tag, drafting a GitHub release, or triggering release packaging/distribution pipelines unless the user explicitly and separately specifies 'create a release' or 'tag and push release' in the request.
5. **COMBINED REQUEST AUTHORIZATION**:
   - If the user says **"push to main"** while also explicitly asking to create/publish a release in the same active request, that single request authorizes both the `main` push and the release publication workflow for the named version.
   - If the user only asks to make/create/publish a release `vX.Y.Z`, this authorizes release publication for that version only. Push the release tag and create the GitHub Release, but do **not** push `main`.
   - If the user only says **"push to main"**, this authorizes only `git push origin main`; it does **not** authorize release packaging, tag pushes, or GitHub Release creation.

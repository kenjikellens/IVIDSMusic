---
trigger: always_on
---

# NO AUTONOMOUS GITHUB RELEASES OR PUSHES

Every AI agent working on IVIDS Music MUST strictly adhere to this constraint:

1. **NO AUTONOMOUS COMMITS OR PUSHES**: You are strictly prohibited from performing any Git commits (`git commit`) or remote Git pushes (`git push`) autonomously. All edits must remain in the working tree so the user can easily review the changes.
2. **EXPLICIT PERMISSION REQUIRED**: You must always ask for explicit permission from the user before using `git push`. Keep your changes in the local workspace until the user's intent clearly authorizes uploading local commits or refs to GitHub.
   - Treat contextual instructions such as **"push it"**, **"push these changes"**, **"upload/sync this to GitHub"**, **"publish main"**, **"put this on GitHub"**, or **"push the current branch"** as permission to push the active branch and its current local commits.
   - If the user limits the push to a specific file, commit, branch, tag, or ref, push only that requested scope.
   - **SINGLE-TURN AUTHORIZATION**: Authorization to commit and push applies ONLY to the active changes discussed in that specific turn. If you make any subsequent or additional edits in a later turn, you MUST obtain fresh, explicit authorization before performing another commit and push.
3. **TAG AND PUSH PROTOCOL**: When instructed to tag and push:
   - You MUST ensure the commit message and release details exactly follow the target version's release description.
   - You must never perform Git push actions automatically as part of a background execution loop without explicit developer oversight.
4. **CODE PUSH VS. RELEASE SEPARATION**: A contextual branch-push request authorizes ONLY pushing the active commit history of the requested branch (for example, `git push origin main`). It does NOT authorize creating a Git tag, drafting a GitHub release, or triggering release packaging/distribution pipelines unless the user explicitly and separately requests release work.
5. **COMBINED REQUEST AUTHORIZATION**:
   - If the user clearly asks to push/upload/sync the branch while also explicitly asking to create/publish a release in the same active request, that single request authorizes both the branch push and the release publication workflow for the named version.
   - If the user only asks to make/create/publish a release `vX.Y.Z`, this authorizes release publication for that version only. Push the release tag and create the GitHub Release, but do **not** push `main`.
   - If the user only asks to push/upload/sync the branch, this authorizes only the branch push; it does **not** authorize release packaging, tag pushes, or GitHub Release creation.

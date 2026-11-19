# Agent notes (Nexus / api)

## Git workflow

- Make commits and pushes on the **production server** only:
  - host: `liwei@47.122.123.12`
  - path: `/srv/blog/apps/api`
- Do **not** commit or push from a local workstation copy.
- Transfer a patch containing only the reviewed changes. Run `git apply --check` before applying it in an isolated branch or worktree on the server, then stage, commit, and push the branch there.
- Open a PR into `main` and own delivery through merge and production verification. Use the engineering-governance skill and follow GitHub's review eligibility, required checks, and environment approval rules.
- Never commit secrets (`.env`, API keys, credential screenshots).
- Before pushing Java changes, run Spotless (e.g. `./mvnw -B -ntp spotless:apply` / `spotless:check`) so GitHub Actions does not fail.

# Agent notes (Nexus / api)

## Git workflow

- Make commits and pushes on the **production server** only:
  - host: `liwei@47.122.123.12`
  - path: `/srv/blog/apps/api`
- Do **not** commit or push from a local workstation copy.
- Sync local edits to the server (e.g. rsync), then `git add` / `git commit` / `git push` there.
- Never commit secrets (`.env`, API keys, credential screenshots).
- Before pushing Java changes, run Spotless (e.g. `./mvnw -B -ntp spotless:apply` / `spotless:check`) so GitHub Actions does not fail.

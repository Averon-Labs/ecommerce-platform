# Deployment Assets

Prepared files for the optional ByetHost demo. Full instructions, environment guidance, and verification
checklist: [`docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md).

## Admin update archive

```text
output/eshopverse-admin.zip   # ~1.08 MB; contents extract directly into htdocs/
├── index.html
├── assets/
└── fonts/
```

The current packages (admin bundle **and** the Laravel application archive) are generated under
`output/`; see [`docs/DEPLOYMENT.md`](../DEPLOYMENT.md#upload-packages-2026-10-28). The earlier
`deploy/eshopverse-admin-dist.zip` name described the same admin layout and is no longer kept in the tree.

Upload the archive to `htdocs`, extract it over the existing `index.html`, `assets/`, and `fonts/`, delete any
old `_worker.js`, then hard-refresh. The backend and database are unchanged by the theme update.

## Laravel front controller

```text
deploy/byethost/htdocs/index.php
deploy/byethost/htdocs/.htaccess
```

These are the same files already deployed at the host root. Keep them if you rebuild the host. `.htaccess`
is hidden by default in the file manager; enable “Show hidden files” before uploading it.

## Security

- Never commit `eshopverse_app/.env`, database passwords, `APP_KEY`, or the demo administrator password.
- The live Laravel app lives in `htdocs/eshopverse_app/`; its own `.htaccess` denies direct web access.
- Use HTTPS and keep `APP_DEBUG=false` on the host.

The `*.zip` archive is intentionally ignored by Git (`.gitignore`); the source of truth is `admin/` plus
`npm run build`. The deployment scripts in this folder are tracked.

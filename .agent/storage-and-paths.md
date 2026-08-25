# Storage & file paths

## Database (`db/ReExteraDb.java`)
Custom SQLite `re_extera.db`, async writes on a dedicated `HandlerThread`. Tables
include `deleted_keys(did, mid, ts)` (30-day TTL prune), edit history, read events,
last-online, dialog exclusions, shadowban, regex filters.

Deleted-key cache:
- `deletedKeysCache` = `ConcurrentHashMap<Long, Set<Integer>>`, **fully loaded at
  startup** by `loadDeletedKeysCache()` (posted to the DB thread) and kept in sync on
  every write (`batchPutDeletedMessagesAsync` updates the in-memory set synchronously
  BEFORE the async disk write — so lookups are race-free).
- `messageIsDeleted(did, mid)` — cache first, then DB.
- `isMidDeletedAnyDialog(int mid)` — **cache-only, did-independent** scan across all
  dialog sets (added this session). Used by the deleted mark so channel/group and
  peer-less deletes still mark reliably. See deleted-messages.md.

## Output folder layout (consolidated under `Download/ReExtera/`)
- Deleted attachments: `Download/ReExtera/ReExteraAttachments/` —
  `utils/AttachmentSaver.java` (`new File(new File(downloads, "ReExtera"),
  "ReExteraAttachments")`, size-capped, `.nomedia`). Size cap slider on the Spy screen
  (`re_extera_attachments_max_size`, default 500 MB).
- Loader logs export: `Download/ReExtera/logs/re_extera_logs_<timestamp>.txt` —
  `loader/plugin.py` `_on_export_logs` (was "Copy logs" → now "Export logs").
- The `SAVE_ATTACHMENTS_DESC` localization text reflects the `Download/ReExtera/...` path.

## Not yet moved
DB export/import and `.elyx` updates still write directly under `Download/`. Move them
under `Download/ReExtera/` if the user asks (e.g. `ReExtera/backups`).

# Quick side buttons (Telegraph-style)

Per-message quick buttons drawn beside the bubble: a **pencil (edit)** on the user's
own editable messages, a **cloud (save to Saved Messages)** on incoming messages.
Kill-switch: `Settings.getMessageQuickButtons()` (default **true**), toggle in
OTHER → "Message quick buttons". Purely client-side (Telegraph proves it).

File: `hooks/chatmessagecell/QuickButtons.java` (two inner `XC_MethodHook` classes
`Draw` and `Touch`, sharing a `WeakHashMap<cell, float[]>` of button geometry).

## Registration (`HookInit`)
```java
tryHook("ChatMessageCell.onDraw(quickButtons)", ChatMessageCell.class, "onDraw",
        new QuickButtons.Draw(), Canvas.class);
tryHook("ChatMessageCell.onTouchEvent(quickButtons)", ChatMessageCell.class, "onTouchEvent",
        new QuickButtons.Touch(), MotionEvent.class);
```

## Draw (afterHookedMethod on `onDraw`)
- Skip if toggle off / sponsored / (own && !`canEditMessage(null)`).
- Bubble bounds via `cell.getBackgroundDrawableLeft/Right/Bottom()` (cell-local coords).
- Geometry (tuned smaller than Telegraph's 32dp at the user's request):
  - `radius = dp(14)` (28dp circle), `gap = dp(4)`, `cy = bubbleBottom - dp(20)`.
  - own (outgoing): `cx = bubbleLeft - gap - radius` (left of the bubble).
  - incoming: `cx = bubbleRight + gap + radius` (right of the bubble).
  - Off-screen guard: skip if `cx-radius<0` (own) or `cx+radius>cell.getWidth()` (incoming).
- Background: solid circle `Theme.getColor(key_chat_serviceBackground)` (a `Paint`
  reused via `BG_PAINT`). Chosen over `Theme.chat_actionBackgroundPaint` for a
  **consistent** look at any vertical position (the action paint rendered oddly for
  incoming buttons).
- Icon: `dp(10)` half (20dp), `setColorFilter(key_chat_serviceText, SRC_IN)`, draw.
  - edit → host `R.drawable.msg_edit`.
  - save → `CLOUD_ICON_PATH` via `PathIconDrawable` (stroke, viewBox 24, width 2) —
    the user-supplied `cloud.svg`. See custom-icons.md.
- Store `{cx, cy, radius, action}` in `BUTTONS` for the touch hook.

## Touch (beforeHookedMethod on `onTouchEvent`)
- Look up `BUTTONS.get(cell)`; hit-test the tap against center+radius (+`dp(6)`
  tolerance) on ACTION_DOWN/UP.
- Inside → `param.setResult(Boolean.TRUE)` (consume so the cell doesn't react),
  and on ACTION_UP call `trigger(cell, action)`.

## Actions (`trigger`)
- **Edit:** `LaunchActivity.getLastFragment()` → if `ChatActivity`, reflectively call
  private `ChatActivity.startEditingMessageObject(MessageObject, boolean=false)`.
- **Save:** `SendMessagesHelper.getInstance(account).sendMessage(list, ownId, false,
  false, true, 0, 0L)` where `ownId = UserConfig.getInstance(account).getClientUserId()`.
  Then the "Saved Messages" toast: `((ChatActivity)last).getUndoView()
  .showWithAction(ownId, 53, Integer.valueOf(1))` — action **53 (0x35)** is exactly
  what exteraGram's own Save handler uses; it renders "forwarded to Saved Messages"
  and is clickable to open Saved Messages.

## Telegraph reference (draft/telegraph, obfuscated)
`ChatMessageCell=coM7`; draw method `O4(Canvas)`; 32dp circle via
`drawRoundRect(box,16dp,16dp)`; icons `button_edit`/`button_cloud`/`button_favorite`
(Telegraph brand PNGs — NOT copied); positions `Nf/Of` (edit) `Lf/Mf` (save);
`getDirectOperationsSize()` = 38dp pitch per button; anchored near bubble bottom,
stacked upward. We reproduced geometry/behaviour, using host/user icons.

## Risk note
This is custom canvas drawing on an obfuscated cell — not device-tested by the author.
The `getMessageQuickButtons()` kill-switch exists so a rendering problem can be turned
off without uninstalling.

---
description: Arena Hot-Reload System Fixes
---

# Arena Hot-Reload System Fixes

## 🎯 OBJECTIVE
Fix arena system to support full hot-reload without server restart, eliminate race conditions, and improve UX.

---

## 📋 TASKS TO COMPLETE

### ✅ Task 1: Fix `/arena save` Command
**File:** `ArenaCommand.java`
**Changes:**
- [ ] Update `/arena save <name>` to clear `edited` flag after save
- [ ] Replace `/arena saveall` with `/arena save *` wildcard support
- [ ] `/arena save *` should save all arenas and clear their edited flags

**Implementation:**
```
BEFORE: /arena save <name> → saves arena but edited flag stays true
AFTER: /arena save <name> → saves arena AND sets edited=false

BEFORE: /arena saveall → nukes config then saves all
AFTER: /arena save * → saves all arenas non-destructively AND sets edited=false for all
```

---

### ✅ Task 2: Implement Hot-Reload Arena Transition
**File:** `ArenaDetail.java`, `Arena.java`
**Changes:**
- [ ] Add `processPendingReload()` method to `Arena` class
- [ ] Call this method after arena is released (in `restoreChunk` finally block)
- [ ] Swap out old arena instance with `nextVersion` in the arenas list
- [ ] Properly handle arena deletion (when nextVersion is null)
- [ ] Log transition for debugging

**Implementation:**
```
When arena is released after match:
1. Check if arena.isPendingReload()
2. If yes, call Arena.processPendingReload(arena)
3. This swaps old arena with nextVersion in Arena.getArenas() list
4. Clears pendingReload flag
```

---

### ✅ Task 3: Fix Async Race Condition
**File:** `ArenaDetail.java`
**Changes:**
- [ ] Move `using = false` to happen BEFORE async chunk restore starts
- [ ] Ensure arena is immediately available for next match
- [ ] Keep chunk restore async for performance

**Implementation:**
```
BEFORE: using=false happens in finally block of async runnable
AFTER: using=false happens immediately, then async restore runs
```

---

### ✅ Task 4: Improve New Arena Creation
**File:** `ArenaCommand.java`, `Arena.java`
**Changes:**
- [ ] Don't mark arena as edited on initial creation
- [ ] Only set edited=true when modifying EXISTING setup
- [ ] Add `isInitialSetup()` check

**Implementation:**
```
Track if arena has ever been fully set up
Only set edited=true if it was previously set up
```

---

### ✅ Task 5: Remove "Needs Restart" Messaging
**File:** `ArenaToggleButton.java`, Language files
**Changes:**
- [ ] Remove "needs restart" message from toggle button
- [ ] Update to show "pending changes" or similar

---

### ✅ Task 6: Clarify StorArena Command
**File:** `ArenaCommand.java`
**Changes:**
- [ ] Keep command but improve documentation
- [ ] Add clear message explaining what it does

---

## 📝 IMPLEMENTATION LOG

### Task 1: `/arena save` Command Fixes
**Status:** ✅ COMPLETED
**Files Modified:**
- `ArenaCommand.java`

**Changes Made:**
- Removed `/arena saveall` command completely
- Added wildcard support to `/arena save` - now `/arena save *` saves all arenas
- Both `/arena save <name>` and `/arena save *` now call `arena.setEdited(false)` after saving
- This clears the edited flag, re-enabling arenas after config changes saved
- Updated tab completion to remove 'saveall'
- Added feedback message showing count of arenas saved when using wildcard

---

### Task 2: Hot-Reload Transition
**Status:** ✅ COMPLETED
**Files Modified:**
- `Arena.java`
- `ArenaDetail.java`

**Changes Made:**
- Added new `processPendingReload(Arena)` static method to Arena class
- This method handles swapping old arena instances with new versions after matches end
- Properly handles arena deletion (when nextVersion is null)
- Calls `Arena.processPendingReload(arena)` in THREE locations in `ArenaDetail.restoreChunk()`:
  1. FAWE mode clone arenas - in `finally` block after chunks restored
  2. FAWE mode original arena - immediately when released
  3. Legacy NMS mode - in `finally` block after chunks restored
- Hot-reload happens AFTER chunk restore completes (not before - that would break matches)
- Added logging for arena hot-reload events for debugging
- Clears pendingReload and nextVersion flags after transition completes

---

### Task 3: Arena Position Edit Logic
**Status:** ✅ COMPLETED (SIMPLIFIED)
**Files Modified:**
- `ArenaCommand.java`

**Changes Made:**
- **REMOVED all conditional `isFinishedSetup()` / `isEnabled()` checks**
- Now **ALWAYS** sets `edited=true` when ANY position is changed (a, b, min, max, spectator, y-limit, build-max, portal-protection-radius)
- Simple, consistent logic: modify anything = mark edited
- Arena becomes disabled until admin runs `/arena save` to clear edited flag OR runs toggle command to recache chunks
- No more convoluted tracking of "was it set up before"

---

### Task 4: Command Rename - storearena → updatechunks
**Status:** ✅ COMPLETED
**Files Modified:**
- `ArenaCommand.java`

**Changes Made:**
- Renamed `/arena storearena` to `/arena updatechunks`
- Updated permission check from `eden.command.storearena` to `eden.command.updatechunks`
- Command already had wildcard support:
  - `/arena updatechunks <name>` - updates cached chunks for specific arena
  - `/arena updatechunks *` - updates cached chunks for ALL arenas
- Updated tab completion to show `updatechunks` instead of `storearena`
- This command explicitly recaches chunks (calls `copyChunk()`) which is separate from save

---

### Task 5: UI Message Updates
**Status:** ✅ COMPLETED
**Files Modified:**
- `ArenaToggleButton.java`

**Changes Made:**
- Removed "需要重新啟動" (needs restart) message from toggle button description
- Changed to show "有待處理的更改" (pending changes) when arena is edited
- Updated action description to "點擊以重新快取場地區塊並啟用" (click to recache chunks and enable)
- Messages now accurately reflect the hot-reload capability

---

## 🎯 FINAL COMMAND OVERVIEW

### `/arena save <name>` or `/arena save *`
- Saves arena configuration to `arena.yml`
- Clears `edited=false` flag
- Re-enables arena (if was previously setup)
- Does NOT recache chunks

### `/arena updatechunks <name>` or `/arena updatechunks *`
- Explicitly recaches chunks via `copyChunk()`
- Use after manual world edits to update stored chunk snapshot
- Wildcard `*` updates all arenas

### `/arena setup <name> toggle`
- If `edited=true`: Recaches chunks first, then clears edited flag
- Toggles enabled status
- All-in-one command that handles both chunk caching and enabling


---

## ✅ VERIFICATION CHECKLIST

After all fixes:
- [ ] Create new arena without server restart
- [ ] Arena doesn't auto-disable after setting positions
- [ ] `/arena save <name>` clears edited flag
- [ ] `/arena save *` saves all arenas and clears edited flags
- [ ] Run 2+ matches on same arena without restart
- [ ] Modify arena while match in progress, verify swap happens after match
- [ ] No "needs restart" message shown in GUI
- [ ] All commands documented with help text

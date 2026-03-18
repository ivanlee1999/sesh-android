# Sesh Android App — Palma 2 Pro (3.5" Color E-ink)

## Context

The `sesh` pomodoro timer exists as a Go terminal app at `/home/ivan/projects/sesh`. This plan designs a native Android port optimized for the Palma 2 Pro — a 3.5" color e-ink phone where slow refresh rates, limited color gamut, and small screen size drive every UI decision. The app will be created at `/home/ivan/projects/sesh-android/`.

The goal: full feature parity with the terminal app, but with an e-ink-native UI that feels like it was designed for paper, not glass.

---

## Architecture: MVVM + Repository + Foreground Service

```
┌───────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)               │
│  Screens observe ViewModel StateFlows     │
├───────────────────────────────────────────┤
│  ViewModel Layer                          │
│  Holds UiState, calls Repos/UseCases      │
│  TimerVM binds to TimerService            │
├───────────────────────────────────────────┤
│  Domain Layer                             │
│  Kotlin data classes, enums, use cases    │
├───────────────────────────────────────────┤
│  Data Layer                               │
│  Room (SQLite), DataStore (prefs),        │
│  Calendar sync (AppAuth + REST)           │
├───────────────────────────────────────────┤
│  Service Layer                            │
│  TimerService (ForegroundService)         │
│  CalendarSyncWorker (WorkManager)         │
└───────────────────────────────────────────┘
```

**Tech stack:** Kotlin, Jetpack Compose, Room, Hilt, DataStore, WorkManager, AppAuth, Coroutines + Flow.

**Why Compose over XML:** Compose allows fine-grained recomposition control (critical for e-ink — only redraw changed pixels), custom drawing primitives for the segmented progress bar and bar chart, and simpler state management with the service-bound timer.

---

## Project Structure

```
sesh-android/
  app/src/main/java/com/sesh/app/
    SeshApplication.kt
    MainActivity.kt
    navigation/
      SeshNavGraph.kt
      Screen.kt                          # Sealed class: Timer, Analytics, History, Settings
    data/
      db/
        SeshDatabase.kt                  # Room DB, v1, seeds default categories
        entity/
          CategoryEntity.kt
          SessionEntity.kt
          PauseEntity.kt
        dao/
          CategoryDao.kt
          SessionDao.kt
      repository/
        SessionRepository.kt
        CategoryRepository.kt
        AnalyticsRepository.kt
      preferences/
        UserPreferences.kt              # Data class with all config fields
        PreferencesRepository.kt         # DataStore wrapper
      calendar/
        GoogleCalendarSync.kt
        OutlookCalendarSync.kt
        CalendarSyncWorker.kt            # WorkManager retry
        AuthStateManager.kt             # EncryptedSharedPreferences for tokens
    domain/
      model/
        TimerPhase.kt                    # Enum: Idle, Focus, Overflow, Paused, Break, BreakOverflow, Abandoned
        TimerState.kt                    # Data class with phase, remaining, elapsed, target, etc.
        SessionType.kt                   # Enum: FullFocus, PartialFocus, Rest, Abandoned
        BreakType.kt                     # Enum: Short, Long
        Category.kt
        Session.kt
        AnalyticsSummary.kt
    service/
      TimerService.kt                    # ForegroundService — the heart of the app
      TimerBroadcastReceiver.kt          # Handles notification button intents
    ui/
      theme/
        EinkTheme.kt
        EinkColors.kt                    # High-contrast, saturated palette
        EinkTypography.kt               # Bold, large type scale
      components/
        EinkProgressBar.kt              # Segmented (20 blocks, no smooth fill)
        EinkButton.kt                   # 56dp tall, sharp corners, 2dp border
        EinkTimerDisplay.kt             # 56sp monospace, phase-colored
        EinkBottomBar.kt                # 4-tab nav
        EinkBarChart.kt                 # 7-day vertical bars
        EinkCategoryChip.kt
      screen/
        timer/
          TimerScreen.kt
          TimerViewModel.kt
          IdleContent.kt
          ActiveTimerContent.kt
          BreakContent.kt
          SessionCompleteSheet.kt
          PostSessionContent.kt
        analytics/
          AnalyticsScreen.kt
          AnalyticsViewModel.kt
        history/
          HistoryScreen.kt
          HistoryViewModel.kt
        settings/
          SettingsScreen.kt
          SettingsViewModel.kt
    di/
      DatabaseModule.kt
      RepositoryModule.kt
      PreferencesModule.kt
```

---

## Data Model (Room — matches Go schema exactly)

Port the schema from `/home/ivan/projects/sesh/internal/db/db.go` (migration001).

### CategoryEntity
```
id: String (PK, UUID)
title: String
hex_color: String (default "#61AFEF")
status: String ("active" | "archived")
sort_order: Int (default 0)
created_at: String (ISO 8601)
updated_at: String (ISO 8601)
```

### SessionEntity
```
id: String (PK, UUID)
title: String (intention, default "")
category_id: String? (FK → categories, ON DELETE SET NULL)
session_type: String ("full_focus" | "partial_focus" | "rest" | "abandoned")
target_seconds: Long
actual_seconds: Long
pause_seconds: Long (default 0)
overflow_seconds: Long (default 0)
started_at: String (ISO 8601)
ended_at: String (ISO 8601)
notes: String?
created_at: String (ISO 8601)
```
Indices: `started_at`, `category_id`, `session_type`

### PauseEntity
```
id: String (PK)
session_id: String (FK → sessions, ON DELETE CASCADE)
paused_at: String
resumed_at: String?
created_at: String
```

### Default Categories (seeded on first run)
Development (#61AFEF), Writing (#E06C75), Design (#C678DD), Research (#E5C07B), Meeting (#56B6C2), Exercise (#98C379), Reading (#D19A66), Admin (#ABB2BF)

### Analytics Queries (port from `/home/ivan/projects/sesh/internal/db/sessions.go`)
- **Today's focus**: `SUM(actual_seconds - pause_seconds) / 60.0` where focus sessions today
- **Streak**: Recursive CTE counting consecutive days with focus sessions backward from today
- **Category breakdown**: Group by category, sum minutes, sort desc
- **7-day chart**: Recursive CTE generating last 7 days, LEFT JOIN with daily sums
- **All-time total**: Sum all focus session durations

Use `@RawQuery` for the two recursive CTEs (Room's `@Query` doesn't always handle `WITH RECURSIVE`).

---

## Timer Service Design

**File:** `service/TimerService.kt` — the most critical component.

### Why ForegroundService
The timer must survive: app backgrounded, screen off, Doze mode. A ViewModel alone cannot guarantee this. The service runs with a persistent notification.

### Timing Strategy
- Use `SystemClock.elapsedRealtime()` (monotonic, survives sleep) for all time calculations
- `AlarmManager.setExactAndAllowWhileIdle()` to schedule phase-completion alarms (focus done, break done) — fires even in Doze
- Handler-based tick for display updates: **every 15 seconds** normally, **every 1 second** in the final 60 seconds
- Expose `StateFlow<TimerState>` for ViewModel to collect via bound service

### State Machine (mirrors Go's `/home/ivan/projects/sesh/internal/state/state.go`)
```
Idle → [start] → Focus → [timer=0] → Overflow → [finish] → (save session)
Focus/Overflow → [pause] → Paused → [resume] → Focus/Overflow
Focus/Overflow → [abandon] → Abandoned (5s undo window) → Idle
Idle → [break] → Break → [timer=0] → BreakOverflow → [end] → Idle
```

### Service Actions (via Intent)
`ACTION_START_FOCUS`, `ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_FINISH`, `ACTION_ABANDON`, `ACTION_UNDO_ABANDON`, `ACTION_START_BREAK`, `ACTION_FINISH_BREAK`

### Session Calculation (on finish, matching Go's `app.go` logic)
1. `actualSeconds` = wall-clock elapsed - total pause duration
2. `overflowSeconds` = max(0, actualSeconds - targetSeconds)
3. `sessionType` = if actualSeconds >= targetSeconds then `full_focus` else `partial_focus`
4. Save to Room, enqueue calendar sync, fire completion notification

### Crash Recovery
Serialize active session state to DataStore. On `START_STICKY` restart, restore and resume timing.

---

## E-ink UI Design

### Color Palette (white background, inverted from Go's dark theme)
```
Background:     White          (e-ink's natural state)
OnBackground:   Black
Surface:        #F0F0F0        (very light gray for cards)

Focus:          #2E7D32        (deep green — saturated for e-ink)
Overflow:       #E65100        (deep orange)
Break:          #1565C0        (deep blue)
Paused:         #6A1B9A        (deep purple)
Abandoned:      #C62828        (deep red)

Category colors: same hues as Go but deeper saturation:
  Development:  #1976D2    Writing:  #C62828    Design:   #7B1FA2
  Research:     #F9A825    Meeting:  #00838F    Exercise: #2E7D32
  Reading:      #E65100    Admin:    #616161
```

### Typography
- Timer display: **56sp monospace bold** (readable from arm's length)
- Phase label: **20sp bold**
- Body: **16sp**
- Minimum touch target: **56dp** (larger than Material's 48dp — for e-ink touchscreen accuracy)

### Key E-ink Rules
1. **No animations** — no `animateFloatAsState`, no transitions. Instant state changes only.
2. **Segmented progress bar** — 20 solid blocks, not a smooth fill. Updates discretely.
3. **Sharp corners everywhere** — `RectangleShape` on all buttons/cards. Rounded corners ghost.
4. **Heavy borders** — 2dp black borders on all interactive elements (buttons, inputs).
5. **Monospace timer** — prevents layout shifts on digit changes (reduces partial refreshes).
6. **Throttled updates** — ViewModel emits UI state every 15s (1s in final minute).
7. **Minimal recomposition** — each section is its own composable, isolated inputs.

---

## Screen Wireframes (320x480dp)

### Timer — Idle
```
┌──────────────────────────┐
│  ┌──────────────────┐    │
│  │ Set intention...  │    │  text field
│  └──────────────────┘    │
│  [■ Development      ▼]  │  category dropdown
│                          │
│        25:00             │  56sp, black
│   [-5] [-1]  [+1] [+5]  │  duration adjust
│                          │
│  ╔══════════════════╗    │
│  ║   START FOCUS    ║    │  56dp, black bg
│  ╚══════════════════╝    │
│  [Short Break] [Long Brk]│  secondary buttons
│                          │
│  Today: 2h15m  5 sess    │
│  Streak: 12 days         │  compact stats
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│  bottom nav
└──────────────────────────┘
```

### Timer — Focus Active
```
┌──────────────────────────┐
│         FOCUS            │  green label
│  "Write architecture doc"│
│  ■ Development           │
│                          │
│       18:30              │  56sp green, updates every 15s
│                          │
│  [████████░░░░░░░░░░░]   │  segmented progress
│                          │
│  ┌────────┐ ┌────────┐   │
│  │ PAUSE  │ │ FINISH │   │  56dp buttons
│  └────────┘ └────────┘   │
│      [ABANDON]           │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

### Timer — Overflow
```
┌──────────────────────────┐
│       OVERFLOW           │  orange label
│  "Write architecture doc"│
│  ■ Development           │
│                          │
│      +03:15              │  56sp orange, counts up
│                          │
│  [████████████████████+] │  overfilled, orange
│                          │
│  ┌────────┐ ┌────────┐   │
│  │ PAUSE  │ │ FINISH │   │
│  └────────┘ └────────┘   │
│      [ABANDON]           │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

### Session Complete (replaces timer area)
```
┌──────────────────────────┐
│     Session Complete     │
│  Duration: 28m 15s       │
│  Type: Full Focus        │
│  Category: Development   │
│                          │
│  Notes (optional):       │
│  ┌──────────────────┐    │
│  │                  │    │
│  └──────────────────┘    │
│  ╔══════════════════╗    │
│  ║       SAVE       ║    │
│  ╚══════════════════╝    │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

### Post-Session
```
┌──────────────────────────┐
│     Session Saved!       │
│  28m 15s ■ Development   │
│                          │
│  Long break suggested    │  (if cumulative >= 100m)
│  (105m focused today)    │
│                          │
│  ╔══════════════════╗    │
│  ║   SHORT BREAK    ║    │
│  ╚══════════════════╝    │
│  ╔══════════════════╗    │
│  ║   LONG BREAK     ║    │  highlighted if suggested
│  ╚══════════════════╝    │
│  [New Session]  [Done]   │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

### Analytics
```
┌──────────────────────────┐
│ Today          Streak    │
│  2h 15m        12 days   │  large bold numbers
│  5 sessions              │
│──────────────────────────│
│ Categories Today         │
│ ████████ Dev      62%    │  horizontal bars
│ ███░░░░░ Writing  24%    │
│ ██░░░░░░ Design   14%    │
│──────────────────────────│
│ Last 7 Days              │
│  M  T  W  T  F  S  S    │
│  █  █  █  █  █  ▄  ░    │  vertical bar chart
│  2h 3h 1h 4h 2h 1h 0h   │
│──────────────────────────│
│ All Time: 342h 15m       │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

### History (scrollable)
```
┌──────────────────────────┐
│        History           │
│ Today, Mar 18            │
│ ┌────────────────────┐   │
│ │ 10:30 Write arch   │   │
│ │ ■ Dev  25m  full   │   │
│ ├────────────────────┤   │
│ │ 10:00 Short Break  │   │
│ │        5m   rest   │   │
│ ├────────────────────┤   │
│ │ 09:30 Fix bug #42  │   │
│ │ ■ Dev  25m  full   │   │
│ └────────────────────┘   │
│ Yesterday, Mar 17        │
│ ┌────────────────────┐   │
│ │ 16:00 Review PR    │   │
│ │ ■ Dev  15m partial │   │
│ └────────────────────┘   │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

### Settings (scrollable)
```
┌──────────────────────────┐
│        Settings          │
│ Timer                    │
│  Focus Duration   [25m]  │
│  Short Break      [ 5m]  │
│  Long Break       [20m]  │
│  Long Break After [100m] │
│  Auto Start Break  [OFF] │
│  Auto Start Focus  [OFF] │
│──────────────────────────│
│ Notifications            │
│  Enabled           [ON]  │
│──────────────────────────│
│ Google Calendar          │
│  Enabled          [OFF]  │
│  [Sign In with Google]   │
│──────────────────────────│
│ Outlook Calendar         │
│  Enabled          [OFF]  │
│  [Sign In]               │
├──────────────────────────┤
│ [Timer] [Anly] [Hist] [Set]│
└──────────────────────────┘
```

---

## Google Calendar Integration (Android)

**Replaces** Go's localhost:19876 callback approach (not viable on mobile).

**Use AppAuth for Android** (`net.openid:appauth:0.11.1`):
1. Register redirect URI `com.sesh.app:/oauth2redirect` in Google Cloud Console
2. Declare intent filter in manifest for that URI
3. Launch authorization via Custom Tabs / system browser
4. Handle callback, exchange code for tokens
5. Store refresh token in `EncryptedSharedPreferences`
6. AppAuth's `AuthState` handles token refresh automatically

**On session complete:** Enqueue `CalendarSyncWorker` (WorkManager) with session ID. Worker creates Google Calendar event via REST API. Retries with exponential backoff on network failure.

**Event format** (matches Go's `/home/ivan/projects/sesh/internal/calsync/common.go`):
- Summary: `"Focus: {intention}"` or `"Focus Session"`
- Description: category, duration, type, overflow, pause, notes

Same pattern for Outlook using MSAL Android.

---

## Notifications

### Channels
- `sesh_timer` (LOW importance): Persistent foreground notification during timer. Updated every 15s. Shows remaining time + actions.
- `sesh_alerts` (HIGH importance): Fires on focus complete, break complete. Vibration pattern: `[0, 300, 200, 300]`.

### Notification Actions
- During Focus/Overflow: **[Pause]** **[Finish]**
- During Paused: **[Resume]** **[Finish]**
- During Break: **[End Break]**

Actions send intents to `TimerService.onStartCommand()`.

---

## Phased Implementation

### Phase 1: MVP Timer (Weeks 1-2)
- Project setup: Gradle, Hilt, Room, Compose, Navigation
- Room database + entities + default category seeding
- `TimerService` with focus + overflow + pause + abandon state machine
- Timer screen: idle (intention, category, duration adjust, start) + active (countdown, pause, finish, abandon)
- E-ink theme, components: `EinkButton`, `EinkTimerDisplay`, `EinkProgressBar`
- `UserPreferences` + `PreferencesRepository` (focus duration only)
- Bottom nav scaffold (only Timer tab functional)
- **Deliverable:** Working pomodoro timer that persists sessions to SQLite

### Phase 2: Breaks + Session Complete (Week 3)
- Break state machine (short/long/overflow)
- Session complete sheet (notes input)
- Post-session screen (break suggestion, long break logic)
- Auto-start break/focus options
- All timer duration settings

### Phase 3: History + Analytics (Weeks 4-5)
- History screen: scrollable, grouped by day
- Analytics screen: today stats, streak, category breakdown, 7-day chart, all-time
- Port all analytics SQL queries including recursive CTEs
- `EinkBarChart` component

### Phase 4: Settings + Notifications (Week 6)
- Full settings screen
- Notification channels, triggers, and actions
- Vibration support

### Phase 5: Calendar Integration (Weeks 7-8)
- AppAuth + Google OAuth flow
- `GoogleCalendarSync` + `CalendarSyncWorker`
- Outlook via MSAL
- Settings UI for calendar auth

### Phase 6: Polish (Week 9)
- Test on Palma 2 Pro hardware
- Tune refresh intervals for ghosting
- Doze mode verification
- Battery optimization whitelisting
- Crash recovery (restore active session after service restart)
- Accessibility (TalkBack)

---

## Key Source Files to Port From

| Go File | What to Port | Android Target |
|---------|-------------|----------------|
| `internal/db/db.go` | Schema (migration001) | Room entities + `SeshDatabase` |
| `internal/db/sessions.go` | All SQL queries, analytics | Room DAOs, `AnalyticsRepository` |
| `internal/db/categories.go` | Default categories, CRUD | `CategoryDao`, DB callback |
| `internal/state/state.go` | Timer state machine, phases | `TimerPhase`, `TimerState`, `TimerService` |
| `internal/app/app.go` | Session lifecycle, pause logic, calculations | `TimerService`, `TimerViewModel` |
| `internal/config/config.go` | All config fields + defaults | `UserPreferences` data class |
| `internal/calsync/common.go` | Event summary/description format | `GoogleCalendarSync` |
| `internal/calsync/google.go` | Calendar event creation | `GoogleCalendarSync` (REST) |
| `internal/ui/view.go` | Screen layouts, analytics display | Compose screens |

---

## Verification Plan

1. **Unit tests:** TimerState transitions, session duration calculations, analytics queries (in-memory Room DB)
2. **Integration tests:** TimerService lifecycle (start, pause, resume, finish, abandon, undo), Room DAO queries with real data
3. **Manual testing on Palma 2 Pro:**
   - Timer countdown visible and readable from arm's length
   - Progress bar updates without excessive ghosting
   - All buttons respond on first tap (touch targets large enough)
   - Timer survives: screen off, app backgrounded, Doze mode
   - Notification fires on timer completion with vibration
   - Session saved correctly to DB after completion
   - Analytics numbers match expected values
   - Google Calendar event created after session (when enabled)
4. **Battery test:** Run 4-hour focus session, measure battery impact of foreground service + throttled updates

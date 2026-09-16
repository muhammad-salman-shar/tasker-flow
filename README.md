# Tasker Flow 🎮

Real-world task RPG — turn your to-do list into quests with Health, EP, Levels, Streaks, Task Debt and Recovery Quests.

## Concept
Create → Execute → Reward → Recover

## MVP v1
- 4 task types: Deadline / Scheduled / Duration / Flexible
- Recurring: Daily / Weekdays / Weekends / Weekly / Monthly / Custom
- Local notifications with Complete / Snooze actions
- EP + Health + Level + Streak
- Missed → Task Debt → Recovery Quest loop
- Home / Tasks / Stats / Me
- Boot restore hook

## Architecture
Task → Occurrence → Event (Room DB)
PlayerStats (singleton)
GamificationEngine (pure Kotlin)
TaskRepository + MainViewModel (StateFlow)
Compose UI (4 tabs)

## Tech
Kotlin, Compose, Material 3, Room, WorkManager, DataStore
Min SDK 26, Target SDK 34

## Build
APK builds via GitHub Actions on push to main.
Artifact: taskerflow-debug.

## Roadmap
v2: Focus Lock, Achievements, Heatmap, Daily missions, Export, Adaptive suggestions
v3: Cloud sync, AI task breakdown

## Philosophy
Tasker doesn't punish you with meaningless penalties.
It makes you finish what you left unfinished.

# ⚔️ Dungeonbound: The Filipino Labyrinth

> *Explore the maze. Fight the creatures of Philippine folklore. Learn their stories.*

**🎮 Genre:** Dungeon crawler &nbsp;|&nbsp; **💻 Built with:** Java (libGDX) &nbsp;|&nbsp; **👤 Mode:** Single-player, offline  
**📚 Course:** OOP 101 &nbsp;|&nbsp; **👥 Team:** NPC (Team 4, CS-2103)

---

## About

**Dungeonbound** is a single-player Java dungeon crawler built with **libGDX**. Players explore a five-stage dungeon, battle creatures inspired by Philippine folklore, unlock entries in the in-game Bestiary/Wiki, and survive returning reinforcements from earlier stages.

There are plenty of dungeon and fantasy games, but few are built around Filipino culture and mythology. This project aims to be both a fun challenge and a way to introduce Filipino mythical creatures to players.

---

## Who It's For

| Player | What they get |
|---|---|
| **Casual gamers & fantasy fans** | A five-stage dungeon adventure with exploration, real-time combat, animated doors, and escalating enemy encounters. |
| **Students & cultural enthusiasts** | An in-game Bestiary/Wiki and lore pages in English and Tagalog that introduce Filipino mythical creatures. |

---

## Features

**🌐 Language Selection & Accounts**
Choose between **Tagalog** and **English**, and log in or sign up so your Bestiary/Wiki unlocks and settings are saved per user.

**🗺️ Five-Stage Folklore Dungeon**
Explore the tilemap dungeon with wall/floor collision, floor traps, and interactive animated doors:
1. **Tiyanak** — smallest starting chamber
2. **Aswang** — upper hall
3. **Tikbalang** — west middle chamber
4. **Manananggal** — east middle chamber
5. **Kapre** — large lower chamber

**👹 Folklore Enemies & Reinforcements**
Each creature has its own walk, attack, and death animations, plus an alert warning window (`!`) before chasing and attacking. Defeated enemies in earlier stages return as hostile reinforcements in later chambers.

**📖 Game Wiki (Bestiary)**
Defeating a creature unlocks its card in the Bestiary so you can read its lore and stats.

---

## How the Game Flows

```
Language Select / Title Screen
      │
      ▼
Start Menu (Play / Wiki / Settings / Exit)
      │
      ▼
Loading Screen ──► Enter Dungeon (Stage 1: Tiyanak)
      │
      ▼
Battle Enemies ──► Unlock Wiki Entries ──► Open Doors (E)
      │
      ▼
Clear Stage ──► Next Chamber (+ returning reinforcements)
      │
      ▼
All 5 Stages Cleared?
   │              │
  lose           win
   │              │
   ▼              ▼
Game Over (R)   Dungeon Cleared (R)
```

---

## 🚀 How to Run the Game

### System Requirements
- **Java Development Kit (JDK):** Version 17 or higher (Java 21/26 supported)
- **Operating System:** Windows, macOS, or Linux

### Quick Start
Clone the repository and run the desktop application using the included Gradle wrapper:

```bash
# 1. Clone the repository
git clone https://github.com/neonekiii/OOP101-FinalProject-CS2103-Team04.git
cd OOP101-FinalProject-CS2103-Team04

# 2. Run the game
# On Windows (PowerShell / Command Prompt):
.\gradlew.bat :lwjgl3:run

# On macOS / Linux:
./gradlew :lwjgl3:run
```

### Controls (Mga Kontrol sa Laro)

| Action | Keyboard | Mouse |
|---|---|---|
| **Movement (Galaw)** | `W`, `A`, `S`, `D` or `Arrow Keys` | — |
| **Attack (Atake)** | `SPACE` (Sword swing) | — |
| **Open Door (Buksan ang Pinto)** | `E` | — |
| **Zoom In / Out** | `+` / `-` | Mouse Wheel |
| **Pause / Menu** | `ESC` | In-game buttons |
| **Restart (after Win / Game Over)** | `R` | — |
| **Fullscreen Toggle** | `F11` | Settings menu |

---

## Project Structure

```
OOP101-FinalProject-CS2103-Team04/
├── assets/          # Game textures, tilemap, audio, fonts, UI, wiki, player & enemy atlases
├── core/            # libGDX shared game logic, screens, UI, and audio management
├── lwjgl3/          # Desktop launcher and packaging/atlas Gradle tasks
├── gradle/          # Gradle wrapper files
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

---

## OOP Concepts

| Concept | Where it applies |
|---|---|
| **Classes & Objects** | `DungeonBound`, `GameScreen`, `WikiCreature`, `AudioManager`, `Ui`, and runtime `Enemy`/`Door`/`Tile` instances. |
| **Encapsulation** | Player HP, enemy stats, cooldowns, preferences, and wiki unlock progress (`WikiProgress`) are managed through dedicated methods. |
| **Inheritance & Polymorphism** | Screens implement libGDX `Screen` / extend `ScreenAdapter` (`TitleScreen`, `StartMenuScreen`, `LoadingScreen`, `GameScreen`, `PauseMenuScreen`, `WikiMenuScreen`), each overriding `render()`, `resize()`, and `dispose()`. |
| **Abstraction** | `AudioManager`, `Ui`, and `AssetManager` hide low-level resource loading and rendering details behind clean helpers. |
| **Exception Handling** | Handles missing/invalid resources, preferences parsing, and crash logging during screen transitions. |
| **Collections** | `List`, `Map`, and `Set` store map layers, tiles, doors, active enemies, reinforcements, and cached animations. |

---

## Team NPC

| Member | Role |
|---|---|
| **Charmaine T. Argona** ([@eniaxen](https://github.com/eniaxen)) | Character Designer, Repository Manager, Programmer, Documentor |
| **Dhymple Norshe F. Carandang** ([@ajax1114](https://github.com/ajax1114)) | Enemy Designer, 2D Background Artist, Programmer, Documentor |
| **Phoebe Leonides M. Dolor** ([@neonekiii](https://github.com/neonekiii)) | GUI & Item Designer, Programmer, Documentor |

---

## 🛡️ Intellectual Property & Assets Notice

All original pixel art, character animations, folklore monster cards, UI layouts, and audio design in this repository were created specifically for **DungeonBound** (OOP101 Final Project, CS2103 Team 04).
- **GUI, Item Designs, Wiki Cards & Sound Effects:** Created and owned by **Phoebe Leonides M. Dolor** ([@neonekiii](https://github.com/neonekiii)).
- **Character Designs:** Created and owned by **Charmaine T. Argona** ([@eniaxen](https://github.com/eniaxen)).
- **Enemy Designs & 2D Backgrounds:** Created and owned by **Dhymple Norshe F. Carandang** ([@ajax1114](https://github.com/ajax1114)).

> **Notice:** All rights reserved. Unauthorized reproduction, extraction, redistribution, or reuse of these creative assets in other academic submissions or third-party projects without explicit permission from the authors is strictly prohibited.

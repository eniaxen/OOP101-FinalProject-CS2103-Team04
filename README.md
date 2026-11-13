# ⚔️ Dungeonbound: The Filipino Labyrinth

> *Explore the maze. Fight the creatures of Philippine folklore. Learn their stories.*

**🎮 Genre:** Dungeon crawler &nbsp;|&nbsp; **💻 Built with:** Java (GUI) &nbsp;|&nbsp; **👤 Mode:** Single-player, offline  
**📚 Course:** OOP 101 &nbsp;|&nbsp; **👥 Team:** NPC (Team 4, CS-2103)

---

## About

Dungeonbound is a single-player Java game with a graphical interface. Players explore randomly generated labyrinths, defeat enemies inspired by Philippine folklore, and earn coins to upgrade their character before facing a final boss.

There are plenty of dungeon and fantasy games, but few are built around Filipino culture and mythology. This project aims to be both a fun challenge and a way to introduce Filipino mythical creatures to players.

---

## Who It's For

| Player | What they get |
|---|---|
| **Casual gamers & fantasy fans** | A dungeon adventure with exploration, combat, and resource management leading to a final boss. |
| **Students & cultural enthusiasts** | An in-game wiki, story, and themed items that teach Filipino mythology and folklore. |

---

## Features

**🔐 Accounts**
Register and log in so your progress is saved.

**🗺️ Labyrinth Exploration**
Move through generated labyrinths with wall collision.

**👹 Folklore Enemies**
Each enemy has its own stats and skills, and is placed randomly in the maze.

**🪙 Coins & Shop**
Collect coins and loot, then spend them on HP, weapon damage, and skills.

**📖 Game Wiki**
Defeat a creature to unlock its entry and read about its place in Philippine folklore.

**☠️ Final Boss**
Conquer every labyrinth to face the boss. Lose and you can retry or end the game.

---

## How the Game Flows

```
Login / Register
      │
      ▼
Enter the dungeon ──► Explore the labyrinth
      │
      ▼
Battle enemies ──► Unlock wiki entries ──► Collect coins
      │
      ▼
Upgrade at the shop
      │
      ▼
All labyrinths conquered?
   │              │
   no             yes
   │              │
   ▼              ▼
Next labyrinth    Final boss
                  │         │
                 lose      win
                  │         │
          Retry / Game Over  Dungeon conquered
```

---

## OOP Concepts

| Concept | Where it applies |
|---|---|
| **Classes & Objects** | `Player`, `Enemy`, `Labyrinth`, `Shop`, `Account`. Every enemy in the maze is its own object. |
| **Encapsulation** | HP, coins, level, weapon, skills, and progress are private and changed only through methods (e.g. spending coins on an upgrade). |
| **Inheritance** | `Character` is the parent of `Player` and `Enemy`. Each folklore enemy extends `Enemy`. |
| **Polymorphism** | `attack()` and `useSkill()` act differently per enemy type: fast but weak, or slow but heavy-hitting. |
| **Abstraction** | The player picks "Upgrade Weapon"; the stat calculations stay hidden in the background. |
| **Exception Handling** | Covers invalid logins, unavailable menu options, and spending more coins than owned. |
| **Arrays / Lists** | Store the labyrinth layout, enemies, inventory, and wiki entries. |

---

## Team NPC

| Member | Role |
|---|---|
| **Charmaine T. Argona** ([@eniaxen](https://github.com/eniaxen)) | Character Designer, Repository Manager, Programmer, Documentor |
| **Dhymple Norshe F. Carandang** ([@ajax1114](https://github.com/ajax1114)) | Enemy Designer, 2D Background Artist, Programmer, Documentor |
| **Phoebe Leonides M. Dolor** ([@neonekiii](https://github.com/neonekiii)) | GUI & Item Designer, Programmer, Documentor |

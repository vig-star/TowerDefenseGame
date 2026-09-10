# How the game works

**Save Judy** runs entirely inside a JavaFX desktop application. Each playthrough creates an in-memory `Game`, and FXML controllers coordinate the player's actions and enemy movement.

## From launch to gameplay

1. [`TowerDefenseApplication.start`](../src/main/java/com/example/judy/TowerDefenseApplication.java) loads `welcome-screen.fxml` with `FXMLLoader`, creates a `Scene`, and displays it in the primary `Stage`.
2. `WelcomeScreenController` handles **START** and replaces the scene with `initial-config-screen.fxml`.
3. `InitialConfigScreenController` accepts a name and difficulty, constructs a `Player` and `Game`, and stores the game through `GameAdmin.setGame`.
4. `InitialGameScreenController` loads the active game, creates the board and controls, and opens the Store and Inventory as separate modal windows.
5. Combat eventually loads the win or game-over screen. Its controller reads the final statistics from `GameAdmin`; **RESTART** clears that reference and returns to the welcome screen.

FXML files live in [`src/main/resources/com/example/judy/screens/`](../src/main/resources/com/example/judy/screens/). Their `fx:controller` attributes select the Java controller classes, `fx:id` values connect controls to `@FXML` fields, and `onAction` attributes connect buttons to handlers. The Java module opens the controller package to `javafx.fxml` so the loader can access those fields and handlers.

## State and class relationships

| Class or family | Role in a playthrough |
| --- | --- |
| [`GameAdmin`](../src/main/java/com/example/judy/modules/GameAdmin.java) | Static access point for the current `Game`; controllers use it to share state |
| [`Game`](../src/main/java/com/example/judy/modules/Game.java) | Owns difficulty, level, player, monument, enemies, inventory counts, placed towers, and damage totals |
| [`Player`](../src/main/java/com/example/judy/modules/Player.java), [`Monument`](../src/main/java/com/example/judy/modules/Monument.java) | Hold the player's name, money, score, and the monument's current and maximum health |
| [`Tower`](../src/main/java/com/example/judy/modules/Tower.java), [`Enemy`](../src/main/java/com/example/judy/modules/Enemy.java) | Abstract base classes for `Cannon` / `Crossbow` / `Tank` and `BasicEnemy` / `StrongEnemy` / `BossEnemy` |
| [`Tile`](../src/main/java/com/example/judy/modules/Tile.java) | Associates a grid position with a JavaFX button, occupancy, path status, and placement rules |

Inheritance provides shared fields and methods. The controller distinguishes concrete tower and enemy types with `instanceof` checks when applying attack rules and rewards. This makes the class-project use of inheritance visible, while leaving much of the behavior in the controller.

## Board, purchases, and upgrades

The board is a **5-row × 9-column** `GridPane`, with **140-pixel** tiles. `InitialGameScreenController.initialize` constructs the path, blocked spaces, and buildable tiles. The route is fixed in code: enemies enter from the left on row 3, turn upward at column 2, cross row 1, and turn downward at column 6 toward the monument. Coordinates are zero-based.

Purchasing in `TowerMenuController` checks the player's balance, subtracts the cost, and increments an inventory count in `Game`. Selecting a tower in `InventoryMenuController` creates the concrete tower and sets the pending placement in `InitialGameScreenController`. Clicking a valid empty tile gives that tower a position and image, adds it to the placed-tower list, and reduces the inventory count.

Clicking an occupied tower tile offers an upgrade. The cost is 60% of that tower type's current purchase price, and an accepted upgrade adds 5 to the instance's damage. The tower type's price is static and gets set when `Game` initializes the selected difficulty.

## Difficulty settings

These values come from the `Game` constructor, including the nonuniform prices on Hard.

| Difficulty | Starting money | Monument HP | Cannon / Crossbow / Tank | Basic / Strong / Boss HP |
| --- | ---: | ---: | --- | --- |
| Easy | $200 | 150 | $50 / $75 / $100 | 100 / 125 / 200 |
| Medium | $150 | 125 | $60 / $75 / $110 | 125 / 150 / 250 |
| Hard | $100 | 100 | $40 / $60 / $100 | 150 / 175 / 300 |

Enemy movement waits are 2,500 ms on Easy, 2,300 ms on Medium, and 2,100 ms on Hard. In this implementation, the `Enemy.speed` field stores a delay in milliseconds: a smaller value means faster movement. Difficulty also sets enemy damage and changes tower damage and score multipliers.

## Combat and the JavaFX thread

`InitialGameScreenController.onStartCombat` starts the basic and strong enemies for level 1. Once both are defeated, another click starts the boss for level 2. These are two scripted encounters, not an unlimited wave generator.

Background JavaFX `Task`s advance enemies along the route. They queue board changes and attacks with `Platform.runLater`, which executes work on the JavaFX application thread. A separate polling task refreshes the money, score, level, and health labels every 100 ms.

At an enemy's path position, `towerDamage` checks each placed tower:

| Tower | Range test | Initial internal damage |
| --- | --- | ---: |
| Cannon | Same row **or** column | 5 |
| Crossbow | Same row **or** column | 8 |
| Tank | Absolute row difference ≤ 2 **and** column difference ≤ 2 | 12 |

Damage per applicable attack is `(5 - difficulty) × tower.getDamage()`, where difficulty is 0, 1, or 2. The field named `DAMAGE_PER_SECOND` supplies the initial damage value, but attacks follow movement/damage updates rather than a one-second firing timer. The store's descriptive damage and DPS strings are therefore not authoritative combat statistics.

Defeated enemies award money and score. Enemies that reach the monument's damage zone continue attacking while both they and the monument have health. Defeating the boss loads the win screen; monument health at or below zero loads the game-over screen.

## Tests and coursework evidence

The seven classes in [`src/test/`](../src/test/) contain **46 JUnit 4/TestFX tests**. They launch the actual application, look up JavaFX controls by ID or text, perform clicks, and check labels or game state. They cover welcome/setup screens, difficulty values, purchases, placement, combat, upgrades, results, and replay. The Maven build explicitly recognizes the original `src/test` location.

GUI execution is enabled with `-Pgui-tests`. The selected CI smoke tests cover welcome, configuration, initial board, and store behavior. The full suite includes long fixed sleeps and exercises combat and replay; it should be assessed separately from a successful compile or smoke run.

The [milestone testing deliverables](../src/main/resources/com/example/judy/assets/testing-deliverables/) explain the original M4–M6 checks. The course identity is grounded in `run_checkstyle.py`, which identifies its authors as the CS 2340 TAs and references Georgia Tech's CS 2340 code-style repository. Team development and the 2021 timeline are preserved in Git history.

## Implementation boundaries

This repository preserves the student game's architecture. The GitHub preparation improves documentation and build reproducibility; it does not redesign the engine.

- **Controller-heavy design:** `InitialGameScreenController` owns board construction, UI updates, movement, combat, and scene transitions. `Tile` and `Tower` hold JavaFX controls/images, so the model is coupled to the UI.
- **Shared mutable state:** `GameAdmin`, pending tower placement, and tower prices are static. They need care during replay and test isolation.
- **Task lifecycle:** background polling and combat tasks are daemon threads without comprehensive cancellation on scene changes. Opening and closing menus can initialize extra game controllers and polling tasks.
- **UI timing:** some handlers sleep on the JavaFX thread, and some background code accesses controls directly. Brief pauses and timing-sensitive tests are possible.
- **Fixed scope:** one board, two combat levels, in-memory state, and a fixed window layout. Store descriptions also need reconciliation with the combat values before they can serve as a balance reference.

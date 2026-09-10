# Design patterns and principles

Save Judy demonstrates object-oriented modeling and event-driven JavaFX programming. Its strongest examples are the separation of screens, controllers, and game data; reusable base classes; and callbacks that connect user actions to behavior. The project also shows the tradeoffs of shared mutable state and a large gameplay controller.

This guide describes the implementation that exists. Possible refactorings below are learning opportunities, not features already implemented.

## MVC-inspired screen organization

**Model–View–Controller (MVC)** separates application data, its presentation, and the handling of user actions. Save Judy follows that broad organization:

| Part | Project example | Responsibility |
| --- | --- | --- |
| Model | [`Game`, `Player`, `Monument`, `Tower`, and `Enemy`](../src/main/java/com/example/judy/modules/) | Represent a playthrough and its entities |
| View | [`initial-game-screen.fxml`](../src/main/resources/com/example/judy/screens/initial-game-screen.fxml) and other FXML files | Declare layouts, controls, IDs, and event connections |
| Controller | [`InitialConfigScreenController`](../src/main/java/com/example/judy/controllers/InitialConfigScreenController.java) and other controllers | React to input, update state, and navigate screens |

For example, the configuration view displays the name field and difficulty buttons. Its controller's `setGameConfigurations` method constructs the `Player` and `Game`, then shares that game through `GameAdmin`. The view describes the controls while Java implements their behavior.

The benefit is navigable code: a layout change starts in FXML, and a click-handling change starts in a controller. The separation is incomplete. `InitialGameScreenController` also implements movement, attack rules, and scoring; `Tower` stores an `ImageView`, and `Tile` stores a `Button`. It is accurate to call this **MVC-inspired**, rather than claim a fully independent model layer.

## Event-driven callbacks and framework control

JavaFX dispatches events to registered handlers. The board's FXML wires its Start button to the controller:

```xml
<Button fx:id="startCombat" onAction="#onStartCombat" text="Start" />
```

The full declaration is in [`initial-game-screen.fxml`](../src/main/resources/com/example/judy/screens/initial-game-screen.fxml). During board construction, [`InitialGameScreenController.initialize`](../src/main/java/com/example/judy/controllers/InitialGameScreenController.java) also registers handlers with `tile.getButton().setOnAction(...)`; a click then calls a placement or rejection method.

This is **Observer-style event handling**: controls produce events and registered callbacks respond. JavaFX supplies the event machinery. The repository does not implement its own general-purpose Observer pattern, and its game models do not publish property-change notifications. `updateGameData` instead polls the game every 100 ms and queues label updates with `Platform.runLater`.

There is also **inversion of control** at the framework level. JavaFX calls `TowerDefenseApplication.start`, `FXMLLoader` creates controllers and injects `@FXML` fields, and the framework invokes event callbacks. The application supplies behavior for those lifecycle hooks. That is distinct from injecting game services: controllers still obtain their game through a static call.

## A shared state holder, with a Singleton-like role

[`GameAdmin`](../src/main/java/com/example/judy/modules/GameAdmin.java) contains a static `Game` reference and static `getGame` / `setGame` methods. Configuration stores the active playthrough there, and controllers read the same reference.

This gives the project a convenient **single access point for current game state**. For a small application with one active playthrough, it avoids passing a `Game` through every screen transition.

It is not a conventional **Singleton** implementation: `GameAdmin` has no private constructor or `getInstance` method, and it does not enforce that only one `Game` can be constructed. It simply holds one current reference, which can be replaced or cleared.

The tradeoff is hidden coupling. A controller's behavior depends on global state being initialized correctly; tests and replay must reset it. Passing the current game to controllers explicitly would make those dependencies easier to see, but that is a potential refactoring rather than the existing design.

## Object-oriented principles in the entities

| Principle | Concrete example | Benefit and boundary |
| --- | --- | --- |
| Abstraction | [`Tower`](../src/main/java/com/example/judy/modules/Tower.java) and [`Enemy`](../src/main/java/com/example/judy/modules/Enemy.java) are abstract base classes | Provide shared concepts and state, though neither defines an abstract attack/movement strategy |
| Inheritance | `Cannon`, `Crossbow`, and `Tank` extend `Tower`; three enemy types extend `Enemy` | Reuse position, damage, image, health, and timing accessors instead of declaring them in every subtype |
| Polymorphism | `Game` stores `ArrayList<Tower>` and `ArrayList<Enemy>`; subclasses override `toString` | Different concrete types share a base type, but combat still uses `instanceof` branches rather than an overridden attack method |
| Encapsulation | `Player` and `Monument` keep fields private; `Monument.maxHealth` is final | Organizes access to state, although broad setters and directly returned mutable collections limit invariant protection |
| Object composition | `Game` holds a `Player`, a `Monument`, and collections of towers and enemies | Builds a playthrough from collaborating objects rather than putting all state in the application entry point |

An important Java detail is that tower prices are accessed through static methods such as `Cannon.getCost()`. Static methods are hidden by subclasses, not dynamically dispatched like instance methods. The common `Tower` type therefore does not provide a polymorphic pricing interface.

## How the design relates to SOLID

SOLID is useful here as a way to evaluate the design, not as a checklist the project fully satisfies.

| Principle | What the code demonstrates |
| --- | --- |
| **Single Responsibility** | `Player` and `Monument` have focused data roles. `InitialGameScreenController` has many reasons to change because it builds the board, handles input, runs combat, and navigates results. |
| **Open/Closed** | New types can extend `Tower` or `Enemy`, but extending gameplay also requires changing type checks, menus, inventory creation, and difficulty setup. Behavior is not fully open to extension without modifying existing code. |
| **Liskov Substitution** | Concrete types can be stored under their base types and use the shared accessors. Inheritance alone does not establish this principle; gameplay's concrete-type checks mean the project does not demonstrate interchangeable attack behaviors through a base contract. |
| **Interface Segregation** | The application uses JavaFX's callback interfaces, but defines no custom service interfaces whose responsibilities demonstrate this principle. |
| **Dependency Inversion** | Controllers depend directly on concrete game classes, `GameAdmin`, and JavaFX controls. There is no abstraction boundary separating a combat service from its UI or storage of the active game. |

The related **Don't Repeat Yourself (DRY)** principle is partly served by the shared entity base classes. Repeated screen-loading code, purchase handlers, and damage/reward branches remain opportunities to extract common behavior.

## Patterns that could improve a future version

The current code does not implement Strategy, Factory Method, or a custom Observer system. Three focused follow-on changes would make useful learning exercises:

1. Extract a `CombatEngine` so movement, damage, and rewards can be tested independently of a JavaFX scene. This strengthens separation of concerns and Single Responsibility.
2. Give towers an attack-range operation, or compose them with a range **Strategy**. This can replace concrete-type checks and make new tower behaviors easier to add.
3. Inject a session object into controllers and expose observable view state. This reduces reliance on global state and can replace label polling with explicit updates or JavaFX property bindings.

## Explaining the class project

> Save Judy is a JavaFX team project that applies object-oriented modeling and event-driven UI programming. We separated FXML layouts, screen controllers, and game-state classes, and used abstract tower and enemy classes to share behavior and state. JavaFX callbacks connect user actions to the controllers. The design is MVC-inspired, with a static holder for the active game. Its main tradeoffs are controller-heavy combat, shared mutable state, and concrete-type checks; extracting the combat logic would be a natural next step.

For the exact game flow and combat rules, see [How the game works](ARCHITECTURE.md).

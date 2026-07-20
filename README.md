# Batalla Naval — Mini Proyecto #4

**Curso:** 750014C Fundamentos de Programación Orientada a Eventos

Implementación en Java 17+ / JavaFX del clásico juego de Batalla Naval (Battleship), jugado entre un jugador humano y una máquina (IA), con colocación de flota, disparos, vista de verificación del tablero enemigo, guardado automático y reanudación de partida.

---

## 👥 Integrantes

| Nombre | Código |
|---|---|
| Joan Lorenzo Hinestroza Cantillo | 2516995 |
| Natalia Andrea Parra Peña | 2516845 |
| Juan Manuel Rosero | 2520822 |
| Juan Sebastián Duarte Quintero | 2516473 |

---

## 🎮 Descripción del juego

Cada jugador (humano y máquina) cuenta con un tablero de 10x10 y debe distribuir su flota de 10 barcos:

| Barco | Tamaño | Cantidad |
|---|---|---|
| Portaaviones | 4 casillas | 1 |
| Submarino | 3 casillas | 2 |
| Destructor | 2 casillas | 3 |
| Fragata | 1 casilla | 4 |

Una vez ubicada la flota, los jugadores se turnan para disparar sobre el tablero contrario. Un disparo puede resultar en **agua**, **tocado** o **hundido**; solo el resultado "agua" cede el turno al oponente. Gana quien hunda primero toda la flota enemiga.

---

## 📋 Historias de Usuario (HU)

| HU | Descripción | Responsables principales |
|---|---|---|
| **HU-1** | Colocación de barcos (horizontal/vertical), validación de solapamiento y límites del tablero, bloqueo tras confirmar la flota | `ShipPlacementController`, `Board.placeShip()`, `BoardValidator`, `InvalidShipPlacementException`, `FleetAlreadyPlacedException` |
| **HU-2** | Disparos sobre el tablero principal, resultado agua/tocado/hundido, manejo de turnos, no repetir celda | `GameBoardController`, `GameEngine.fireAsHuman()`, `Board.receiveShot()`, `TurnManager`, `CellAlreadyShotException` |
| **HU-3** | Vista de verificación del tablero de la máquina, sin niebla de guerra, en ventana separada | `OpponentBoardViewController`, `BoardView(machineBoard, VERIFICATION_VIEW)` |
| **HU-4** | IA de la máquina: coloca su flota al azar, dispara al azar/con estrategia de caza, respeta las reglas | `RandomFleetPlacementStrategy`, `RandomShotStrategy`, `HuntTargetShotStrategy`, `MachineTurnTask` |
| **HU-5** | Guardado automático tras cada jugada (`.ser` + `.txt`), opción "Continuar" que carga la partida más reciente | `SaveGameManager`, `SerializedGameRepository`, `PlainTextPlayerStatsRepository`, `MainMenuController` |

---

## 🏗️ Arquitectura

El proyecto sigue **MVC** con paquetes separados por responsabilidad (`model`, `view`, `controller`, `persistence`, `concurrency`, `util`), sin que el modelo tenga ninguna dependencia de JavaFX.

### Decisión de diseño clave: un solo `Board`

En lugar de modelar el "tablero de posición" y el "tablero principal" como clases distintas (lo que violaría SRP/DRY), se usa una única clase `Board` (10x10, con su flota y sus disparos) y un enum `BoardRenderMode` que determina cómo se renderiza:

- `OWNER_VIEW` → tablero propio del jugador humano (HU-1), sin listeners de disparo.
- `FOG_VIEW` → tablero principal, con niebla de guerra y listeners de disparo (HU-2/HU-4).
- `VERIFICATION_VIEW` → vista de verificación, solo lectura, revela todo (HU-3).

### Estructura de paquetes (resumen)

```
src/main/java/com/example/navalbattle/
├── Main.java
├── model/
│   ├── board/        (Board, Cell, CellState, Coordinate)
│   ├── ship/          (Ship y subclases, ShipType, ShipFactory, Orientation)
│   ├── player/        (Player, HumanPlayer, MachinePlayer, PlayerStats)
│   ├── shot/           (Shot, ShotResult, strategy/ShotStrategy...)
│   ├── placement/     (FleetPlacementStrategy, RandomFleetPlacementStrategy)
│   ├── game/            (GameEngine, GameState, TurnManager, events/*)
│   └── exceptions/    (excepciones propias, checked/unchecked)
├── persistence/       (GameRepository, PlayerStatsRepository, SaveGameManager...)
├── concurrency/        (MachineTurnTask, AutoSaveTask, GameClockThread)
├── controller/         (MainMenuController, ShipPlacementController,
│                        GameBoardController, OpponentBoardViewController)
├── view/                (BoardView, BoardRenderMode, shapes/CellShapeFactory)
└── util/                (AppConfig, BoardValidator)
```
---

## 🚀 Requisitos y ejecución

- **Java 17+**
- **Maven**
- **JavaFX** (gestionado vía Maven)

```bash
# Compilar
mvn clean compile

# Ejecutar
mvn javafx:run

# Ejecutar pruebas
mvn test

# Generar Javadoc (HTML)
mvn javadoc:javadoc
```

---

## 🧪 Pruebas unitarias (JUnit)

| Clase de prueba | Qué valida |
|---|---|
| `ShipTest` | Tamaño por tipo, registro de impactos, detección de hundimiento |
| `BoardTest` | Rechazo de solapamiento/fuera de rango, no repetir disparo, flota hundida |
| `GameEngineTest` | Cambio de turno, condición de victoria |
| `RandomShotStrategyTest` | Nunca repite coordenada, siempre dentro del tablero |
| `SaveGameManagerTest` | Guardar/cargar reproduce el mismo estado (round-trip) |

---

## 🌳 Flujo de trabajo con Git

- `main`: solo releases etiquetados.
- `develop`: rama de integración.
- `feature/HU-1-ship-placement`, `feature/HU-2-shooting`, `feature/HU-3-opponent-view`, `feature/HU-4-machine-ai`, `feature/HU-5-autosave`: ramas de trabajo por historia de usuario.
- Pull Requests hacia `develop`; tag de entrega (p. ej. `v1.0.0`) sobre `main`.

---

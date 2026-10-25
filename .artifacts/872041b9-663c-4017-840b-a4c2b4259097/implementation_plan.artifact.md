# Meleoras del Tablero y Fichas de Dominó Conquista

Mejorar la experiencia visual y jugabilidad del juego de dominó ampliando el tablero, embelleciendo los territorios y casillas especiales, y dando un aspecto más profesional y estilizado a las fichas.

## User Review Required

> [!IMPORTANT]
> - **Ampliación del Tablero**: Se ampliará de **8x8 (64 casillas)** a **10x10 (100 casillas)** para ofrecer mayor espacio estratégico y permitir más combinaciones de juego con las 28 fichas.
> - **Casillas Especiales y Territorios**: Se rediseñarán con mejores colores, iconos distintivos y bordes de propiedad más limpios.
> - **Fichas de Dominó**: Se mejorará el diseño de las fichas (`TileView`) con texturas de marfil/hueso (`#FFFDD0`), líneas divisorias estilizadas y tipografía de alto contraste.

## Proposed Changes

### Model & Logic
#### [MODIFY] [RoomRepository.kt](file:///C:/Users/Familiaa/StudioProjects/Domino/app/src/main/java/com/example/domino_game/model/RoomRepository.kt)
- Ampliar la generación inicial del tablero de 8x8 a 10x10.

#### [MODIFY] [GameRepository.kt](file:///C:/Users/Familiaa/StudioProjects/Domino/app/src/main/java/com/example/domino_game/model/GameRepository.kt)
- Ampliar la recreación del tablero en `restart()` de 8x8 a 10x10.

#### [MODIFY] [MoveValidator.kt](file:///C:/Users/Familiaa/StudioProjects/Domino/app/src/main/java/com/example/domino_game/model/MoveValidator.kt)
- Actualizar límites de validación de movimientos y vecinos adyacentes de 0..7 a 0..9.

### UI & Views
#### [MODIFY] [GridBoardView.kt](file:///C:/Users/Familiaa/StudioProjects/Domino/app/src/main/java/com/example/domino_game/view/components/GridBoardView.kt)
- Renderizar una cuadrícula 10x10.
- Mejorar `TerritoryCellView` con mejores colores de fondo, bordes para territorios conquistados, y distintivos visuales claros para casillas especiales (`BONUS`, `TREASURE`, `TRAP`, `FORTRESS`, `PORTAL`).

#### [MODIFY] [TileView.kt](file:///C:/Users/Familiaa/StudioProjects/Domino/app/src/main/java/com/example/domino_game/view/components/TileView.kt)
- Rediseñar las fichas con aspecto de hueso/marfil premium, bordes definidos, sombra/elevación visual y números nítidos.

## Verification Plan

### Automated Tests
- Ejecutar compilación del proyecto (`gradle_build("app:compileDebugSources")`) para verificar que no existan errores de compilación.
- Ejecutar tests existentes (`gradle_build("app:testDebugUnitTest")`).

### Manual Verification
- Desplegar o probar la app en el emulador/dispositivo para inspeccionar el nuevo tablero 10x10 y el rediseño de casillas y fichas.

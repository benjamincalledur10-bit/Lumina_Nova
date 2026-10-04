# Validación de v0.0.2-alpha

Esta versión añade la pestaña General y la integración opcional con Mod Menu para Minecraft 26.3.
La candidata del frustum de la versión anterior se conserva. Ultra Optimization es una preferencia
persistente de vista previa, sin cambios de motor ni beneficio de FPS en esta alpha.

## Alcance implementado

- Acceso mediante Opciones → Ajustes de vídeo y el botón de configuración de Mod Menu.
- Registro visible en Mod Menu, con nombre, descripción traducida y logo propio.
- Renderizado 2–50; simulación 5–32; brillo; pantalla completa y exclusiva; resolución;
  VSync; FPS máximos; visibilidad de menú/Dock en macOS.
- Controles del juego, guardado en `options.txt`, cierre con Hecho/Escape y navegación al padre.
- Preferencia Ultra en `luminanova.properties`, con aviso visible y tooltip de vista previa.
- Textos propios en inglés, español de España y español de México; las etiquetas del juego
  utilizan sus traducciones nativas.

## Verificación local

Entorno: Apple M4, macOS 27.0.1, Temurin 25.0.4.1, Minecraft 26.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3, Loom 1.17.21, Gradle 9.6.0. Integración opcional: Mod Menu 21.0.0.

`runUiSmoke` abre el cliente real, espera la carga de recursos y realiza estas comprobaciones:

- Rango de los `OptionInstance` transformados: 2–50 y 5–32.
- Clamp transformado de `ChunkMap`: 50; instancia del tracker de tickets de `DistanceManager`: radio 50.
- Sustitución de la pantalla vanilla exacta, conservando la pantalla de retorno.
- Presencia de los widgets de renderizado, simulación, brillo, pantalla completa, VSync y FPS.
- Acciones sobre sliders y el interruptor Ultra, aplicación de valores pendientes al cerrar.
- Guardado de 50/32 chunks, recarga de `options.txt` y reapertura de Ultra con su estado guardado.
- Sin Mod Menu: arranque correcto, pantalla funcional y capturas en escalas GUI 2 y 3.
- Con Mod Menu: entrada en `ROOT_MODS`, factory de configuración registrado, pantalla obtenida
  mediante `ModMenu.getConfigScreen`, búsqueda de Lumina Nova en la lista y captura de su icono.

Las capturas se crean con el framebuffer de Minecraft en `run-ui-smoke/screenshots/`.
Se revisaron el logo, las etiquetas, la pestaña, el scroll y los avisos; se corrigió un valor
que duplicaba el nombre de Ultra y se ampliaron controles con etiquetas largas.
El mod auxiliar es exclusivo del entorno de pruebas y no se incluye en el JAR.

Las pruebas JUnit cubren además la conservación de otras propiedades al guardar Ultra,
la lectura de su estado y el rechazo de archivos inválidos sin sobrescribirlos, junto con las
pruebas diferenciales del frustum existentes. `runFrustumSmoke` verifica la ruta activada
bajo Fabric después de los cambios de interfaz.

## Límites de la validación

No se midieron FPS de esta versión ni se implementó Ultra. Ampliar el radio a 50 permite
que el servidor integrado acepte y siga ese valor; las pruebas del controlador no sustituyen
una sesión prolongada de carga y generación de terreno a esa distancia. En multijugador
se respetan las distancias del servidor remoto. No se validaron mundos de larga duración,
shaders u otros mods que sustituyan la pantalla de vídeo o modifiquen los puntos de inyección.

El selector de resolución depende de los modos que exponga el monitor y utiliza las rutas
nativas del juego. Las capturas locales no validan todos los monitores, sistemas operativos
ni transiciones de pantalla completa. El rendimiento del juego sigue sujeto al
[protocolo de benchmarks](BENCHMARKS.md).

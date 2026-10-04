# Validación de v0.0.3-alpha

Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.
Mod Menu 21.0.0 es opcional. Solo se publica el JAR instalable del mod.

## Cambios

Pantalla General propia con barra lateral, logo, fondo oscuro translúcido, etiquetas a la
izquierda y valores a la derecha. Los sliders aparecen al pasar el ratón o enfocar una fila.
Aplicar activa y guarda; Aceptar aplica y vuelve; Escape descarta cambios pendientes.
Se conserva la optimización experimental del frustum de las alphas anteriores.
Ultra Optimization guarda una preferencia de vista previa y no activa optimizaciones.

## Comprobaciones

La prueba `runUiSmoke` usa los widgets del cliente real y captura su framebuffer. Comprueba:

- Apertura desde la pantalla vanilla de vídeo.
- Cambios pendientes separados de los valores activos.
- Aplicación de renderizado 50, simulación 32, brillo 0.6 y VSync apagado/encendido.
- Cambio de FPS máximos de 250 a 120: tanto la opción como el campo del limitador real
  `FramerateLimitTracker` quedan en 120; el límite efectivo es menor o igual a 120.
- Callbacks de pantalla completa solicitan entrar/salir; actualización del estado de la ventana.
- Guardado y recarga de `options.txt`, persistencia de Ultra y retorno a la pantalla padre.
- Escape descarta los cambios pendientes.
- Diseño a escalas de interfaz 2 y 3 y entrada opcional desde Mod Menu.

Las siete pruebas JUnit cubren configuración y equivalencia del frustum. La prueba de arranque
`runFrustumSmoke` compara 300 000 cajas contra la implementación original transformada.

## Límites de la evidencia

El límite de FPS usa la temporización de Minecraft: esta comprobación valida el limitador activo,
no una medición prolongada de FPS en mundos. Las distancias efectivas en servidores remotos
siguen dependiendo del servidor. Las sesiones largas a 50 chunks no se han validado.
No se afirma una mejora de FPS ni superioridad sobre Sodium. Ultra no cambia el motor.

# Arquitectura inicial

## Alcance

Minecraft Java 26.3, Java 25, Fabric y ejecución únicamente en el cliente.
El plugin Loom sin remapeo corresponde al juego sin ofuscación; no añadir Yarn ni
copiar mixins de versiones anteriores sin inspeccionar las clases de 26.3.

El punto de entrada carga configuración y anuncia el estado. El código de cliente
vive en `src/client/java`; `fabric.mod.json` declara `environment: client`.
Hay dos candidatas experimentales desactivadas por defecto: la prueba booleana del frustum y
la visibilidad de bloques animados introducida en 0.0.5.
La interfaz compartida se ha comprobado con Sodium 0.9.2 e Iris 1.11.7; no hay pruebas con shaderpacks ni VulkanMod.

## Primera candidata: prueba booleana del frustum

En el cliente oficial 26.3, `Frustum.isVisible(AABB)` llama al método privado
`cubeInFrustum(DDDDDD)I`, que utiliza `FrustumIntersection.intersectAab` de JOML 1.10.9.
Luego acepta tanto `INSIDE` como `INTERSECT`, descartando la distinción que acaba de calcular.

El mixin redirige únicamente esa llamada hacia `FrustumIntersection.testAab`, conservando
las restas de cámara en double y su posterior conversión a float. Devuelve `INTERSECT`
para cajas aceptadas y un índice de plano para cajas rechazadas; el llamador solo consulta
si el resultado es visible. Los otros consumidores de la clasificación completa no se alteran.
Se reutiliza la intersección del propio frustum: cambios de cámara, proyección, copias y offsets
siguen el estado de vanilla, sin una caché adicional ni invalidaciones nuevas.

`NovaSettings` carga una configuración inmutable al iniciar. Si `enabled=false` o
`fast_frustum=false`, el hook llama al método original. El mixin exige un punto de inyección;
un cambio incompatible en esa ruta falla explícitamente al cargar en vez de omitir la optimización.
La versión del mod está restringida a Minecraft 26.3.

Las pruebas diferenciales usan la clase original del juego. El mod auxiliar de arranque prueba
la clase transformada bajo Fabric y los interruptores. JMH mide una carga sintética del método
original y el cálculo de reemplazo. Esta evidencia no sustituye pruebas visuales, mediciones de
fotogramas, compatibilidad con otros mods ni sesiones prolongadas. La candidata permanece opt-in
hasta completar esas validaciones.

## Interfaz de 0.0.5-alpha

`NovaVideoSettingsScreen` usa una pantalla propia con panel lateral y filas de ajustes.
Cada fila mantiene un valor pendiente separado de la `OptionInstance` real. Aplicar y Aceptar
invocan `OptionInstance.set`, conservando los callbacks del motor; Escape descarta lo pendiente.
El callback de FPS actualiza `FramerateLimitTracker`, VSync invalida la configuración de la
superficie, y `Options.save` persiste los valores y envía las preferencias al servidor.
Pantalla completa se aplica al final porque puede redimensionar la interfaz; la resolución usa
los modos del monitor y `Window.changeFullscreenVideoMode`. Las filas conservan su estado al
redimensionar, admiten ratón, teclado y desplazamiento con rueda.
El mixin de `Gui.setScreen` sustituye únicamente instancias de la clase vanilla exacta;
no redirige subclases de otros mods. Mod Menu registra un factory opcional para la misma pantalla.
La dependencia de Mod Menu es `compileOnly`; solo se añade al cliente de desarrollo con
`-PwithModMenu=true`. No se incluye en el JAR ni es necesaria para arrancar.

Los rangos de `Options` se modifican antes de cargar `options.txt`, preservando sus callbacks y
codecs: renderizado 2–50 y simulación 5–32. Dos mixins, registrados solo en el entorno cliente,
amplían el clamp de `ChunkMap` y el radio del tracker de tickets de `DistanceManager` a 50 para
el servidor integrado. No cambian servidores remotos ni el radio de spawn natural.

Ultra guarda únicamente una preferencia. El frustum experimental conserva su interruptor
independiente y no se activa con Ultra. Guardar la preferencia vuelve a leer las propiedades,
conserva claves ajenas y usa un archivo temporal con reemplazo atómico cuando el sistema lo admite;
un archivo inválido no se sobrescribe. La carga al iniciar sigue siendo conservadora.

## Etapas siguientes

- Diagnóstico: medir el coste del capturador auxiliar y ampliar escenarios, recorridos y metadatos.
- Visibilidad: instrumentar primero las rutas reales de 26.3 y cuantificar su coste antes de diseñar cachés.
- Invalidación: contemplar posición/orientación de cámara, proyección/FOV, distancia de renderizado,
  carga/descarga y reconstrucción de chunks, bloques, dimensión, recursos y estado del renderizador.
  Si no se puede demostrar validez, recalcular con el comportamiento original.
- Cada técnica tendrá su propio interruptor y podrá desactivarse; activación por defecto solo con evidencia.
- Integración gráfica: aislar los puntos que dependan del backend cuando exista una necesidad concreta.
  No asumir OpenGL, acceso a temporizadores GPU ni que otro mod conserve las mismas rutas.

## Criterio de activación por defecto

Compilación y arranque correctos, una optimización con mejora repetible, ausencia de regresiones visuales
en la matriz definida, sesiones largas y resultados con hardware, versiones y configuración completos.
Compilar un JAR no satisface estos criterios. La versión del proyecto identifica el trabajo hacia la alpha.

## Calidad y renderer opcional

Las opciones nativas conservan OptionInstance y sus callbacks; los cambios de mipmaps,
filtrado y anisotropía solicitan recarga de recursos. NovaQualitySettings publica un snapshot
inmutable persistido atómicamente en luminanova-quality.properties. Un cambio de sampler
invalida el sampler del terreno; los cambios de fluidos reconstruyen los chunks.
TerrainSamplerMixin cambia los filtros de magnificación/minificación; FluidQualityMixin
elimina superficies superiores ocultas por el bloque anegado y ajusta la altura visual
en esquinas anegadas; EntitySortMixin ordena submits de entidades por distancia a su AABB.
Estas políticas independientes no reproducen exactamente los algoritmos de Sodium.

NovaMixinPlugin omite esos tres mixins al detectar Sodium. NovaSodiumIntegration registra
páginas mediante ConfigEntryPoint y vincula las cuatro políticas a las opciones de su
renderer. La dependencia es compileOnly: no se empaqueta y no se carga sin Sodium.
NovaScreens elige el menú nativo compartido cuando está presente; Mod Menu usa ese factory.
La entrada vanilla solo redirige la clase exacta, conservando pantallas de otros mods.

## Visibilidad de bloques animados en 0.0.5

`BlockEntityVisibilityMixin` se instala en `BlockEntityRenderDispatcher.tryExtractRenderState` antes
de crear el estado. No sustituye la simulación ni el renderizador de chunks. `NovaPerformanceSettings`
publica un snapshot inmutable; el archivo independiente se guarda mediante reemplazo atómico y se conserva
si contiene valores inválidos. La candidata permanece desactivada por defecto.

Se aceptan solamente clases exactas de bloques, entidades de bloque y renderizadores vanilla conocidos.
El frustum usa una caja con un bloque de margen alrededor de la celda; una cámara capturada conserva su
frustum capturado y el modo panorámico conserva el original. Cofres simples cerrados pueden omitirse si sus
seis vecinos tienen `isSolidRender`; una cámara dentro de ese volumen, una tapa animada, un cofre doble,
un renderer desconocido y un overlay de rotura impiden esa segunda optimización. No hay resultados almacenados
entre fotogramas: cámara y bloques se consultan nuevamente.

`NovaRenderCompatibility` determina quién gestiona la ocultación. Con Entity Culling, More Culling, Iris
o VulkanMod, el plugin omite por completo el mixin nuevo y las pantallas explican la decisión.
Con Sodium también se omite el frustum antiguo, además de las tres políticas gráficas que ya cedían al renderer.
La candidata nueva puede funcionar con Sodium y Lithium cuando no hay otro dueño de esa ruta.

Los IDs de opciones compartidas pasan por `NovaOptionIds`, que usa minúsculas con `Locale.ROOT`,
independientemente del idioma del sistema. Las traducciones mantienen sus claves originales.
Esto corrige `IdentifierException: luminanova:options.renderDistance` en un cliente instalado;
el entorno de desarrollo anterior no ejercitaba esa validación.

Las tareas empaquetadas arrancan `KnotClient` con `fabric.development=false`, el JAR real y dependencias
en un directorio `mods` aislado. Exigen un marcador final para detectar cierres anticipados aun cuando
el proceso devuelve cero. El mod auxiliar contiene el muestreo de intervalos en memoria y sus contadores;
ninguna de esas clases ni sus mixins forma parte del JAR instalable.

## Cola del servidor integrado

La 0.0.4 elevaba el radio de tickets de jugador de 32 a 50, pero conservaba la capacidad vanilla
de `ChunkTaskPriorityQueue`. Al cerrar el mundo se podía solicitar el nivel 52 en una cola de 46
entradas. `ChunkTaskPriorityMixin` amplía `PRIORITY_LEVEL_COUNT` después del inicializador estático,
preservando el margen de generación de `ChunkLevel.MAX_LEVEL` y añadiendo los 18 niveles del radio
ampliado. No cambia prioridades ni reglas de generación. La prueba de interfaz reproduce submit/resort
en el nivel que fallaba; la prueba de visibilidad también verifica la salida real del mundo.

# Lumina Nova

Mod experimental de optimización para **Minecraft Java 26.3**, exclusivamente de cliente con Fabric.
Versión experimental: `0.0.5-alpha`. Incluye un menú más uniforme con General, Calidad y Optimización,
búsqueda, integración opcional con Mod Menu y el menú compartido de Sodium/Iris.
Corrige el crash del JAR de 0.0.4 al registrar opciones en Sodium.
La nueva candidata evita trabajo de renderizado de bloques animados invisibles. El piloto con 6.300 cofres
ocultos obtuvo **3,30×** (76,9 → 253,7 FPS); el control visible bajó de 71,2 a 69,5 FPS.
Es una escena artificial a 854×480, no una mejora general; consulta la [validación](docs/ALPHA_0.0.5_VALIDATION.md).
**No se garantiza una mejora general de 3× o 5×** ni superioridad sobre Sodium. Solo admite 26.3.

## Ajustes de vídeo

![Menú General de 0.0.5-alpha](docs/results/alpha5/ui/alpha5-general.png)

Abre **Opciones → Ajustes de vídeo**. Con Mod Menu instalado, Lumina Nova aparece con su logo
y su botón de configuración abre la misma pantalla. Mod Menu 21.0.0 para 26.3 es opcional.

- Renderizado: 2–50 chunks; simulación: 5–32 chunks.
- Brillo, tamaño de interfaz, pantalla completa y pantalla completa exclusiva.
- Resolución de pantalla completa, con los modos disponibles del monitor.
- VSync y FPS máximos, de 10 a 250 en pasos de 10, más Sin límite al extremo del slider.
  Por ejemplo, 120 actualiza el limitador real del juego a 120 al pulsar Aplicar/Aceptar.
- En macOS, visibilidad del menú/Dock en pantalla completa.
- **Ultra Optimization (vista previa)**: interruptor que guarda una preferencia; en esta alpha
  no modifica el motor, los chunks ni las sombras y no tiene efecto sobre los FPS. Se indica en
  la pantalla y en su tooltip. Su implementación queda pendiente para una futura release.

Pulsa **Aplicar** para guardar y activar los cambios, o **Aceptar** para aplicarlos y volver.
Escape vuelve sin aplicar los cambios pendientes. La interfaz usa una barra lateral y filas
con el nombre a la izquierda y el valor a la derecha. Haz clic o arrastra la fila para ajustar
un valor; con teclado, usa Tab y las flechas izquierda/derecha. La rueda desplaza las opciones. Los ajustes del juego se guardan en `options.txt`;
la preferencia Ultra se guarda como `ultra_optimization` en `config/luminanova.properties`.
Guardar esta preferencia conserva las demás propiedades y normaliza el formato del archivo.
Si el archivo es inválido o no se puede guardar, se muestra un error y se conserva.
La interfaz funciona también dentro de un mundo; los cambios de vídeo usan los controles de Minecraft.

En un mundo local se amplían el límite de carga y el radio de tickets del servidor integrado a 50.
En multijugador, el servidor sigue controlando las distancias efectivas de carga y simulación.
Subir las distancias aumenta el trabajo y la memoria necesarios; no es una optimización de FPS.
El despliegue de terreno y sesiones largas a 50 chunks aún requiere pruebas de estabilidad.

## Calidad e integración opcional

Calidad ofrece transparencia mejorada, nubes y su distancia, radio del clima, hojas,
partículas, iluminación suave, mezcla de biomas, distancia y sombras de entidades,
viñeta, transición de chunks, mipmaps, filtrado de texturas y anisotropía.
Estos controles aplican las opciones reales de Minecraft; apagar Nubes elimina las nubes del mundo.
Los cambios de mipmaps, filtrado y anisotropía recargan los recursos de texturas.

También ofrece interpolación de texels, ocultación de fluidos, forma de fluidos y ordenación
de entidades. Sin Sodium, las políticas propias se guardan en
`config/luminanova-quality.properties`: sampler Nearest/Linear, eliminación de la cara superior
oculta de fluidos en bloques anegados, ajuste visual de altura junto a bloques anegados y
ordenación por el punto más cercano de la caja de entidades. Los valores iniciales conservan
el comportamiento vanilla. Son cambios visuales, no una mejora de FPS medida.

Con **Sodium 0.9.2 para 26.3** instalado, Lumina registra General, Calidad y Optimización dentro de su
menú nativo compartido. **Iris 1.11.7** conserva su sección y opciones en esa misma interfaz.
Las últimas cuatro opciones usan entonces la configuración real de Sodium y sus algoritmos;
los mixins gráficos propios de Lumina se desactivan para evitar competir con ese renderizador.
Iris requiere Sodium. Ambos son opcionales para Lumina: también funciona con solo Fabric API.
No se incluyen esos mods en el JAR.

## Primera optimización de bloques animados

En **Optimización → Visibilidad de bloques animados**, activa la candidata experimental y pulsa Aplicar.
Se guarda en `config/luminanova-performance.properties` como `block_entity_culling=true` y se aplica
sin reiniciar. Su valor inicial es `false` mientras se completan pruebas prolongadas.

La candidata evita crear estados y enviar modelos de cofres, cofres de Ender, shulkers y vasijas vanilla
fuera del frustum de cámara. Usa límites amplios para conservar cofres dobles, tapas abiertas y animaciones.
También omite cofres simples cerrados rodeados en sus seis caras por bloques completamente opacos.
Abrir cualquier cara restaura la extracción en ese mismo fotograma; vidrio, hojas y bloques parciales
no cuentan como barreras. No hay caché de visibilidad entre fotogramas.

No reduce calidad, distancias, entidades ni sus ticks. Lithium mantiene sus optimizaciones de lógica;
Nova trabaja en la preparación y envío de modelos del cliente. Con Sodium sin los mods siguientes,
esta candidata permanece disponible. El frustum antiguo y las políticas gráficas de Nova ceden sus rutas a Sodium.

Con **Entity Culling, More Culling, Iris o VulkanMod**, la candidata nueva se desactiva automáticamente,
el control indica qué mod gestiona esa ruta y no se instala su mixin. Es una decisión conservadora para
no duplicar ocultación ni afectar pases de sombras. La interfaz y las demás funciones siguen disponibles.
Los renderizadores personalizados, efectos fuera del bloque y overlays de rotura conservan la ruta original.
VulkanMod no tiene una versión para 26.3 en la fecha de esta validación y no se ha probado.

Consulta [la evidencia, las versiones probadas y los límites de 0.0.5](docs/ALPHA_0.0.5_VALIDATION.md).
No todos los mods de optimización han sido probados y no todos son compatibles entre sí.

## Desarrollo

- `main`: base estable y cambios validados.
- `luminanovadev`: desarrollo de la siguiente alpha.
- Integrar en `main` después de revisar compilación, comportamiento y evidencia de rendimiento cuando corresponda.

Dependencias fijadas en `gradle.properties`: Minecraft 26.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3, Mod Menu 21.0.0 (solo compilación/integración opcional)
y Loom 1.17.21. Gradle Wrapper 9.6.0; JDK 25 necesario para compilar.
El wrapper comprueba el SHA-256 de la distribución. No hay versiones dinámicas ni SNAPSHOT.

```sh
./gradlew build
./gradlew runClient
./gradlew runClient -PwithModMenu=true
./gradlew runClient -PwithIris=true
./gradlew runClient -PwithSodium=true -PwithLithium=true
```

En macOS, si ya tienes JDK 25 instalado: `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`.
La primera compilación descarga Gradle, Minecraft y sus dependencias.
El JAR instalable queda en `build/libs/lumina-nova-0.0.5-alpha.jar`;
el archivo `-sources.jar` contiene código fuente y no se instala como mod.
Para probarlo, usa una instalación separada de Minecraft 26.3 con Fabric Loader y Fabric API.

Al iniciar el cliente se crea `config/luminanova.properties`. `enabled=false` desactiva
las candidatas de rendimiento; la interfaz y sus rangos siguen disponibles. Hay que reiniciar
el juego para aplicar cambios manuales a `enabled` y `fast_frustum`.
Un archivo inválido o ilegible desactiva Nova y deja una advertencia en el registro sin sobrescribirlo.
La optimización experimental requiere añadir `fast_frustum=true` al archivo, manteniendo
`enabled=true`, y reiniciar. Su valor por defecto es `false`; los archivos anteriores se conservan.
La opción evita calcular contención completa cuando el renderizador solo necesita saber si una
caja es visible, usando los mismos planos y conversiones numéricas de Minecraft.
La candidata del frustum no guarda resultados entre fotogramas ni modifica distancias, calidad o las pruebas que necesitan
la clasificación completa. Hay un mixin sobre `Frustum.isVisible`, omitido con Sodium o VulkanMod. No se ha validado con todos los mods
que modifiquen esa misma ruta. La prueba auxiliar de 0.0.5 captura intervalos entre fotogramas en mundos controlados;
no es un capturador de uso general incluido en el JAR.

GitHub Actions compila los pushes y pull requests de ambas ramas y conserva los JAR como artefactos;
no publica releases automáticamente.

## Objetivo y próximos pasos

La meta experimental es alcanzar hasta 5× en escenarios definidos conservando calidad y ajustes.
El piloto de cofres aporta una mejora localizada; aún faltan recorridos representativos,
resoluciones mayores y sesiones prolongadas. No demuestra superioridad sobre otros mods.

1. Completar más pruebas visuales y sesiones de estabilidad de la optimización experimental en mundos reales.
2. Ampliar el capturador auxiliar de fotogramas y medir su propio coste.
3. Establecer referencias de vanilla, Nova, Sodium y VulkanMod cuando existan versiones compatibles.
4. Perfilar las pruebas de visibilidad e investigar reutilización de resultados con invalidación correcta.
5. Activar optimizaciones por defecto solo con una mejora medida, pruebas visuales y estabilidad sostenida.
6. Implementar y medir Ultra Optimization; ampliar después las pestañas y versiones compatibles.

Consulta [el protocolo de pruebas](docs/BENCHMARKS.md) y [las decisiones de arquitectura](docs/ARCHITECTURE.md).

### Verificación de la candidata

```sh
./gradlew test
./gradlew runFrustumSmoke -PfastFrustum=true
./gradlew runFrustumSmoke -PfastFrustum=false
./gradlew runFrustumSmoke -PfastFrustum=true -PnovaEnabled=false
./gradlew frustumBenchmark
./gradlew runUiSmoke
./gradlew runUiSmoke -PwithModMenu=true
./gradlew runQualityWorld
./gradlew runCompatSmoke -PwithIris=true
./gradlew runPackagedCompat -PwithSodium=true -PwithLithium=true
./gradlew runPackagedCompat -PwithIris=true -PwithLithium=true
./gradlew runVisibilityWorld
./gradlew runPerformanceWorld -PwithLithium=true
./gradlew runPerformanceWorld -PbenchmarkSpacing=2 -PwithSodium=true -PwithLithium=true
```

La prueba de arranque usa un mod auxiliar que compara la clase transformada con el método original
y cierra el cliente automáticamente. No valida imágenes ni una sesión de juego. El mod auxiliar,
JUnit y JMH no forman parte del JAR instalable. El benchmark sintético compara el método original
con el cálculo de reemplazo; no mide FPS ni el coste completo de la integración con Fabric.
Los datos de JMH se guardan en `build/frustum-benchmark.json`. Consulta los
[resultados de la candidata del frustum](docs/ALPHA_0.0.1_RESULTS.md).
La prueba de interfaz comprueba navegación, guardado y recarga de ajustes, rangos del servidor
integrado e integración registrada en Mod Menu, y captura el framebuffer del juego para revisar
la presentación. Usa `run-ui-smoke/`, una instalación de prueba separada que se reinicia cada vez.
Consulta [la validación de 0.0.5-alpha](docs/ALPHA_0.0.5_VALIDATION.md) y [el logo](docs/branding/README.md).

Referencias: [Fabric para 26.3](https://fabricmc.net/2026/09/15/263.html),
[proyecto de ejemplo oficial](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

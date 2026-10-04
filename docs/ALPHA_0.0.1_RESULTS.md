# Evidencia experimental de 0.0.1-alpha.1

Fecha: 2026-10-04. Plataforma objetivo: Minecraft 26.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3; compilación con Loom 1.17.21 y Gradle 9.6.0.

La candidata reduce trabajo de CPU en `Frustum.isVisible(AABB)`: sustituye la clasificación
completa por una prueba booleana equivalente. **No hay resultados de FPS ni una comparación
con Sodium.** La prerelease es experimental y la optimización requiere `fast_frustum=true`.

## Verificaciones completadas

- `./gradlew build`: compilación, empaquetado y cinco pruebas JUnit satisfactorias.
- Comparación contra la clase original de Minecraft: 1.000.000 de cajas en 100 cámaras y
  proyecciones, incluyendo coordenadas cercanas a los límites del mundo; cero diferencias.
- 675 casos adicionales de bordes de planos, cajas degeneradas, infinito y NaN; cero diferencias.
- Configuración: creación de valores por defecto, conservación de archivos anteriores,
  activación explícita y desactivación ante archivos inválidos o ilegibles.
- `runFrustumSmoke` bajo Fabric: 300.000 comparaciones de la clase transformada por cada modo
  (`fast_frustum=true`, `fast_frustum=false`, `enabled=false`), cero diferencias. Se verifica
  que el mixin se aplica y selecciona la ruta esperada, con copias y offsets del frustum.
- El JAR instalable contiene únicamente el mod; no incluye JUnit, JMH ni el mod auxiliar.
- Arranque normal con `runClient` y la candidata activada: inicialización del mod,
  creación de ventana SDL y carga de recursos con el backend OpenGL de Apple M4.

La prueba de arranque cierra el cliente después de las comparaciones, antes de jugar.
No es una prueba visual ni una sesión de estabilidad.

## Microbenchmark de CPU

Equipo: MacBook Air, Apple M4 (10 núcleos), 16 GB RAM, macOS 27.0.1 arm64.
JVM: Eclipse Temurin 25.0.4.1+1. JOML: 1.10.9, la biblioteca utilizada por Minecraft 26.3.
JMH: 1.37, modo AverageTime, un hilo, tres JVM independientes por caso, tres iteraciones
de calentamiento de un segundo y cinco iteraciones de medición de un segundo por JVM.
Cada invocación procesa 4096 cajas preconstruidas; el tiempo se normaliza por caja.
No se asignan cajas durante la medición. La semilla es 263, FOV 70°, aspecto 16:9,
planos 0.05–512, cámara (29999000.25, 80.5, -29999000.25).

Se compara el llamador original del juego sin mixins contra el cálculo de reemplazo y
su conversión a booleano. **No se mide el coste completo del mixin/Fabric, ni fotogramas,
GPU o una escena del juego.** Se ejecuta primero Nova y después vanilla; las cifras son
una observación local de una carga sintética, no una garantía para otros equipos.

| Distribución | Vanilla (ns/caja) | Nova (ns/caja) | Reducción de tiempo |
| --- | --- | --- | --- |
| Todas visibles | 11.305 ± 0.203 | 7.760 ± 0.129 | 31.4% |
| Mezcla (2087/4096 visibles) | 5.308 ± 0.061 | 3.928 ± 0.033 | 26.0% |
| Todas fuera | 3.308 ± 0.074 | 2.928 ± 0.045 | 11.5% |

Los errores son los intervalos de confianza al 99.9% reportados por JMH, con 15 muestras
por método y distribución. Se conservan todas las muestras y parámetros en
[el JSON de JMH](results/26.3-frustum-jmh.json).

Una primera ejecución coincidió con descargas y arranques de validación del proyecto;
no se utiliza como resultado de referencia. Se repitió la matriz completa sin esas tareas.
Los números de esta tabla corresponden a esa segunda ejecución, no a una selección del
mejor fork. Comando reproducible: `./gradlew frustumBenchmark`.

## Pendientes antes de activar por defecto

Medir tiempos de fotograma y el coste completo del hook dentro de mundos controlados;
pruebas visuales de geometría, transparencias, cambios de cámara y recursos; cambios de
dimensión, distancias y FOV; sesiones de al menos 60 minutos; compatibilidad con otros mods.
La reducción del tiempo de esta función no equivale a esa misma ganancia en FPS.
El [protocolo de rendimiento](BENCHMARKS.md) sigue siendo el criterio para afirmar una
mejora del juego completo y para comparar con otros optimizadores.

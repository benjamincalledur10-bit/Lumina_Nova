# Lumina Nova

Mod experimental de optimización para **Minecraft Java 26.3**, exclusivamente de cliente con Fabric.
Versión experimental: `0.0.2-alpha`. Incluye la pestaña General de ajustes de vídeo, integración
opcional con Mod Menu, un logo propio y la optimización experimental del frustum de la alpha anterior.
**No hay una mejora de FPS validada** ni evidencia de superioridad sobre Sodium. Solo admite 26.3.

## Ajustes de vídeo

Abre **Opciones → Ajustes de vídeo**. Con Mod Menu instalado, Lumina Nova aparece con su logo
y su botón de configuración abre la misma pantalla. Mod Menu 21.0.0 para 26.3 es opcional.

- Renderizado: 2–50 chunks; simulación: 5–32 chunks.
- Brillo, pantalla completa y pantalla completa exclusiva.
- Resolución de pantalla completa, con los modos disponibles del monitor.
- VSync y FPS máximos, con el límite sin restricciones de Minecraft al extremo del slider.
- En macOS, visibilidad del menú/Dock en pantalla completa.
- **Ultra Optimization (vista previa)**: interruptor que guarda una preferencia; en esta alpha
  no modifica el motor, los chunks ni las sombras y no tiene efecto sobre los FPS. Se indica en
  la pantalla y en su tooltip. Su implementación queda pendiente para una futura release.

Pulsa **Hecho** o Escape para guardar y volver. Los ajustes del juego se guardan en `options.txt`;
la preferencia Ultra se guarda como `ultra_optimization` en `config/luminanova.properties`.
Guardar esta preferencia conserva las demás propiedades y normaliza el formato del archivo.
Si el archivo es inválido o no se puede guardar, se muestra un error y se conserva.
La interfaz funciona también dentro de un mundo; los cambios de vídeo usan los controles de Minecraft.

En un mundo local se amplían el límite de carga y el radio de tickets del servidor integrado a 50.
En multijugador, el servidor sigue controlando las distancias efectivas de carga y simulación.
Subir las distancias aumenta el trabajo y la memoria necesarios; no es una optimización de FPS.
El despliegue de terreno y sesiones largas a 50 chunks aún requiere pruebas de estabilidad.

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
```

En macOS, si ya tienes JDK 25 instalado: `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`.
La primera compilación descarga Gradle, Minecraft y sus dependencias.
El JAR instalable queda en `build/libs/lumina-nova-0.0.2-alpha.jar`;
el archivo `-sources.jar` contiene código fuente y no se instala como mod.
Para probarlo, usa una instalación separada de Minecraft 26.3 con Fabric Loader y Fabric API.

Al iniciar el cliente se crea `config/luminanova.properties`. `enabled=false` desactiva
la candidata de rendimiento; la interfaz y sus rangos siguen disponibles. Hay que reiniciar
el juego para aplicar cambios manuales a `enabled` y `fast_frustum`.
Un archivo inválido o ilegible desactiva Nova y deja una advertencia en el registro sin sobrescribirlo.
La optimización experimental requiere añadir `fast_frustum=true` al archivo, manteniendo
`enabled=true`, y reiniciar. Su valor por defecto es `false`; los archivos anteriores se conservan.
La opción evita calcular contención completa cuando el renderizador solo necesita saber si una
caja es visible, usando los mismos planos y conversiones numéricas de Minecraft.
La candidata del frustum no guarda resultados entre fotogramas ni modifica distancias, calidad o las pruebas que necesitan
la clasificación completa. Hay un mixin sobre `Frustum.isVisible`; no se ha validado con otros mods
que modifiquen esa misma ruta. Todavía no hay captura de tiempos de fotograma dentro del juego.

GitHub Actions compila los pushes y pull requests de ambas ramas y conserva los JAR como artefactos;
no publica releases automáticamente.

## Objetivo y próximos pasos

La meta experimental es alcanzar hasta 5× en escenarios definidos conservando calidad y ajustes.
No hay mediciones que demuestren ese resultado, superioridad sobre otros mods ni originalidad de las técnicas.

1. Completar pruebas visuales y sesiones de estabilidad de la optimización experimental en mundos reales.
2. Implementar captura de tiempos de fotograma y exportación de resultados, midiendo su propio coste.
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
Consulta [la validación de 0.0.2-alpha](docs/ALPHA_0.0.2_VALIDATION.md) y [el logo](docs/branding/README.md).

Referencias: [Fabric para 26.3](https://fabricmc.net/2026/09/15/263.html),
[proyecto de ejemplo oficial](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

# Lumina Nova

Mod experimental de optimización para **Minecraft Java 26.3**, exclusivamente de cliente con Fabric.
Versión experimental: `0.0.1-alpha.1`. Incluye una primera optimización de CPU en las pruebas
booleanas de visibilidad, activable explícitamente. **No hay una mejora de FPS validada**
ni evidencia de superioridad sobre Sodium. Solo es compatible con 26.3.

## Desarrollo

- `main`: base estable y cambios validados.
- `luminanovadev`: desarrollo de la siguiente alpha.
- Integrar en `main` después de revisar compilación, comportamiento y evidencia de rendimiento cuando corresponda.

Dependencias fijadas en `gradle.properties`: Minecraft 26.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3 y Loom 1.17.21. Gradle Wrapper 9.6.0; JDK 25 necesario para compilar.
El wrapper comprueba el SHA-256 de la distribución. No hay versiones dinámicas ni SNAPSHOT.

```sh
./gradlew build
./gradlew runClient
```

En macOS, si ya tienes JDK 25 instalado: `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`.
La primera compilación descarga Gradle, Minecraft y sus dependencias.
El JAR instalable queda en `build/libs/lumina-nova-0.0.1-alpha.1.jar`;
el archivo `-sources.jar` contiene código fuente y no se instala como mod.
Para probarlo, usa una instalación separada de Minecraft 26.3 con Fabric Loader y Fabric API.

Al iniciar el cliente se crea `config/luminanova.properties`. `enabled=false` desactiva
la inicialización funcional de Nova; hay que reiniciar el juego para aplicar los cambios.
Un archivo inválido o ilegible desactiva Nova y deja una advertencia en el registro sin sobrescribirlo.
La optimización experimental requiere añadir `fast_frustum=true` al archivo, manteniendo
`enabled=true`, y reiniciar. Su valor por defecto es `false`; los archivos anteriores se conservan.
La opción evita calcular contención completa cuando el renderizador solo necesita saber si una
caja es visible, usando los mismos planos y conversiones numéricas de Minecraft.
No guarda resultados entre fotogramas ni modifica distancias, calidad o las pruebas que necesitan
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
5. Publicar la primera alpha solo con una mejora medida, pruebas visuales y estabilidad sostenida.

Consulta [el protocolo de pruebas](docs/BENCHMARKS.md) y [las decisiones de arquitectura](docs/ARCHITECTURE.md).

### Verificación de la candidata

```sh
./gradlew test
./gradlew runFrustumSmoke -PfastFrustum=true
./gradlew runFrustumSmoke -PfastFrustum=false
./gradlew runFrustumSmoke -PfastFrustum=true -PnovaEnabled=false
./gradlew frustumBenchmark
```

La prueba de arranque usa un mod auxiliar que compara la clase transformada con el método original
y cierra el cliente automáticamente. No valida imágenes ni una sesión de juego. El mod auxiliar,
JUnit y JMH no forman parte del JAR instalable. El benchmark sintético compara el método original
con el cálculo de reemplazo; no mide FPS ni el coste completo de la integración con Fabric.
Los datos de JMH se guardan en `build/frustum-benchmark.json`. Consulta los
[resultados y límites de esta prerelease](docs/ALPHA_0.0.1_RESULTS.md).

Referencias: [Fabric para 26.3](https://fabricmc.net/2026/09/15/263.html),
[proyecto de ejemplo oficial](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

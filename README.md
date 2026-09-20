# Lumina Nova

Mod experimental de optimización para **Minecraft Java 26.3**, exclusivamente de cliente con Fabric.
Versión de desarrollo: `0.0.1-alpha.1`. Esta base todavía **no mejora los FPS** ni constituye una alpha validada.

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
Por ahora no hay modificaciones del renderizador, mixins, captura de métricas ni interruptores de optimizaciones individuales.

GitHub Actions compila los pushes y pull requests de ambas ramas y conserva los JAR como artefactos;
no publica releases automáticamente.

## Objetivo y próximos pasos

La meta experimental es alcanzar hasta 5× en escenarios definidos conservando calidad y ajustes.
No hay mediciones que demuestren ese resultado, superioridad sobre otros mods ni originalidad de las técnicas.

1. Validar el arranque de esta base en Minecraft 26.3.
2. Implementar captura de tiempos de fotograma y exportación de resultados, midiendo su propio coste.
3. Establecer referencias de vanilla, Nova, Sodium y VulkanMod cuando existan versiones compatibles.
4. Perfilar las pruebas de visibilidad e investigar reutilización de resultados con invalidación correcta.
5. Publicar la primera alpha solo con una mejora medida, pruebas visuales y estabilidad sostenida.

Consulta [el protocolo de pruebas](docs/BENCHMARKS.md) y [las decisiones de arquitectura](docs/ARCHITECTURE.md).

Referencias: [Fabric para 26.3](https://fabricmc.net/2026/09/15/263.html),
[proyecto de ejemplo oficial](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

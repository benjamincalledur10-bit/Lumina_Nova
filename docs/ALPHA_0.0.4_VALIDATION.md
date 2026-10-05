# Validación de v0.0.4-alpha

Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 y Java 25.
Solo se adjunta el JAR instalable a la pre-release.

## Comprobaciones dentro del juego

Los registros de las ejecuciones locales contienen:

- `LUMINA_UI_OK modmenu=true` (21:00:32): opciones generales y de calidad mediante
  widgets reales, Aplicar/Aceptar, descarte, búsqueda, escalas de interfaz, persistencia,
  FPS 120 en el limitador activo, pantalla completa y fábrica de Mod Menu.
- `LUMINA_COMPAT_OK sodium=0.9.2 iris=1.11.7` (20:59:33): menú nativo compartido,
  módulos Lumina/Iris registrados, nubes y limitador nativos, configuración y persistencia
  de las cuatro políticas gráficas de Sodium.
- `LUMINA_QUALITY_WORLD_OK clouds gpuSampler fluidCulling fluidShaping entitySorting`
  (20:59:00): mundo real, nubes activas y luego ausentes tras apagarlas desde Calidad,
  sampler GPU Nearest/Linear, eliminación de superficie de fluido oculta en losa anegada
  superior conservando la superficie visible de la inferior, altura alternativa junto a
  bloques anegados y distancia de ordenación de entidades en la clase transformada.

Las capturas locales en `run-quality-world/screenshots/` muestran el mismo mundo con
nubes activadas/apagadas; `run-ui-smoke/screenshots/` y `run-compat-smoke/screenshots/`
conservan las comprobaciones visuales del menú. Se generan al ejecutar las pruebas indicadas.
El mod auxiliar, JUnit y JMH no se incluyen en el JAR del mod.

## Comprobación final de compilación

`./gradlew --offline --no-daemon build testmodClasses benchmarkClasses` terminó
satisfactoriamente con Java 25. JUnit ejecutó doce pruebas: doce satisfactorias, cero fallos.
Los siete casos anteriores y cinco nuevos cubren persistencia conservadora de Calidad y
distancia a cajas de entidades. El JAR instalable se genera mediante Gradle; no contiene
las clases de pruebas, benchmarks ni dependencias opcionales.

La prueba final de mundo repitió sampler, fluidos y entidades, apagó nubes mediante la interfaz
y comprobó además transparencia mejorada en el estado real de renderizado.

## Límites

Ultra Optimization sigue siendo una preferencia de vista previa sin efecto de rendimiento.
No hay mejora de FPS medida, comparación de rendimiento contra Sodium, pruebas con shaderpacks
ni sesiones prolongadas a 50 chunks. Iris se probó junto a Sodium; no funciona como dependencia
independiente. Las políticas gráficas propias de Lumina y las de Sodium tienen implementaciones
diferentes. Los servidores remotos conservan sus límites efectivos de distancias.


JAR Gradle: `lumina-nova-0.0.4-alpha.jar`. SHA-256:
`44c671578a981f2f89ae90cc3f4e3993828cc3215f52903b8653d0be9bd0eacb`.

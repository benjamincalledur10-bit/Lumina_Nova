# Protocolo de rendimiento

Este documento define pruebas futuras. Todavía no hay capturas ni resultados.

## Condiciones controladas

Registrar commit/JAR, Minecraft, loader, mods, Java y argumentos JVM; SO, CPU, GPU,
controlador/backend, RAM, energía y temperatura inicial. Anotar resolución real del framebuffer,
pantalla completa/ventana, VSync, límite de FPS, FOV, distancias de renderizado/simulación,
calidad, resource packs y shaders. Mantener constantes esos valores.

Guardar una copia del mundo de referencia con semilla y checksum. Restaurarla antes de cada ejecución.
Fijar hora, clima, dificultad, entidades y recorrido con duración y cámara reproducibles.
Separar pruebas de chunks ya cargados de generación/carga de terreno nuevo; no mezclarlas.
El recorrido automatizado y los mundos de referencia todavía están pendientes de creación.

| Escenario | Trabajo principal | Comprobación visual |
| --- | --- | --- |
| Bosque denso | Geometría, hojas y visibilidad | Hojas, transparencias y terreno |
| Ciudad | Muchas secciones visibles | Edificios y cambios de perspectiva |
| Cuevas | Oclusión y cambios de iluminación | Paredes y aperturas |
| Muchas entidades | Actualización y dibujo de entidades | Aparición y animación |
| Movimiento rápido | Carga y reconstrucción de chunks | Pop-in y secciones faltantes |

## Ejecución

1. Preparar perfiles separados para vanilla, Nova desactivado, Nova activado y cada competidor compatible.
   Comparar además Fabric + Fabric API sin Nova para separar el coste de la plataforma.
2. Calentar cada perfil durante 120 segundos y medir un recorrido de 180 segundos.
3. Realizar al menos cinco ejecuciones por perfil y escenario, alternando el orden para reducir sesgos térmicos.
4. Capturar tiempos entre fotogramas con el mismo método en todos los perfiles. Medir el coste del capturador
   con y sin captura; no comparar métodos incompatibles como si fueran equivalentes.
5. Guardar datos crudos y metadatos. Excluir carga inicial, menús o pausas solo mediante reglas fijadas previamente.
6. Hacer una sesión de estabilidad de al menos 60 minutos, incluyendo cambios de dimensión, recursos,
   cámara, FOV, distancias, ediciones de bloques y carga/descarga de chunks.

## Métricas y evaluación

- FPS medios = número de intervalos medidos / duración total de esos intervalos en segundos.
- Tiempo de fotograma en ms: mediana, p95 y p99; indicar método de percentil usado.
- Tirones: contar intervalos >50 ms y >100 ms y reportar duración de la captura.
- Memoria: diferenciar heap usado/comprometido y memoria del proceso; registrar pausas de GC cuando sea posible.
- CPU/GPU: usar tiempos del subsistema solo si la herramienta/backend los ofrece; no inferir tiempo GPU
  a partir de FPS ni confundir tiempo de CPU con tiempo completo del fotograma.

Reportar mediana y dispersión entre ejecuciones, no el mejor resultado aislado.
Ganancia de FPS = FPS de Nova / FPS de referencia, en cada escenario y hardware.
No extrapolar un 5× puntual al juego completo. Una técnica se mantiene desactivada por defecto
si su ganancia no supera el ruido o si produce regresiones visuales, de tirones o memoria sin resolver.

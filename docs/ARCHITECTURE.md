# Arquitectura inicial

## Alcance

Minecraft Java 26.3, Java 25, Fabric y ejecución únicamente en el cliente.
El plugin Loom sin remapeo corresponde al juego sin ofuscación; no añadir Yarn ni
copiar mixins de versiones anteriores sin inspeccionar las clases de 26.3.

El punto de entrada carga configuración y anuncia el estado. El código de cliente
vive en `src/client/java`; `fabric.mod.json` declara `environment: client`.
No hay optimización implementada ni compatibilidad comprobada con Sodium, VulkanMod o Iris.

## Etapas previstas, todavía no implementadas

- Diagnóstico: captura acotada en memoria, sin escritura por fotograma, exportación fuera de la medición.
- Visibilidad: instrumentar primero las rutas reales de 26.3 y cuantificar su coste antes de diseñar cachés.
- Invalidación: contemplar posición/orientación de cámara, proyección/FOV, distancia de renderizado,
  carga/descarga y reconstrucción de chunks, bloques, dimensión, recursos y estado del renderizador.
  Si no se puede demostrar validez, recalcular con el comportamiento original.
- Cada técnica tendrá su propio interruptor y podrá desactivarse; activación por defecto solo con evidencia.
- Integración gráfica: aislar los puntos que dependan del backend cuando exista una necesidad concreta.
  No asumir OpenGL, acceso a temporizadores GPU ni que otro mod conserve las mismas rutas.

## Criterio de primera alpha

Compilación y arranque correctos, una optimización con mejora repetible, ausencia de regresiones visuales
en la matriz definida, sesiones largas y resultados con hardware, versiones y configuración completos.
Compilar un JAR no satisface estos criterios. La versión del proyecto identifica el trabajo hacia la alpha.

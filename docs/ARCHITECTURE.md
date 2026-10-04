# Arquitectura inicial

## Alcance

Minecraft Java 26.3, Java 25, Fabric y ejecución únicamente en el cliente.
El plugin Loom sin remapeo corresponde al juego sin ofuscación; no añadir Yarn ni
copiar mixins de versiones anteriores sin inspeccionar las clases de 26.3.

El punto de entrada carga configuración y anuncia el estado. El código de cliente
vive en `src/client/java`; `fabric.mod.json` declara `environment: client`.
Hay una optimización experimental de la prueba booleana de visibilidad, desactivada por defecto.
No hay compatibilidad comprobada con Sodium, VulkanMod o Iris.

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

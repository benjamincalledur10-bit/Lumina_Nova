package io.github.benjamincalledur10.luminanova.render;

/** Squared distance to an entity's world-space bounding box, using double precision. */
public final class EntitySortDistance {
    private EntitySortDistance() { }
    public static double closest(double x, double y, double z, double width, double height,
                                 double cameraX, double cameraY, double cameraZ) {
        double half = width * 0.5;
        double dx = Math.max(0, Math.abs(cameraX - x) - half);
        double dy = Math.max(0, Math.max(y - cameraY, cameraY - y - height));
        double dz = Math.max(0, Math.abs(cameraZ - z) - half);
        return dx * dx + dy * dy + dz * dz;
    }
}

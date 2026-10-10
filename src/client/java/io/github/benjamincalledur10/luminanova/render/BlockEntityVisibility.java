package io.github.benjamincalledur10.luminanova.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/** No visibility cache: block edits and camera changes are reflected on the same frame. */
public final class BlockEntityVisibility {
    private static final Direction[] FACES = Direction.values();
    private BlockEntityVisibility() { }

    public static boolean enclosed(BlockGetter level, BlockPos position, Vec3 camera, BlockPos.MutableBlockPos scratch) {
        // A camera inside the chest or a neighboring occluder must retain vanilla rendering.
        if (camera.x >= position.getX() - 1 && camera.x <= position.getX() + 2
                && camera.y >= position.getY() - 1 && camera.y <= position.getY() + 2
                && camera.z >= position.getZ() - 1 && camera.z <= position.getZ() + 2) return false;
        for (Direction face : FACES) {
            scratch.set(position).move(face);
            if (!level.getBlockState(scratch).isSolidRender()) return false;
        }
        return true;
    }
}

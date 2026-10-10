package io.github.benjamincalledur10.luminanova.test;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.benjamincalledur10.luminanova.compat.NovaRenderCompatibility;
import io.github.benjamincalledur10.luminanova.config.NovaPerformanceConfig;
import io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/** Rendering guards in the transformed client, including third-party renderer fallback. */
public final class VisibilityWorldTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("luminanova.visibilityWorld")) return;
        try (var world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("tp @a 0 82 -40 0 30");
            BlockPos front=new BlockPos(0,74,0),back=new BlockPos(0,82,-50);
            world.getServer().runOnServer(server -> {
                var level=server.overworld();
                level.setBlock(front,Blocks.CHEST.defaultBlockState(),3);
                level.setBlock(back,Blocks.CHEST.defaultBlockState(),3);
                for (Direction direction:Direction.values()) level.setBlock(front.relative(direction),Blocks.STONE.defaultBlockState(),3);
                level.setBlock(new BlockPos(3,74,0),Blocks.SHULKER_BOX.defaultBlockState(),3);
                level.setBlock(new BlockPos(6,74,0),Blocks.DECORATED_POT.defaultBlockState(),3);
                level.setBlock(new BlockPos(3,82,-50),Blocks.SHULKER_BOX.defaultBlockState(),3);
                level.setBlock(new BlockPos(6,82,-50),Blocks.DECORATED_POT.defaultBlockState(),3);
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(30);
            context.runOnClient(mc -> {
                boolean available=io.github.benjamincalledur10.luminanova.config.NovaSettings.CONFIG.enabled()
                        && NovaRenderCompatibility.blockEntityOwner().isEmpty();
                NovaPerformanceSettings.apply(NovaPerformanceConfig.DEFAULT);
                check(extract(mc,front,null),"Vanilla enclosed chest retained when feature off");
                check(extract(mc,back,null),"Vanilla off-camera chest retained when feature off");
                NovaPerformanceSettings.apply(new NovaPerformanceConfig(true));
                check(extract(mc,front,null)!=available,"Enclosed chest behavior matches renderer ownership");
                check(extract(mc,back,null)!=available,"Off-camera chest behavior matches renderer ownership");
                check(extract(mc,new BlockPos(3,74,0),null),"Visible shulker retained");
                check(extract(mc,new BlockPos(6,74,0),null),"Visible pot retained");
                check(extract(mc,new BlockPos(3,82,-50),null)!=available,"Off-camera shulker behavior");
                check(extract(mc,new BlockPos(6,82,-50),null)!=available,"Off-camera pot behavior");
                if (available) {
                    check(extract(mc,front,new ModelFeatureRenderer.CrumblingOverlay(1,new PoseStack().last())),"Breaking overlay retained");
                    for (Direction face:Direction.values()) {
                        for (var state:new net.minecraft.world.level.block.state.BlockState[]{Blocks.AIR.defaultBlockState(),
                                Blocks.GLASS.defaultBlockState(),Blocks.OAK_LEAVES.defaultBlockState(),Blocks.OAK_SLAB.defaultBlockState()}) {
                            mc.level.setBlock(front.relative(face),state,3);
                            check(extract(mc,front,null),"Transparent/partial face restores chest immediately: "+face+" "+state);
                        }
                        mc.level.setBlock(front.relative(face),Blocks.STONE.defaultBlockState(),3);
                        check(!extract(mc,front,null),"Restored stone shell culls again");
                    }
                    check(!io.github.benjamincalledur10.luminanova.render.BlockEntityVisibility.enclosed(mc.level,front,
                            new Vec3(.5,74.5,.5),new BlockPos.MutableBlockPos()),"Camera inside shell never occluded");
                    testCustomRenderer(mc,back);
                }
            });
            context.takeScreenshot("alpha5-visibility-world");
            System.out.println("LUMINA_VISIBILITY_WORLD_OK owner="+NovaRenderCompatibility.blockEntityOwner());
        } catch (Exception failure) { throw new RuntimeException(failure); }
        finally { NovaPerformanceSettings.apply(NovaPerformanceConfig.DEFAULT); }
    }
    private static boolean extract(Minecraft mc,BlockPos position,ModelFeatureRenderer.CrumblingOverlay overlay) {
        var entity=mc.level.getBlockEntity(position);check(entity!=null,"Fixture loaded: "+position);
        return mc.levelRenderer.blockEntityRenderDispatcher().tryExtractRenderState(entity,0,overlay,false)!=null;
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void testCustomRenderer(Minecraft mc,BlockPos position) throws Exception {
        var dispatcher=mc.levelRenderer.blockEntityRenderDispatcher();
        var field=BlockEntityRenderDispatcher.class.getDeclaredField("renderers");field.setAccessible(true);
        var renderers=(Map<BlockEntityType<?>,BlockEntityRenderer<?,?>>)field.get(dispatcher);
        var type=mc.level.getBlockEntity(position).getType();
        var original=renderers.get(type);
        var copy=new java.util.HashMap<>(renderers);
        copy.put(type,new Wrapper((BlockEntityRenderer)original));
        field.set(dispatcher,copy);
        try { check(extract(mc,position,null),"Unknown renderer uses original extraction, even off camera"); }
        finally { field.set(dispatcher,renderers); }
    }
    private record Wrapper(BlockEntityRenderer<BlockEntity,BlockEntityRenderState> delegate)
            implements BlockEntityRenderer<BlockEntity,BlockEntityRenderState> {
        @Override public BlockEntityRenderState createRenderState() { return delegate.createRenderState(); }
        @Override public void extractRenderState(BlockEntity entity,BlockEntityRenderState state,float tick,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay) {
            delegate.extractRenderState(entity,state,tick,camera,overlay);
        }
        @Override public void submit(BlockEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
            delegate.submit(state,pose,collector,camera);
        }
    }
    private static void check(boolean success,String message) { if (!success) throw new AssertionError(message); }
}

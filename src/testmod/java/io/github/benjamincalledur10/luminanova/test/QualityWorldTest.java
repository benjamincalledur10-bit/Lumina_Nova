package io.github.benjamincalledur10.luminanova.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import io.github.benjamincalledur10.luminanova.config.NovaQualityConfig;
import io.github.benjamincalledur10.luminanova.config.NovaQualitySettings;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;

/** Real isolated flat world: verifies renderer state and transformed visual policy effects. */
public final class QualityWorldTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (Boolean.getBoolean("luminanova.performanceWorld") || Boolean.getBoolean("luminanova.visibilityWorld")) return;
        try (var world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 0 100 0 0 -35");
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(mc -> {
                mc.options.cloudStatus().set(CloudStatus.FANCY);
                mc.options.renderDistance().set(5);
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(120);
                NovaQualitySettings.apply(new NovaQualityConfig(false,true,true,true));
            });
            context.waitTicks(30);
            context.runOnClient(mc -> {
                check(mc.gameRenderer.gameRenderState().optionsRenderState.cloudStatus==CloudStatus.FANCY,"Cloud render snapshot on");
                Field field=mc.levelRenderer.getClass().getDeclaredField("chunkLayerSampler"); field.setAccessible(true);
                GpuSampler sampler=(GpuSampler)field.get(mc.levelRenderer);
                check(sampler!=null && sampler.getMagFilter()==FilterMode.NEAREST && sampler.getMinFilter()==FilterMode.NEAREST,"Active GPU terrain sampler nearest");
                verifyFluidPolicies(mc);
                verifyEntityPolicy(mc);
            });
            context.takeScreenshot("alpha4-world-clouds-on");
            context.runOnClient(mc -> {
                mc.gui.setScreen(new VideoSettingsScreen(null,mc,mc.options));
                ScreenActions.press(mc.gui.screen(),"luminanova.video.quality");
                ScreenActions.press(mc.gui.screen(),"options.renderClouds"); // Fancy -> Off.
                ScreenActions.press(mc.gui.screen(),"options.improvedTransparency");
                ScreenActions.press(mc.gui.screen(),"luminanova.video.accept");
            });
            context.waitTicks(15);
            context.runOnClient(mc -> {
                check(mc.options.cloudStatus().get()==CloudStatus.OFF,"Clouds disabled by actual UI");
                check(mc.gameRenderer.gameRenderState().optionsRenderState.improvedTransparency,"Improved transparency renderer active");
                check(mc.gameRenderer.gameRenderState().optionsRenderState.cloudStatus==CloudStatus.OFF,"Render snapshot clouds off");
                NovaQualitySettings.apply(new NovaQualityConfig(true,false,false,false));
            });
            context.waitTicks(15);
            context.runOnClient(mc -> {
                Field field=mc.levelRenderer.getClass().getDeclaredField("chunkLayerSampler"); field.setAccessible(true);
                GpuSampler sampler=(GpuSampler)field.get(mc.levelRenderer);
                check(sampler.getMinFilter()==FilterMode.LINEAR && sampler.getMagFilter()==FilterMode.LINEAR,"GPU sampler switches back to linear");
            });
            context.takeScreenshot("alpha4-world-clouds-off");
            System.out.println("LUMINA_QUALITY_WORLD_OK clouds gpuSampler fluidCulling fluidShaping entitySorting");
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }

    private static void verifyFluidPolicies(Minecraft mc) throws Exception {
        var renderer=new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
        var top=Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true).setValue(BlockStateProperties.SLAB_TYPE,SlabType.TOP);
        var bottom=top.setValue(BlockStateProperties.SLAB_TYPE,SlabType.BOTTOM);
        int original=count(renderer,new Fixture(mc,Map.of(BlockPos.ZERO,top)),top,false);
        int optimized=count(renderer,new Fixture(mc,Map.of(BlockPos.ZERO,top)),top,true);
        check(optimized<original,"Hidden top fluid geometry removed: "+original+" -> "+optimized);
        int bottomOriginal=count(renderer,new Fixture(mc,Map.of(BlockPos.ZERO,bottom)),bottom,false);
        int bottomOptimized=count(renderer,new Fixture(mc,Map.of(BlockPos.ZERO,bottom)),bottom,true);
        check(bottomOriginal==bottomOptimized,"Visible bottom-slab fluid top preserved");
        Method average=FluidRenderer.class.getDeclaredMethod("calculateAverageHeight",BlockAndTintGetter.class,Fluid.class,float.class,float.class,float.class,BlockPos.class);
        average.setAccessible(true);
        var fixture=new Fixture(mc,Map.of(BlockPos.ZERO,bottom));
        NovaQualitySettings.apply(NovaQualityConfig.DEFAULT);
        float vanilla=(float)average.invoke(renderer,fixture,net.minecraft.world.level.material.Fluids.WATER,0.5f,0.2f,0.2f,BlockPos.ZERO);
        NovaQualitySettings.apply(new NovaQualityConfig(false,true,true,true));
        float alternative=(float)average.invoke(renderer,fixture,net.minecraft.world.level.material.Fluids.WATER,0.5f,0.2f,0.2f,BlockPos.ZERO);
        check(alternative>vanilla && alternative<=1,"Alternative shaping changes real fluid heights");
    }
    private static int count(FluidRenderer renderer,Fixture fixture,BlockState state,boolean cull) {
        NovaQualitySettings.apply(new NovaQualityConfig(false,cull,false,false));
        var vertices=new Counter();
        renderer.tesselate(fixture,BlockPos.ZERO,layer -> vertices,state,state.getFluidState());
        return vertices.count;
    }
    private static void verifyEntityPolicy(Minecraft mc) {
        var camera=mc.gameRenderer.mainCamera().position();
        var entity=new EntityRenderState();entity.x=camera.x+3;entity.y=camera.y-1;entity.z=camera.z;
        entity.boundingBoxWidth=2;entity.boundingBoxHeight=2;
        var pose=new PoseStack();pose.translate(10,0,0);
        var submit=new ModelFeatureRenderer.Submit<>(null,pose.last(),null,entity,0,0,-1,null,null);
        NovaQualitySettings.apply(NovaQualityConfig.DEFAULT);
        check(submit.distanceToCameraSq()==100,"Default entity sorting uses original model origin");
        NovaQualitySettings.apply(new NovaQualityConfig(false,true,true,true));
        check(Math.abs(submit.distanceToCameraSq()-4)<0.001,"Enhanced entity sorting uses closest bounding surface");
    }
    private static void check(boolean success,String message) { if (!success) throw new AssertionError(message); }

    private record Fixture(Minecraft minecraft,Map<BlockPos,BlockState> blocks) implements BlockAndTintGetter {
        @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos,Blocks.AIR.defaultBlockState()); }
        @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
        @Override public LevelLightEngine getLightEngine() { return minecraft.level.getLightEngine(); }
        @Override public CardinalLighting cardinalLighting() { return CardinalLighting.DEFAULT; }
        @Override public int getBlockTint(BlockPos pos,ColorResolver resolver) { return 0xFF4488FF; }
        @Override public int getHeight() { return 384; }
        @Override public int getMinY() { return -64; }
    }
    private static final class Counter implements VertexConsumer {
        int count;
        @Override public VertexConsumer addVertex(float x,float y,float z) { count++;return this; }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) { return this; }
        @Override public VertexConsumer setColor(int color) { return this; }
        @Override public VertexConsumer setUv(float u,float v) { return this; }
        @Override public VertexConsumer setUv1(int u,int v) { return this; }
        @Override public VertexConsumer setUv2(int u,int v) { return this; }
        @Override public VertexConsumer setUv3(float u,float v) { return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    }
}

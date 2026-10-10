package io.github.benjamincalledur10.luminanova.test;

import io.github.benjamincalledur10.luminanova.compat.NovaRenderCompatibility;
import io.github.benjamincalledur10.luminanova.config.NovaPerformanceConfig;
import io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Controlled real-world A/B pilot; both modes keep identical graphics settings and the same world. */
public final class PerformanceWorldTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("luminanova.performanceWorld")) return;
        int spacing=Integer.getInteger("luminanova.benchmark.spacing",3);
        if (spacing!=2 && spacing!=3) throw new IllegalArgumentException("Spacing must be 2 or 3");
        int layers=spacing==2?7:4;
        int chestCount=(60/spacing)*(60/spacing)*layers;
        int fixtureY=65+3*spacing;
        BlockPos fixture=new BlockPos(0,fixtureY,0);
        try (var world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            System.out.println("LUMINA_PERFORMANCE_SETUP initial chunks ready");
            world.getServer().runCommand("gamemode spectator @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 0 82 -40 0 30");
            // Both fixture densities fit y=64..79 sections with visible outer surfaces.
            for (int layer=0;layer<layers;layer++) {
                int y=65+layer*spacing;
                world.getServer().runCommand("fill -31 "+(y-1)+" -31 29 "+(y+1)+" 29 stone");
            }
            System.out.println("LUMINA_PERFORMANCE_SETUP stone ready");
            world.getServer().runOnServer(server -> {
                var level=server.overworld();
                for (int layer=0;layer<layers;layer++) for (int x=-30;x<30;x+=spacing) for (int z=-30;z<30;z+=spacing) {
                    level.setBlock(new BlockPos(x,65+layer*spacing,z),Blocks.CHEST.defaultBlockState(),3);
                }
                level.setBlock(new BlockPos(0,82,-46),Blocks.CHEST.defaultBlockState(),3);
            });
            System.out.println("LUMINA_PERFORMANCE_SETUP chests ready");
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(mc -> {
                mc.options.renderDistance().set(6);
                mc.options.simulationDistance().set(5);
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(260);
                mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
                mc.options.pauseOnLostFocus=false;
                mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                NovaPerformanceSettings.apply(NovaPerformanceConfig.DEFAULT);
            });
            waitForSceneSections(context);
            context.waitTicks(80);
            context.runOnClient(mc -> NovaPerformanceSettings.apply(NovaPerformanceConfig.DEFAULT));
            context.takeScreenshot("alpha5-enclosed-baseline");
            boolean available=NovaRenderCompatibility.blockEntityOwner().isEmpty();
            if (available) {
                context.runOnClient(mc -> {
                    System.out.println("LUMINA_PERFORMANCE_CAMERA "+mc.gameRenderer.mainCamera().position());
                    check(extract(mc,fixture),"Baseline enclosed chest is extracted");
                    NovaPerformanceSettings.apply(new NovaPerformanceConfig(true));
                    check(!extract(mc,fixture),"Enclosed chest skips extraction");
                    check(!extract(mc,new BlockPos(0,82,-46)),"Off-camera chest skips extraction");
                    check(!io.github.benjamincalledur10.luminanova.render.BlockEntityVisibility.enclosed(
                            mc.level,fixture,new net.minecraft.world.phys.Vec3(.5,fixtureY+.5,.5),
                            new BlockPos.MutableBlockPos()),"Camera inside enclosed chest retains rendering");
                    var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)mc.level.getBlockEntity(fixture);
                    chest.triggerEvent(1,1);
                    for (int tick=0;tick<5;tick++) net.minecraft.world.level.block.entity.ChestBlockEntity.lidAnimateTick(mc.level,chest.getBlockPos(),chest.getBlockState(),chest);
                    check(chest.getOpenNess(0)>0 && extract(mc,chest.getBlockPos()),"Animated open lid retains rendering");
                    chest.triggerEvent(1,0);
                    for (int tick=0;tick<20;tick++) net.minecraft.world.level.block.entity.ChestBlockEntity.lidAnimateTick(mc.level,chest.getBlockPos(),chest.getBlockState(),chest);
                });
            }
            Path directory=context.computeOnClient(mc -> mc.gameDirectory.toPath().resolve("benchmark-results"));
            ArrayList<String> results=new ArrayList<>();
            int runs=Integer.getInteger("luminanova.benchmark.runs",5);
            for (int run=0;run<runs;run++) {
                for (int phase=0;phase<2;phase++) {
                    boolean enabled=(run+phase)%2==1;
                    String title="Prueba Lumina Nova "+(run+1)+"/"+runs+" — no cerrar; cierre automático";
                    context.runOnClient(mc -> {
                        mc.getWindow().setTitle(title);
                        NovaPerformanceSettings.apply(new NovaPerformanceConfig(enabled));
                    });
                    context.waitTicks(100);
                    context.runOnClient(mc -> FrameSamples.start());
                    waitSeconds(context,Integer.getInteger("luminanova.benchmark.seconds",15));
                    String name="enclosed-"+(enabled?"on":"off")+"-"+run;
                    String result=context.computeOnClient(mc -> FrameSamples.stop(directory,name));
                    results.add(result);System.out.println("LUMINA_FRAME_RESULT "+result);
                }
            }
            context.runOnClient(mc -> NovaPerformanceSettings.apply(new NovaPerformanceConfig(true)));
            context.waitTicks(5);
            context.takeScreenshot("alpha5-enclosed-optimized");
            // Opening any face must restore extraction immediately; glass and partial blocks are not occluders.
            world.getServer().runOnServer(server -> {
                for (int layer=0;layer<layers;layer++) {
                    int y=65+layer*spacing;
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                            "fill -31 "+(y-1)+" -31 29 "+(y+1)+" 29 air replace stone");
                }
                // Keep a visible section mesh beneath the now-exposed animated blocks.
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "fill -31 64 -31 29 64 29 stone");
                server.overworld().setBlock(fixture.above(),Blocks.GLASS.defaultBlockState(),3);
            });
            world.getConnection().waitForClientboundPackets();
            // Rebuild before checking the scene, including meshes that became empty.
            context.runOnClient(mc -> mc.levelExtractor.allChanged());
            waitForSceneSections(context);
            context.waitTicks(20);
            if (available) context.runOnClient(mc -> {
                NovaPerformanceSettings.apply(new NovaPerformanceConfig(true));
                check(extract(mc,fixture),"Glass opening restores chest extraction without cached visibility");
            });
            context.runOnClient(mc -> NovaPerformanceSettings.apply(NovaPerformanceConfig.DEFAULT));
            context.waitTicks(5);
            context.takeScreenshot("alpha5-exposed-baseline");
            for (int run=0;run<2;run++) for (int phase=0;phase<2;phase++) {
                boolean enabled=(run+phase)%2==1;
                context.runOnClient(mc -> {
                    mc.getWindow().setTitle("Prueba Lumina Nova: cofres visibles — no cerrar; cierre automático");
                    NovaPerformanceSettings.apply(new NovaPerformanceConfig(enabled));
                });
                context.waitTicks(40);
                context.runOnClient(mc -> FrameSamples.start());
                waitSeconds(context,5);
                String name="exposed-"+(enabled?"on":"off")+"-"+run;
                String result=context.computeOnClient(mc -> FrameSamples.stop(directory,name));
                results.add(result);System.out.println("LUMINA_FRAME_RESULT "+result);
            }
            context.runOnClient(mc -> NovaPerformanceSettings.apply(new NovaPerformanceConfig(true)));
            context.waitTicks(5);
            context.takeScreenshot("alpha5-exposed-optimized");
            String mods=FabricLoader.getInstance().getAllMods().stream()
                    .filter(mod -> !mod.getMetadata().getId().startsWith("fabric-"))
                    .map(mod -> "\""+mod.getMetadata().getId()+"\":\""+mod.getMetadata().getVersion().getFriendlyString()+"\"")
                    .sorted().collect(java.util.stream.Collectors.joining(","));
            String implementationHash;
            try (var bytes=getClass().getClassLoader().getResourceAsStream("io/github/benjamincalledur10/luminanova/mixin/BlockEntityVisibilityMixin.class")) {
                if (bytes==null) throw new IllegalStateException("Cannot fingerprint optimization");
                implementationHash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.readAllBytes()));
            }
            String metadata=context.computeOnClient(mc -> "\"framebuffer_width\":"+mc.getWindow().getWidth()
                    +",\"framebuffer_height\":"+mc.getWindow().getHeight()+",\"render_distance\":"+mc.options.renderDistance().get()
                    +",\"simulation_distance\":"+mc.options.simulationDistance().get()+",\"fov\":"+mc.options.fov().get());
            Files.writeString(directory.resolve("results.json"),"{\"scene\":\""+chestCount+" enclosed vanilla chests, then walls removed with floor retained\",\"spacing\":"+spacing+","
                    +"\"pilot\":true,\"vsync\":false,\"fps_limit\":\"unlimited\",\"culling_available\":"+available+","
                    +"\"implementation_sha256\":\""+implementationHash+"\",\"development\":"+FabricLoader.getInstance().isDevelopmentEnvironment()+","
                    +metadata+",\"mods\":{"+mods+"},\"samples\":["+String.join(",",results)+"]}\n");
            System.out.println("LUMINA_PERFORMANCE_WORLD_OK cullingAvailable="+available);
        } catch (Exception failure) { throw new RuntimeException(failure); }
        finally { NovaPerformanceSettings.apply(NovaPerformanceConfig.DEFAULT); }
    }

    private static boolean extract(Minecraft mc,BlockPos position) {
        var entity=mc.level.getBlockEntity(position);
        check(entity!=null,"Fixture block entity received: "+position);
        return mc.levelRenderer.blockEntityRenderDispatcher().tryExtractRenderState(entity,0,null,false)!=null;
    }
    private static void waitSeconds(ClientGameTestContext context,int seconds) {
        long end=System.nanoTime()+seconds*1_000_000_000L;
        context.waitFor(mc -> System.nanoTime()>=end,ClientGameTestContext.NO_TIMEOUT);
    }
    private static void waitForSceneSections(ClientGameTestContext context) {
        // A global compile queue is not a scene-readiness fence on every renderer.
        // Require the four central, visible scene sections to have compiled geometry.
        context.waitFor(mc -> {
            for (int x=-1;x<=0;x++) for (int z=-1;z<=0;z++) {
                if (FabricLoader.getInstance().isModLoaded("sodium")) {
                    if (!net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer.instance().isSectionReady(x,4,z)) return false;
                } else if (!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(x*16,64,z*16),0)) return false;
            }
            return true;
        });
    }
    private static void check(boolean success,String message) { if (!success) throw new AssertionError(message); }
}

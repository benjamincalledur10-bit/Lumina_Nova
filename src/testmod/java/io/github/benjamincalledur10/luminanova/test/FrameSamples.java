package io.github.benjamincalledur10.luminanova.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/** Test-only fixed buffer. No file writes, allocations, sorting or logs during measurement. */
public final class FrameSamples {
    private static final long[] INTERVALS=new long[262144];
    private static boolean recording;
    private static long previous;
    private static int count;
    private static long extracted;
    private static boolean invalid;
    private FrameSamples() { }
    public static void start() { previous=0;count=0;extracted=0;invalid=false;recording=true; }
    public static void extracted(boolean present) { if (recording && present) extracted++; }
    public static void frame(Minecraft minecraft, boolean render) {
        if (!recording) return;
        if (!render || minecraft.level==null || minecraft.isPaused() || minecraft.gui.screen()!=null
                || minecraft.getFramerateLimitTracker().getThrottleReason()
                    != com.mojang.blaze3d.platform.FramerateLimitTracker.FramerateThrottleReason.NONE) {
            invalid=true;
            return;
        }
        long now=System.nanoTime();
        if (previous!=0 && count<INTERVALS.length) INTERVALS[count++]=now-previous;
        previous=now;
    }
    public static String stop(Path directory,String name) throws Exception {
        recording=false;
        if (invalid) throw new AssertionError("Measurement invalidated by pause, menu, inactivity or window throttling");
        if (count<100) throw new AssertionError("Insufficient frames: "+count);
        long[] sorted=Arrays.copyOf(INTERVALS,count); Arrays.sort(sorted);
        long sum=0; int over50=0,over100=0;
        StringBuilder csv=new StringBuilder("frame,interval_ns\n");
        for (int i=0;i<count;i++) {
            long interval=INTERVALS[i];sum+=interval;
            if (interval>50_000_000L) over50++;
            if (interval>100_000_000L) over100++;
            csv.append(i).append(',').append(interval).append('\n');
        }
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(name+".csv"),csv);
        return String.format(Locale.ROOT,
                "{\"name\":\"%s\",\"frames\":%d,\"seconds\":%.6f,\"fps\":%.6f,\"median_ms\":%.6f,\"p95_ms\":%.6f,\"p99_ms\":%.6f,\"over_50ms\":%d,\"over_100ms\":%d,\"extracted_per_frame\":%.6f}",
                name,count,sum/1e9,count*1e9/sum,sorted[(count-1)/2]/1e6,
                sorted[(int)Math.ceil(count*.95)-1]/1e6,sorted[(int)Math.ceil(count*.99)-1]/1e6,
                over50,over100,(double)extracted/(count+1));
    }
}

package com.ghostchu.plugins.riaeew.eew;

import com.ghostchu.plugins.riaeew.RIAEEW;
import com.ghostchu.plugins.riaeew.eew.datasource.DataSource;
import com.ghostchu.plugins.riaeew.eew.datasource.EarthQuakeInfoBase;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EEWUpdater implements Runnable {
    private final RIAEEW plugin;
    private final DataSource dataSource;
    private long pointer = -1;
    private final Cache<String, Boolean> DUPLICATE_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public EEWUpdater(RIAEEW plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        initPointer();
        plugin.getServer().getScheduler().buildTask(plugin, this).repeat(5, TimeUnit.SECONDS).schedule();
    }

    private void initPointer() {
        List<EarthQuakeInfoBase> infos = dataSource.getEarthQuakeList(-1).join();
        long max = -1;
        List<Long> longs = infos.stream().map(EarthQuakeInfoBase::getStartAt).collect(Collectors.toList());
        for (Long aLong : longs) {
            max = Math.max(max, aLong);
        }
        pointer = max;
        plugin.getLogger().info("Initial pointer value to " + pointer);
    }


    @Override
    public void run() {
        discoverEarthQuakeEvents();
        updateRunningTasks();
    }

    private void updateRunningTasks() {
        for (Map.Entry<String, EEWTraceTask> entry : plugin.getTraceTask().entrySet()) {
            EEWTraceTask task = entry.getValue();
            dataSource.getEarthQuakeInfo(entry.getKey()).thenAccept(list -> {
                if (list == null || list.isEmpty()) return;
                EarthQuakeInfoBase latestBase = list.get(list.size() - 1);
                if (latestBase.getUpdates() != task.getBase().getUpdates()) {
                    task.update(latestBase);
                    plugin.getLogger().info("更新报： " + task.getBase() + " -> " + latestBase);
                    pointer = latestBase.getStartAt();
                    plugin.getLogger().info("Updated pointer value to " + pointer);
                } else {
                    plugin.getLogger().info("忽略更新报：事件已结束或不存在");
                }
            }).exceptionally(th -> {
                plugin.getLogger().error("无法取得指定地震的更新报", th);
                return null;
            });
        }
    }

    private void discoverEarthQuakeEvents() {
        CompletableFuture<List<EarthQuakeInfoBase>> future = dataSource.getEarthQuakeList(pointer);
        future.thenAccept(list -> {
            if (list == null || list.isEmpty()) return;
            for (EarthQuakeInfoBase base : list) {
                if (plugin.getTraceTask().containsKey(base.getId())) {
                    plugin.getLogger().info("Skipped eventId " + base.getId() + ": already in tasks");
                    continue;
                }
                if (DUPLICATE_CACHE.asMap().containsKey(base.getId())) {
                    plugin.getLogger().info("Skipped eventId " + base.getId() + ": duplicated");
                    continue;
                }
                EEWTraceTask traceTask = new EEWTraceTask(plugin, base);
                plugin.getTraceTask().put(base.getId(), traceTask);
                DUPLICATE_CACHE.put(base.getId(), true);
                traceTask.start();
                plugin.getLogger().info("地震预警启动 " + base);
                pointer = traceTask.getBase().getStartAt();
                plugin.getLogger().info("Updated pointer value to " + pointer);
            }
        }).exceptionally(th -> {
            plugin.getLogger().error("无法取得最新地震列表", th);
            return null;
        });
    }
}

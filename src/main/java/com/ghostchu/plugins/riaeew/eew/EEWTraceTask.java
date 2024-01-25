package com.ghostchu.plugins.riaeew.eew;

import com.ghostchu.plugins.riaeew.RIAEEW;
import com.ghostchu.plugins.riaeew.eew.datasource.EarthQuakeInfoBase;
import com.ghostchu.plugins.riaeew.geoip.GeoIPResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.SoundCategory;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.Sound;
import dev.simplix.protocolize.data.packets.SoundEffect;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.yaml.snakeyaml.util.UriEncoder;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EEWTraceTask implements Runnable {
    private final RIAEEW plugin;
    private final boolean protocolizeInstalled;
    @Getter
    public EarthQuakeInfoBase base;
    //@Getter
    //private List<Player> players;
    @Getter
    private final Set<Player> warnedWithCountDown = new HashSet<>();
    @Getter
    private final Map<UUID, Long> eewTime = new HashMap<>();
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    @Getter
    private boolean stopped = false;
    private ScheduledTask task;
    private long startAt = -1;

    public EEWTraceTask(RIAEEW plugin, EarthQuakeInfoBase base) {
        this.base = base;
        this.plugin = plugin;
        this.protocolizeInstalled = plugin.getServer().getPluginManager().getPlugin("protocolize").isPresent();
        plugin.log("创建新的地震追踪任务 "+base.toString());
    }

    public void start() {
        if(MixUtil.isOutOfChina(base.getLongitude(), base.getLatitude())){
            plugin.getLogger().info("忽略 "+base+" 的地震信息：处于中国境外");
            plugin.log("忽略：处于中国境外： "+base.toString());
            stop();
            return;
        }
        broadcastGlobal();
        this.startAt = System.currentTimeMillis();
        //this.players = plugin.getGeoIPResultMap().keySet().stream().map(u -> plugin.getServer().getPlayer(u))
        //        .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        this.task = plugin.getServer().getScheduler().buildTask(plugin, this).repeat(1, TimeUnit.SECONDS).schedule();
    }

    public void stop() {
        stopped = true;
        if(this.task != null && this.task.status() == TaskStatus.SCHEDULED) {
            this.task.cancel();
        }
        if (!warnedWithCountDown.isEmpty()) {
            Component locationComponent = Component
                    .text(base.getLocation()).style(Style.style(NamedTextColor.GOLD, TextDecoration.UNDERLINED))
                    .hoverEvent(HoverEvent.showText(Component.text("点击在高德地图网页版中查看位置")))
                    .clickEvent(amapClickEvent(base.getLocation(), base.getLongitude(), base.getLatitude(),
                            -1, -1));
            plugin.getServer().getAllPlayers().forEach(p -> plugin.text().of(p, "broadcast-global-ended", locationComponent, warnedWithCountDown.size()).send());
        }
        plugin.getTraceTask().remove(base.getId());
    }

    public void update(EarthQuakeInfoBase base) {
        if(MixUtil.isOutOfChina(base.getLongitude(), base.getLatitude())){
            plugin.getLogger().info("忽略 "+base+" 的地震信息：处于中国境外");
            plugin.log("忽略：处于中国境外： "+ base);
            stop();
            return;
        }
        if (stopped) {
            return;
        }
        this.base = base;
        broadcastGlobal();
    }

    public void broadcastGlobal() {
        plugin.log("全局广播： "+base.toString());
        double intensity = HuaniaEarthQuakeCalculator.getIntensity(base.getMagnitude(), 0.0001d);
        IntensityLevel level = IntensityLevel.mapValue(intensity);
        //Component intensityTitle = plugin.text().of("intensity." + level.name() + ".title").component();
        Component intensityDescription = plugin.text().of("intensity." + level.name() + ".description").component();
        for (Player player : plugin.getServer().getAllPlayers()) {
            if (canBroadcastPlayer(player)) {
                plugin.log("广播给 "+player.getUsername()+"： "+base.toString());
                GeoIPResult geo = plugin.getGeoIPResultMap().get(player.getUniqueId());
                double myLongitude = -1;
                double myLatitude = -1;
                String geoIp = "定位失败，IP 库不存在您的信息";
                if (geo != null) {
                    myLongitude = geo.getLongitude();
                    myLatitude = geo.getLatitude();
                    geoIp = geo.getCountry()+", "+geo.getRegion()+", "+geo.getCity() + " - 经度："+geo.getLongitude()+", 维度："+geo.getLatitude();
                }
                Component locationComponent = Component
                        .text(base.getLocation()).style(Style.style(NamedTextColor.GOLD, TextDecoration.UNDERLINED))
                        .hoverEvent(HoverEvent.showText(Component.text("点击在高德地图网页版中查看位置")))
                        .clickEvent(amapClickEvent(base.getLocation(), base.getLongitude(), base.getLatitude(),
                                myLongitude, myLatitude));
                plugin.text().of(player, "broadcast-global", base.getUpdates(), locationComponent, base.getLatitude(),
                        base.getLongitude(), colorMagnitude(base.getMagnitude()), thin(base.getDepth()),
                        colorIntensity(intensity), intensityDescription,
                        format.format(new Date(base.getStartAt())), format.format(new Date(base.getUpdateAt())),
                        officialPublisherComponent(),
                        plugin.getDataSource().getName(),geoIp).send();
            }
        }
    }

    private Component officialPublisherComponent() {
        return Component.text("@中国地震台网速报")
                .style(Style.style(NamedTextColor.AQUA, TextDecoration.UNDERLINED))
                .clickEvent(ClickEvent.openUrl("https://www.weibo.com/u/1904228041"))
                .hoverEvent(HoverEvent.showText(Component.text("点击打开 @中国地震台网速报 的官方新浪微博账号")));
    }

    @Override
    public void run() {
        if(MixUtil.isOutOfChina(base.getLongitude(), base.getLatitude())){
            return;
        }
        double maxSeconds = 0.0d;
        for (Player player : plugin.getGeoIPResultMap().keySet().stream().map(u -> plugin.getServer().getPlayer(u))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())) {
            GeoIPResult geo = plugin.getGeoIPResultMap().get(player.getUniqueId());
            if (geo == null) continue;
            double playerDistance = HuaniaEarthQuakeCalculator.getDistance(base.getLatitude(), base.getLongitude(),
                    geo.getLatitude(), geo.getLongitude());
            double playerIntensity = HuaniaEarthQuakeCalculator.getIntensity(base.getMagnitude(), playerDistance);
            Instant startAt = Instant.ofEpochMilli(base.getStartAt());
            int passTimeSeconds = (int) HuaniaEarthQuakeCalculator.getCountDownSeconds(base.getDepth(), playerDistance);
            Instant reachedAt = startAt.plus(passTimeSeconds, ChronoUnit.SECONDS);
            long remainsSeconds = ChronoUnit.SECONDS.between(Instant.now(), reachedAt);
            if (canWarnPlayer(player, playerDistance, playerIntensity, remainsSeconds)) {
                maxSeconds = Math.max(maxSeconds, remainsSeconds);
                warnedWithCountDown.add(player);
                warnPlayer(player, base, geo, playerDistance, playerIntensity, remainsSeconds);
            }
        }
        if (maxSeconds <= 0.0d && (System.currentTimeMillis() - startAt) > 120 * 1000) {
            plugin.log("生命周期结束： "+base.toString());
            stop();
        }
    }

    private void warnPlayer(Player player, EarthQuakeInfoBase base, GeoIPResult geo, double playerDistance, double playerIntensity,
                            long countdownSeconds) {
        IntensityLevel intensityLevel = IntensityLevel.mapValue(playerIntensity);
        // Component intensityTitle = plugin.text().of("intensity." + intensityLevel.name() + ".title").component();
        Component intensityDescription = plugin.text().of("intensity." + intensityLevel.name() + ".description").component();
        Component line1 = plugin.text().of("countdown-titles.title", countdownSeconds).component();
        Component line2 = plugin.text().of("countdown-titles.subtitle", colorIntensity(playerIntensity), thin(playerDistance)).component();
        Component endedLine1 = plugin.text().of("countdown-titles.title-ended", countdownSeconds).component();
        Component endedLine2 = plugin.text().of("countdown-titles.subtitle-ended", base.getLocation(),
                colorMagnitude(base.getMagnitude()), thin(playerDistance)).component();
        eewTime.putIfAbsent(player.getUniqueId(), countdownSeconds);

        if (countdownSeconds % 2 == 0) {
            line1 = line1.color(NamedTextColor.RED);
        } else {
            line1 = line1.color(NamedTextColor.YELLOW);
        }

        if (countdownSeconds > 0) {
            player.showTitle(Title.title(line1, line2, Title.Times.times(Duration.ZERO, Duration.of(5, ChronoUnit.SECONDS),
                    Duration.ZERO)));
            player.sendActionBar(plugin.text().of("broadcast-affected-actionbar", colorIntensity(playerIntensity),
                    intensityDescription).component());
            plugin.getLogger().info("[实时计时] 距离地震横波到达 " + player.getUsername() + " 的位置，剩余 " + countdownSeconds + " 秒");
            plugin.log("[实时计时] 距离地震横波到达 " + player.getUsername() + " 的位置，剩余 " + countdownSeconds + " 秒");
            if (countdownSeconds >= 60) {
                playSound(player.getUniqueId(), new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0, 0, 0, Float.MAX_VALUE, 1.0f));
            } else if (countdownSeconds >= 30) {
                playSound(player.getUniqueId(), new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0, 0, 0, Float.MAX_VALUE, 0.5f));
            } else {
                playSound(player.getUniqueId(), new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0, 0, 0, Float.MAX_VALUE, 0.0f));
            }
        } else {
            Component locationComponent = Component
                    .text(base.getLocation()).style(Style.style(NamedTextColor.GOLD, TextDecoration.UNDERLINED))
                    .hoverEvent(HoverEvent.showText(Component.text("点击在高德地图网页版中查看位置")))
                    .clickEvent(amapClickEvent(base.getLocation(), base.getLongitude(), base.getLatitude(), geo.getLongitude(),
                            geo.getLatitude()));
            player.showTitle(Title.title(endedLine1, endedLine2, Title.Times.times(Duration.ZERO,
                    Duration.of(3, ChronoUnit.SECONDS), Duration.ZERO)));
            plugin.text().of(player, "broadcast-affected-ended", locationComponent,
                    thin(base.getLongitude()), thin(base.getLatitude()),
                    colorMagnitude(base.getMagnitude()),
                    thin(base.getDepth()),
                    colorIntensity(playerIntensity),
                    intensityDescription,
                    format.format(new Date(base.getStartAt())),
                    format.format(new Date(base.getUpdateAt())),
                    eewTime.getOrDefault(player.getUniqueId(), -1L),
                    officialPublisherComponent(), plugin.getDataSource().getName(), format.format(base.getStartAt())).send();
            plugin.getLogger().info("[预警结束] 为 " + player.getUsername() + " 提前预警了 " +
                    eewTime.getOrDefault(player.getUniqueId(), -1L) + " 秒");
            plugin.log("[预警结束] 为 " + player.getUsername() + " 提前预警了 " +
                    eewTime.getOrDefault(player.getUniqueId(), -1L) + " 秒");
            playSound(player.getUniqueId(), new SoundEffect(Sound.ENTITY_WITHER_DEATH, SoundCategory.MASTER, 0, 0, 0, Float.MAX_VALUE, 1.0f));
        }
    }

    private ClickEvent amapClickEvent(String position, double longitude, double latitude, double myLongitude, double myLatitude) {
        String url = "https://uri.amap.com/marker?markers=";
        String args = longitude + "," + latitude + ",震中：" + position;
        if (myLongitude != -1 && myLatitude != -1) {
            args += "|" + myLongitude + "," + myLatitude + ",我的位置";
        }
        url = url + UriEncoder.encode(args);
        return ClickEvent.openUrl(url);
    }

    private void playSound(UUID uuid, SoundEffect soundEffect) {
        if (!protocolizeInstalled) return;
        ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
        player.sendPacket(soundEffect);
    }

    private Component colorMagnitude(double magnitude) {
        MagnitudeLevel magnitudeLevel = MagnitudeLevel.mapValue(magnitude);
        return Component.text(thin(magnitude)).color(magnitudeLevel.getColor());
    }

    private Component colorIntensity(double intensity) {
        IntensityLevel intensityLevel = IntensityLevel.mapValue(intensity);
        return Component.text(thin(intensity)).color(intensityLevel.getColor());
    }

    private static String thin(double s) {
        return String.format("%.1f", s);
    }

    private boolean canBroadcastPlayer(Player player) {
        return base.getMagnitude() >= 5d;
    }

    private boolean canWarnPlayer(Player player, double playerDistance, double playerIntensity, long countdownSeconds) {
        if (playerIntensity < 3) {
            return false;
        }
        return countdownSeconds <= 1200 && countdownSeconds >= 0;
    }
}

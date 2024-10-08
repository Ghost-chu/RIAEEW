package com.ghostchu.plugins.riaeew;

import com.ghostchu.plugins.riaeew.eew.EEWTraceTask;
import com.ghostchu.plugins.riaeew.eew.EEWUpdater;
import com.ghostchu.plugins.riaeew.eew.datasource.DataSource;
import com.ghostchu.plugins.riaeew.eew.datasource.impl.WolfxJP;
import com.ghostchu.plugins.riaeew.eew.datasource.impl.WolfxJPMock;
import com.ghostchu.plugins.riaeew.geoip.GeoIPResult;
import com.ghostchu.plugins.riaeew.geoip.impl.IP2LocationImpl;
import com.ghostchu.plugins.riaeew.text.TextManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import kong.unirest.Unirest;
import lombok.Getter;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Plugin(
        id = "riaeew-velocity",
        name = "RIAEEW-Velocity",
        version = "1.0-SNAPSHOT",
        dependencies = {@Dependency(id = "protocolize", optional = true)}
)
public class RIAEEW {
    @Getter
    @Inject
    private Logger logger;
    @Getter
    @Inject
    private ProxyServer server;
    @Getter
    private final Map<UUID, GeoIPResult> geoIPResultMap = new ConcurrentSkipListMap<>();
    @Inject
    @DataDirectory
    private Path dataFolderPath;
    private File dataFolder;
    private IP2LocationImpl geoIPDatabase;
    private TextManager text;
    @Getter
    private DataSource dataSource;
    @Getter
    private EEWUpdater eewUpdater;
    @Getter
    private final Map<String, EEWTraceTask> traceTask = new ConcurrentHashMap<>();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.dataFolder = dataFolderPath.toFile();
        this.dataFolder.mkdirs();
        try {
            this.geoIPDatabase = new IP2LocationImpl(getLogger(),this.dataFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.text = new TextManager(this, new File(dataFolder, "messages.yml"));
        if (System.getProperty("riaeew.debug") != null) {
            this.dataSource = new WolfxJPMock();
            getLogger().info("插件目前运行在模拟数据源下");
        } else {
//            this.dataSource = new ChinaEEW(this);
//            getLogger().info("插件目前运行在ChinaEEW数据源下");
            this.dataSource = new WolfxJP();
        }
        getLogger().info("插件目前运行在 "+this.dataSource.getName()+" 数据源下");
        //
        this.eewUpdater = new EEWUpdater(this, this.dataSource);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        Unirest.shutDown();
        if(geoIPDatabase != null){
            geoIPDatabase.close();
        }
        logger.info("正在安全关闭 RIAEEW 使用的资源……");
    }

    public ConfigurationSection getConfig() {
        return null;
    }

    public TextManager text() {
        return this.text;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onConnected(LoginEvent event) {
        if (!event.getResult().isAllowed()) return;
        GeoIPResult result = geoIPDatabase.query(event.getPlayer().getRemoteAddress().getAddress());
        geoIPResultMap.put(event.getPlayer().getUniqueId(), result);
        logger.info("Located user " + event.getPlayer().getUsername() + " with IP address [" + event.getPlayer().getRemoteAddress().getAddress().getHostAddress() + "] to GeoLocation: " + result.toString());
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnected(DisconnectEvent event) {
        geoIPResultMap.remove(event.getPlayer().getUniqueId());
    }

    public void log(String content){
        CompletableFuture.supplyAsync(()->{
           writeLog(content);
           return null;
        });
    }
    private void writeLog(String content){
        synchronized (this) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                StringBuilder builder = new StringBuilder();
                builder.append("[").append(format.format(new Date())).append("] ");
                builder.append(content);
                builder.append("\n");
                File logFile = new File(dataFolder, "log.txt");
                if (!logFile.exists()) {
                    try {
                        logFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Files.writeString(logFile.toPath(), builder.toString(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }catch (Throwable th){
                // 禁止日志工具导致流程停止
                th.printStackTrace();
            }
        }
    }
}

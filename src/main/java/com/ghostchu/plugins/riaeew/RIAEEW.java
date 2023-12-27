package com.ghostchu.plugins.riaeew;

import com.ghostchu.plugins.riaeew.eew.EEWTraceTask;
import com.ghostchu.plugins.riaeew.eew.EEWUpdater;
import com.ghostchu.plugins.riaeew.eew.datasource.DataSource;
import com.ghostchu.plugins.riaeew.eew.datasource.impl.ChinaEEW;
import com.ghostchu.plugins.riaeew.eew.datasource.impl.ChinaEEWLocalMock;
import com.ghostchu.plugins.riaeew.geoip.GeoIPResult;
import com.ghostchu.plugins.riaeew.geoip.impl.IP2LocationImpl;
import com.ghostchu.plugins.riaeew.text.TextManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
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
            this.geoIPDatabase = new IP2LocationImpl(this.dataFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.text = new TextManager(this, new File(dataFolder, "messages.yml"));
        if (System.getProperty("riaeew.debug") != null) {
            this.dataSource = new ChinaEEWLocalMock();
            getLogger().info("插件目前运行在模拟数据源下");
        } else {
            this.dataSource = new ChinaEEW(this);
            getLogger().info("插件目前运行在ChinaEEW数据源下");
        }
        //
        this.eewUpdater = new EEWUpdater(this, this.dataSource);
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

}

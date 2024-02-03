package com.ghostchu.plugins.riaeew.geoip.impl;

import com.ghostchu.plugins.riaeew.geoip.GeoIPDatabase;
import com.ghostchu.plugins.riaeew.geoip.GeoIPResult;
import com.ip2location.IP2Location;
import com.ip2location.IPResult;
import lombok.SneakyThrows;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;

public class IP2LocationImpl extends FileAlterationListenerAdaptor implements GeoIPDatabase {
    private final IP2Location IPV4 = new IP2Location();
    private final IP2Location IPV6 = new IP2Location();
    private final Timer updateNotification = new Timer();
    private final Timer confirm = new Timer();
    private final long day30ms = 2592000L * 1000;
    private final Logger logger;
    private final File dataFolder;
    private File ipv4File;
    private File ipv6File;
    private File updateConfirmFile;
    private FileAlterationMonitor monitor;

    public IP2LocationImpl(Logger logger, File dataFolder) throws IOException {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.updateConfirmFile = new File(dataFolder,"confirm.txt");
        ipv4File = new File(dataFolder, "IP2LOCATION-IPV4.BIN");
        ipv6File = new File(dataFolder, "IP2LOCATION-IPV6.BIN");
        checkUpdatePatches();
        updateNotification.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkFileExpired();
            }
        }, 10 * 1000, 1800 * 1000);
        confirm.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(updateConfirmFile.exists()){
                    updateConfirmFile.delete();
                    checkUpdatePatches();
                }
            }
        }, 10 * 1000, 1800 * 1000);

    }

    @Override
    public void close() {
        if (monitor != null) {
            try {
                monitor.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateNotification.cancel();
    }

    private void checkUpdatePatches(){
        File updateIPV4 = new File(dataFolder, "IP2LOCATION-IPV4.BIN.UPDATE");
        File updateIPV6 = new File(dataFolder, "IP2LOCATION-IPV6.BIN.UPDATE");
        updateFiles(updateIPV4);
        updateFiles(updateIPV6);
    }

    private void openDatabases() throws IOException {
        IPV4.Open(new File(dataFolder, "IP2LOCATION-IPV4.BIN").getPath(), true);
        IPV6.Open(new File(dataFolder, "IP2LOCATION-IPV6.BIN").getPath(), true);
    }

    private void closeDatabases() {
        IPV4.Close();
        IPV6.Close();
    }

    private void updateFiles(File file) {
        try {
            File updateIPV4 = new File(dataFolder, "IP2LOCATION-IPV4.BIN.UPDATE");
            File updateIPV6 = new File(dataFolder, "IP2LOCATION-IPV6.BIN.UPDATE");

            if(!file.exists()){
                return;
            }

            if (!file.equals(updateIPV4) && !file.equals(updateIPV6)) {
                return;
            }
            try {
                IP2Location test = new IP2Location();
                test.Open(file.getPath());
                test.Close();
            } catch (Throwable e) {
                logger.warn("数据库更新文件 " + file.getName() + " 无效或者损坏。是否已上传完整？如果已上传完整，则可能是格式不兼容。", e);
            }
            if (file.equals(updateIPV4)) {
                closeDatabases();
                ipv4File.delete();
                Files.move(file.toPath(), ipv4File.toPath());
                openDatabases();
            }
            if (file.equals(updateIPV6)) {
                closeDatabases();
                ipv6File.delete();
                Files.move(file.toPath(), ipv6File.toPath());
                openDatabases();
            }
            logger.info("GeoIP 数据库文件 " + file.getName() + " 已完成更新");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    private void checkFileExpired() {
        boolean anyHit = false;
        if ((System.currentTimeMillis() - ipv4File.lastModified()) > day30ms) {
            anyHit = true;
            logger.warn("RIAEEW 的 GeoIP - IPV4 地址库已过期（超过 60 天未更新），" +
                    "无法为地震预警服务提供准确定位，请从 https://lite.ip2location.com/database-download " +
                    "下载 IPV4 地址库的 BIN 格式文件更新（IP-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE-ZIPCODE-TIMEZONE），请将新下载的文件解压缩，内部的 BIN 文件重命名为 IP2LOCATION-IPV4.BIN.UPDATE 并放置在数据目录下");
        }
        if ((System.currentTimeMillis() - ipv6File.lastModified()) > day30ms) {
            anyHit = true;
            logger.warn("RIAEEW 的 GeoIP - IPV6 地址库已过期（超过 60 天未更新），" +
                    "无法为地震预警服务提供准确定位，请从 https://lite.ip2location.com/database-download " +
                    "下载 IPV6 地址库的 BIN 格式文件更新（IP-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE-ZIPCODE-TIMEZONE），请将新下载的文件解压缩，内部的 BIN 文件重命名为 IP2LOCATION-IPV6.BIN.UPDATE 并放置在数据目录下");
        }
        if(anyHit){
            logger.warn("放置完成后，请在数据目录下创建 confirm.txt 启动应用更新程序，或者重载插件");
        }
    }

    @SneakyThrows
    @Override
    public GeoIPResult query(InetAddress address) {
        IP2Location loc;
        if (address.isLoopbackAddress()) {
            address = InetAddress.getByName("8.8.8.8");
        }
        if (address instanceof Inet4Address) {
            loc = IPV4;
        } else if (address instanceof Inet6Address) {
            loc = IPV6;
        } else {
            throw new IllegalArgumentException("Address " + address + " neither IPV4 or IPV6 address");
        }
        try {
            IPResult result = loc.IPQuery(address.getHostAddress());
            return new GeoIPResult(result.getCountryLong(), result.getRegion(), result.getCity(), result.getLatitude(), result.getLongitude());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

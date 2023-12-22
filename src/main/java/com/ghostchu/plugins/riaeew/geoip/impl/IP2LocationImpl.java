package com.ghostchu.plugins.riaeew.geoip.impl;

import com.ghostchu.plugins.riaeew.geoip.GeoIPDatabase;
import com.ghostchu.plugins.riaeew.geoip.GeoIPResult;
import com.ip2location.IP2Location;
import com.ip2location.IPResult;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public class IP2LocationImpl implements GeoIPDatabase {
    private final IP2Location IPV4 = new IP2Location();
    private final IP2Location IPV6 = new IP2Location();

    public IP2LocationImpl(File dataFolder) throws IOException {
        IPV4.Open(new File(dataFolder,"IP2LOCATION-IPV4.BIN").getPath(), true);
        IPV6.Open(new File(dataFolder,"IP2LOCATION-IPV6.BIN").getPath(), true);
    }

    @SneakyThrows
    @Override
    public GeoIPResult query(InetAddress address) {
        IP2Location loc;
        if(address.isLoopbackAddress()){
            loc = IPV4;
            address = InetAddress.getByName("119.179.4.206");
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

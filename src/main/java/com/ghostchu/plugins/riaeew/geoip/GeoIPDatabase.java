package com.ghostchu.plugins.riaeew.geoip;

import java.net.InetAddress;

public interface GeoIPDatabase {
    GeoIPResult query(InetAddress address);
}

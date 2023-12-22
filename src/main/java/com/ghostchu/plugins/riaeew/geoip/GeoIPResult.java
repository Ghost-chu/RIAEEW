package com.ghostchu.plugins.riaeew.geoip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class GeoIPResult {
    private String country;
    private String region;
    private String city;
    private double latitude;
    private double longitude;
}

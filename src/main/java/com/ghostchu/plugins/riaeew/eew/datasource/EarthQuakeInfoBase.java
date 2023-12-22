package com.ghostchu.plugins.riaeew.eew.datasource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class EarthQuakeInfoBase {
    public String id;
    private int updates;
    public long startAt;
    public long updateAt;
    public double latitude;
    public double longitude;
    public double magnitude;
    public double depth;
    public String location;
}

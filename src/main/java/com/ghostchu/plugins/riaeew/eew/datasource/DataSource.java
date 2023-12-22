package com.ghostchu.plugins.riaeew.eew.datasource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DataSource {
    CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeList(long startPointer);

    CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeInfo(String earthQuakeId);

    String getName();
}

package com.ghostchu.plugins.riaeew.eew.datasource.impl;

import com.ghostchu.plugins.riaeew.eew.datasource.DataSource;
import com.ghostchu.plugins.riaeew.eew.datasource.EarthQuakeInfoBase;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChinaEEWLocalMock implements DataSource {
    private final Gson GSON = new Gson();
    @Override
    public CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeList(long startPointer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EarthQuakeListResult result = GSON.fromJson(Files.readString(new File("D:\\test.list").toPath()), EarthQuakeListResult.class);
                if (result.getCode() != 0) {
                    throw new IllegalStateException("查询地震信息失败，服务器返回非正常响应: " );
                }
                return result.getData().stream().map(data -> new EarthQuakeInfoBase(String.valueOf(data.getEventId()), data.getUpdates(), data.getStartAt(), data.getUpdateAt(), data.getLatitude(), data.getLongitude(), data.getMagnitude(), data.getDepth(), data.getEpicenter())).collect(Collectors.toList());
            } catch (JsonSyntaxException | IOException e) {
                throw new IllegalStateException("查询地震信息失败，服务器返回非JSON响应: ",e);
            }
        });
    }

    @Override
    public CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeInfo(String earthQuakeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EarthQuakeListResult result = GSON.fromJson(Files.readString(new File("D:\\test.single").toPath()), EarthQuakeListResult.class);
                if (result.getCode() != 0) {
                    throw new IllegalStateException("查询地震信息失败，服务器返回非正常响应: ");
                }
                return result.getData().stream().map(data -> new EarthQuakeInfoBase(String.valueOf(data.getEventId()), data.getUpdates(), data.getStartAt(), data.getUpdateAt(), data.getLatitude(), data.getLongitude(), data.getMagnitude(), data.getDepth(), data.getEpicenter())).collect(Collectors.toList());
            } catch (JsonSyntaxException | IOException e) {
                throw new IllegalStateException("查询地震信息失败，服务器返回非JSON响应: ");
            }
        });
    }

    @Override
    public String getName() {
        return "成都高新减灾研究所";
    }

    @NoArgsConstructor
    @Data
    static class EarthQuakeListResult {
        @SerializedName("code")
        private Integer code;
        @SerializedName("message")
        private String message;
        @SerializedName("data")
        private List<DataDTO> data;

        @NoArgsConstructor
        @Data
        public static class DataDTO {
            @SerializedName("eventId")
            private Long eventId;
            @SerializedName("updates")
            private Integer updates;
            @SerializedName("latitude")
            private Double latitude;
            @SerializedName("longitude")
            private Double longitude;
            @SerializedName("depth")
            private Double depth;
            @SerializedName("epicenter")
            private String epicenter;
            @SerializedName("startAt")
            private Long startAt;
            @SerializedName("updateAt")
            private Long updateAt;
            @SerializedName("magnitude")
            private Double magnitude;
            @SerializedName("insideNet")
            private Integer insideNet;
            @SerializedName("sations")
            private Integer sations;
        }
    }
}

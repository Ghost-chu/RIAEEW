package com.ghostchu.plugins.riaeew.eew.datasource.impl;

import com.ghostchu.plugins.riaeew.eew.datasource.DataSource;
import com.ghostchu.plugins.riaeew.eew.datasource.EarthQuakeInfoBase;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChinaEEW implements DataSource {
    private final Gson GSON = new Gson();
    private final String huaniaApi = new String(Base64.getDecoder().decode("aHR0cHM6Ly9tb2JpbGUtbmV3LmNoaW5hZWV3LmNuL3YxLw=="), StandardCharsets.UTF_8);

    @Override
    public CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeList(long startPointer) {
        return CompletableFuture.supplyAsync(() -> {
            HttpResponse<String> resp = Unirest.get(huaniaApi + "earlywarnings?updates=3&start_at=" + startPointer)
                    .asString();
            if (!resp.isSuccess()) {
                throw new IllegalStateException("查询地震信息失败，HTTP请求错误: " + resp.getStatus() + " - " + resp.getStatusText() + ": " + resp.getBody());
            }
            try {
                EarthQuakeListResult result = GSON.fromJson(resp.getBody(), EarthQuakeListResult.class);
                if (result.getCode() != 0) {
                    throw new IllegalStateException("查询地震信息失败，服务器返回非正常响应: " + resp.getBody());
                }
                return result.getData().stream().map(data -> new EarthQuakeInfoBase(String.valueOf(data.getEventId()), data.getUpdates(), data.getStartAt(), data.getUpdateAt(), data.getLatitude(), data.getLongitude(), data.getMagnitude(), data.getDepth(), data.getEpicenter())).collect(Collectors.toList());
            } catch (JsonSyntaxException e) {
                throw new IllegalStateException("查询地震信息失败，服务器返回非JSON响应: " + resp.getBody());
            }
        });
    }

    @Override
    public CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeInfo(String earthQuakeId) {
        return CompletableFuture.supplyAsync(() -> {
            HttpResponse<String> resp = Unirest.get(huaniaApi + "earlywarnings/" + earthQuakeId)
                    .asString();
            if (!resp.isSuccess()) {
                throw new IllegalStateException("查询地震信息失败，HTTP请求错误: " + resp.getStatus() + " - " + resp.getStatusText() + ": " + resp.getBody());
            }
            try {
                EarthQuakeListResult result = GSON.fromJson(resp.getBody(), EarthQuakeListResult.class);
                if (result.getCode() != 0) {
                    throw new IllegalStateException("查询地震信息失败，服务器返回非正常响应: " + resp.getBody());
                }
                return result.getData().stream().map(data -> new EarthQuakeInfoBase(String.valueOf(data.getEventId()), data.getUpdates(), data.getStartAt(), data.getUpdateAt(), data.getLatitude(), data.getLongitude(), data.getMagnitude(), data.getDepth(), data.getEpicenter())).collect(Collectors.toList());
            } catch (JsonSyntaxException e) {
                throw new IllegalStateException("查询地震信息失败，服务器返回非JSON响应: " + resp.getBody());
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

package com.ghostchu.plugins.riaeew.eew.datasource.impl;

import com.ghostchu.plugins.riaeew.eew.datasource.DataSource;
import com.ghostchu.plugins.riaeew.eew.datasource.EarthQuakeInfoBase;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WolfxJP implements DataSource {
    private final String WOLFX_API = "https://api.wolfx.jp/cenc_eqlist.json";
    private static final Gson GSON = new Gson();
  //  private volatile String lastMD5 = "";

    @Override
    public CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeList(long startPointer) {
        return CompletableFuture.supplyAsync(() -> {
            HttpResponse<JsonNode> resp = Unirest.get(WOLFX_API)
                    .asJson();
            if (!resp.isSuccess()) {
                throw new IllegalStateException("查询地震信息失败，HTTP请求错误: " + resp.getStatus() + " - " + resp.getStatusText() + ": " + resp.getBody());
            }
            JSONObject node = resp.getBody().getObject();
//            String md5 = node.getString("md5");
//            if(md5.equalsIgnoreCase(lastMD5)){
//                return Collections.emptyList();
//            }
           // lastMD5 = md5;
            List<WolfxJsonNoBean> entries = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                if (!node.has("No" + i)) {
                    break;
                }
                JSONObject object = node.getJSONObject("No" + i);
                entries.add(GSON.fromJson(object.toString(), WolfxJsonNoBean.class));
            }
            return entries.stream().map(wolfx -> new EarthQuakeInfoBase(
                    wolfx.getLocation(),
                     1,
                     dateToMs(wolfx.getTime(), "yyyy-MM-dd HH:mm:ss"),
                     dateToMs(wolfx.getTime(), "yyyy-MM-dd HH:mm:ss"),
                     Double.parseDouble(wolfx.getLatitude()),
                     Double.parseDouble(wolfx.getLongitude()),
                     Double.parseDouble(wolfx.getMagnitude()),
                     Double.parseDouble(wolfx.getDepth()),
                     wolfx.getLocation()
             )).filter(eew->eew.getStartAt() > startPointer).collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<EarthQuakeInfoBase>> getEarthQuakeInfo(String earthQuakeId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public String getName() {
        return "Wolfx.jp";
    }
    public static long dateToMs(String _date,String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        try {
            Date date = format.parse(_date);
            return date.getTime();
        } catch (Exception e) {
            return 0;
        }
    }
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class WolfxJsonNoBean {

        @SerializedName("type")
        private String type;
        @SerializedName("time")
        private String time;
        @SerializedName("location")
        private String location;
        @SerializedName("magnitude")
        private String magnitude;
        @SerializedName("depth")
        private String depth;
        @SerializedName("latitude")
        private String latitude;
        @SerializedName("longitude")
        private String longitude;
    }
}

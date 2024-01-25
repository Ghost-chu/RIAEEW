package com.ghostchu.plugins.riaeew.eew;

public class MixUtil {
    private static final double radius = 6378.137d;
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static double distance(double lat1, double lon1, double lat2, double lon2){
        double dlat = Math.toRadians(lat2-lat1);
        double dlon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dlat/2) * Math.sin(dlat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = radius * c;
        return d;
    }

    public static boolean isOutOfChina(double longitude, double latitude){
        return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271;
    }


    
}


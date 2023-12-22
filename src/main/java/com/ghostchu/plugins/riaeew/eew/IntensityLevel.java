package com.ghostchu.plugins.riaeew.eew;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum IntensityLevel {
    NONE(NamedTextColor.WHITE),
    LIGHT(NamedTextColor.GREEN),
    MEDIUM(TextColor.fromHexString("#ECBA30")),
    HEAVY(TextColor.fromHexString("#EA7B08"));

    private final TextColor color;

    IntensityLevel(TextColor color){
        this.color = color;
    }

    public TextColor getColor(){
        return this.color;
    }
    public static IntensityLevel mapValue(double intensity) {
        if (intensity >= 1.0 && intensity < 2.0) {
            return LIGHT;
        } else if (intensity >= 2.0 && intensity < 4.0) {
            return MEDIUM;
        } else if (intensity >= 4.0) {
            return HEAVY;
        } else {
            return NONE;
        }
    }
}

package com.ghostchu.plugins.riaeew.eew;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum MagnitudeLevel {
    ONLY_RECORD(NamedTextColor.GRAY),
    NOTIFY(TextColor.fromHexString("#165DE4")),
    MODERATE(TextColor.fromHexString("#54CF5D")),
    WARNING(TextColor.fromHexString("#ECBA30")),
    ALERT(TextColor.fromHexString("#EA7B08")),
    DISASTER(NamedTextColor.DARK_RED);

    private final TextColor color;

    MagnitudeLevel(TextColor color) {
        this.color = color;
    }

    public TextColor getColor() {
        return color;
    }

    public static MagnitudeLevel mapValue(double magnitude) {
        if (magnitude < 2) {
            return ONLY_RECORD;
        } else if (magnitude < 5) {
            return NOTIFY;
        } else if (magnitude < 6) {
            return MODERATE;
        } else if (magnitude < 7) {
            return WARNING;
        } else if (magnitude < 8) {
            return ALERT;
        } else {
            return DISASTER;
        }
    }
}

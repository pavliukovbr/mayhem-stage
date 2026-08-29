package dev.pavliukovbr.mayhem;

import java.util.Map;

/**
 * Marcas de palco do vestido (coordenadas do mundo, ancoradas no palco em
 * 0,-59,0). O deck elevado pisa em -54; o chao atras do telao, em -60.
 */
public final class ShowMarks {
    public record Mark(double x, double y, double z, float yaw) {}

    public static final Map<String, Map<String, Mark>> PROPS = Map.of(
        "dress", Map.of(
            "backstage", new Mark(0.5, -60.0, 75.0, 180f),
            "portal",    new Mark(0.5, -54.0, 32.0, 180f),
            "mainstage", new Mark(0.5, -54.0, 8.0, 180f)
        )
    );

    private ShowMarks() {}
}

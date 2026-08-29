package dev.pavliukovbr.mayhem;

import java.util.Map;

/**
 * Marcas de palco (coordenadas do mundo, palco ancorado em 0,-59,0).
 * Deck elevado pisa em -54; chao atras do telao, -60; debaixo do palco, -75.
 */
public final class ShowMarks {
    public record Mark(double x, double y, double z, float yaw) {}

    /** O vestido e um grupo: duas metades que andam juntas e abrem no eixo X. */
    public static final Map<String, Mark> DRESS = Map.of(
        "backstage", new Mark(0.5, -60.0, 75.0, 180f),
        "portal",    new Mark(0.5, -54.0, 32.0, 180f),
        "mainstage", new Mark(0.5, -54.0, 15.0, 180f)
    );

    private ShowMarks() {}
}

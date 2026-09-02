package dev.pavliukovbr.mayhem.client;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;

import java.util.ArrayList;
import java.util.List;

/**
 * Iluminacao diferida por cima do frame do jogo: reconstruimos a posicao
 * de cada pixel a partir do depth buffer e somamos luzes coloridas de
 * verdade em TUDO: blocos, props, jogadores. As normais saem das
 * derivadas da propria posicao reconstruida.
 *
 * O depth do jogo e reversed-Z e a matriz do Frustum e classica, entao a
 * conversao e depth_classico_ndc = 1 - 2*depth_amostrado.
 */
public final class ShowLights {
    public record Light(double x, double y, double z, float r, float g, float b,
                        float intensity, float radius,
                        float dx, float dy, float dz, float cosOuter, float cosInner) {
        static Light point(double x, double y, double z, float r, float g, float b,
                           float it, float rad) {
            return new Light(x, y, z, r, g, b, it, rad, 0, -1, 0, -2f, -2f);
        }
        static Light spot(double x, double y, double z, float dx, float dy, float dz,
                          float r, float g, float b, float it, float rad,
                          float outerDeg, float innerDeg) {
            float L = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            return new Light(x, y, z, r, g, b, it, rad, dx/L, dy/L, dz/L,
                    (float) Math.cos(Math.toRadians(outerDeg)),
                    (float) Math.cos(Math.toRadians(innerDeg)));
        }
    }

    private static final int MAX = 16;
    private static final List<Light> LIGHTS = new ArrayList<>();
    public static float ambientScale = 1f;

    private static int program = -1, vao;
    private static int uInvVp, uCam, uCount, uPosRad, uColorInt, uDirCone, uInner, uDepth;

    public static void setPreset(String name) {
        LIGHTS.clear();
        switch (name) {
            case "red" -> {
                ambientScale = 0.35f;
                LIGHTS.add(Light.point(0.5, -20, 20, 1f, 0.06f, 0.10f, 2.4f, 95f));
                LIGHTS.add(Light.point(-45, -30, 5, 1f, 0.05f, 0.08f, 1.6f, 70f));
                LIGHTS.add(Light.point(46, -30, 5, 1f, 0.05f, 0.08f, 1.6f, 70f));
                LIGHTS.add(Light.point(0.5, -40, -95, 1f, 0.07f, 0.10f, 1.8f, 85f));
            }
            case "white" -> {
                ambientScale = 0.55f;
                LIGHTS.add(Light.spot(-40, 2, 40, 0.35f, -1f, -0.55f, 1f, 0.97f, 0.9f, 3.2f, 120f, 24f, 10f));
                LIGHTS.add(Light.spot(41, 2, 40, -0.35f, -1f, -0.55f, 1f, 0.97f, 0.9f, 3.2f, 120f, 24f, 10f));
                LIGHTS.add(Light.spot(0.5, 2, 45, 0f, -1f, -0.7f, 1f, 0.97f, 0.9f, 3.5f, 130f, 20f, 8f));
            }
            case "blue" -> {
                ambientScale = 0.4f;
                LIGHTS.add(Light.point(0.5, -20, 20, 0.15f, 0.25f, 1f, 2.2f, 95f));
                LIGHTS.add(Light.point(0.5, -40, -95, 0.2f, 0.3f, 1f, 1.6f, 85f));
            }
            default -> ambientScale = 1f;   // off
        }
    }

    /** Luzes extras por frame (ex.: brilho do telao na cor do video). */
    public static final List<Light> DYNAMIC = new ArrayList<>();

    static void render(Matrix4f frustum, double camX, double camY, double camZ,
                       int depthTexId) {
        List<Light> all = new ArrayList<>(LIGHTS);
        all.addAll(DYNAMIC);
        if (all.isEmpty()) return;
        if (program == -1) init();

        Matrix4f inv = new Matrix4f(frustum).invert();
        float[] m = new float[16];
        inv.get(m);

        int n = Math.min(all.size(), MAX);
        float[] posRad = new float[n * 4], colInt = new float[n * 4];
        float[] dirCone = new float[n * 4], inner = new float[n];
        for (int i = 0; i < n; i++) {
            Light l = all.get(i);
            posRad[i*4] = (float) (l.x() - camX); posRad[i*4+1] = (float) (l.y() - camY);
            posRad[i*4+2] = (float) (l.z() - camZ); posRad[i*4+3] = l.radius();
            colInt[i*4] = l.r(); colInt[i*4+1] = l.g(); colInt[i*4+2] = l.b();
            colInt[i*4+3] = l.intensity();
            dirCone[i*4] = l.dx(); dirCone[i*4+1] = l.dy(); dirCone[i*4+2] = l.dz();
            dirCone[i*4+3] = l.cosOuter();
            inner[i] = l.cosInner();
        }

        GL33C.glUseProgram(program);
        GL33C.glUniformMatrix4fv(uInvVp, false, m);
        GL33C.glUniform1i(uCount, n);
        GL33C.glUniform4fv(uPosRad, posRad);
        GL33C.glUniform4fv(uColorInt, colInt);
        GL33C.glUniform4fv(uDirCone, dirCone);
        GL33C.glUniform1fv(uInner, inner);
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, depthTexId);
        GL33C.glUniform1i(uDepth, 0);

        GL33C.glDisable(GL33C.GL_DEPTH_TEST);
        GL33C.glDepthMask(false);
        GL33C.glEnable(GL33C.GL_BLEND);
        GL33C.glBlendFunc(GL33C.GL_ONE, GL33C.GL_ONE);
        GL33C.glBindVertexArray(vao);
        GL33C.glDrawArrays(GL33C.GL_TRIANGLES, 0, 3);
        GL33C.glDisable(GL33C.GL_BLEND);
        GL33C.glDepthMask(true);
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);
    }

    private static void init() {
        vao = GL33C.glGenVertexArrays();   // triangulo de tela cheia sem atributos
        String vs = """
                #version 150
                out vec2 vUv;
                void main() {
                    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                    vUv = p;
                    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
                }""";
        String fs = """
                #version 150
                uniform sampler2D uDepth;
                uniform mat4 uInvVp;
                uniform int uCount;
                uniform vec4 uPosRad[16];
                uniform vec4 uColorInt[16];
                uniform vec4 uDirCone[16];
                uniform float uInner[16];
                in vec2 vUv;
                out vec4 fragColor;
                void main() {
                    float d = texture(uDepth, vUv).r;
                    if (d <= 0.0001) discard;              // ceu (reversed-Z: far = 0)
                    // depth reversed -> ndc classico da matriz do Frustum
                    vec4 clip = vec4(vUv * 2.0 - 1.0, 1.0 - 2.0 * d, 1.0);
                    vec4 pw = uInvVp * clip;
                    vec3 pos = pw.xyz / pw.w;              // relativo a camera
                    vec3 N = normalize(cross(dFdx(pos), dFdy(pos)));
                    vec3 acc = vec3(0.0);
                    for (int i = 0; i < uCount; i++) {
                        vec3 L = uPosRad[i].xyz - pos;
                        float dist = length(L);
                        if (dist > uPosRad[i].w) continue;
                        L /= dist;
                        float att = 1.0 - dist / uPosRad[i].w;
                        att *= att;
                        float ndl = max(dot(N, L), 0.0);
                        float cone = 1.0;
                        if (uDirCone[i].w > -1.5) {
                            float c = dot(-L, uDirCone[i].xyz);
                            cone = smoothstep(uDirCone[i].w, uInner[i], c);
                        }
                        acc += uColorInt[i].rgb * (uColorInt[i].a * att * ndl * cone);
                    }
                    fragColor = vec4(acc, 1.0);
                }""";
        program = compile(vs, fs);
        uInvVp = GL33C.glGetUniformLocation(program, "uInvVp");
        uCount = GL33C.glGetUniformLocation(program, "uCount");
        uPosRad = GL33C.glGetUniformLocation(program, "uPosRad");
        uColorInt = GL33C.glGetUniformLocation(program, "uColorInt");
        uDirCone = GL33C.glGetUniformLocation(program, "uDirCone");
        uInner = GL33C.glGetUniformLocation(program, "uInner");
        uDepth = GL33C.glGetUniformLocation(program, "uDepth");
    }

    static int compile(String vsSrc, String fsSrc) {
        int v = GL33C.glCreateShader(GL33C.GL_VERTEX_SHADER);
        GL33C.glShaderSource(v, vsSrc); GL33C.glCompileShader(v);
        if (GL33C.glGetShaderi(v, GL33C.GL_COMPILE_STATUS) == 0)
            throw new IllegalStateException("vs: " + GL33C.glGetShaderInfoLog(v));
        int f = GL33C.glCreateShader(GL33C.GL_FRAGMENT_SHADER);
        GL33C.glShaderSource(f, fsSrc); GL33C.glCompileShader(f);
        if (GL33C.glGetShaderi(f, GL33C.GL_COMPILE_STATUS) == 0)
            throw new IllegalStateException("fs: " + GL33C.glGetShaderInfoLog(f));
        int p = GL33C.glCreateProgram();
        GL33C.glAttachShader(p, v); GL33C.glAttachShader(p, f);
        GL33C.glLinkProgram(p);
        if (GL33C.glGetProgrami(p, GL33C.GL_LINK_STATUS) == 0)
            throw new IllegalStateException("link: " + GL33C.glGetProgramInfoLog(p));
        GL33C.glDeleteShader(v); GL33C.glDeleteShader(f);
        return p;
    }

    private ShowLights() {}
}

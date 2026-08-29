package dev.pavliukovbr.mayhem.client;

import dev.pavliukovbr.mayhem.MayhemShow;
import dev.pavliukovbr.mayhem.client.mixin.FrustumAccessor;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Desenha o castelo do Meshy como VBO retido, com GL direto por baixo da
 * abstracao GpuDevice (backend GL no macOS). 150k triangulos parados custam
 * um unico draw call por frame; o detalhe fino esta na malha e na textura,
 * nao em blocos.
 *
 * A matriz vem do Frustum da propria engine (view-projection relativa a
 * camera), entao nao ha como divergir da projecao do jogo, mesmo com FOV
 * dinamico ou spyglass.
 */
public final class PropRenderer {
    /** Um prop: malha + posicao interpolada + rumo. */
    public static final class Prop {
        final String meshPath;
        int vao = -1, indexCount;
        double sx, sy, sz, tx, ty, tz;    // origem e destino do movimento
        float syaw, tyaw;                  // rotacao tambem anima (portas!)
        long moveStart; int moveDur;       // ms

        Prop(String meshPath, double x, double y, double z, float yaw) {
            this.meshPath = meshPath;
            sx = tx = x; sy = ty = y; sz = tz = z; syaw = tyaw = yaw;
        }

        void moveTo(double x, double y, double z, float yaw, int durMs) {
            double[] now = pos();
            sx = now[0]; sy = now[1]; sz = now[2]; syaw = (float) now[3];
            tx = x; ty = y; tz = z; tyaw = yaw;
            moveStart = System.currentTimeMillis();
            moveDur = Math.max(durMs, 1);
        }

        double[] pos() {
            long dt = System.currentTimeMillis() - moveStart;
            double t = moveDur <= 0 ? 1.0 : Math.min(1.0, dt / (double) moveDur);
            t = t * t * (3 - 2 * t);   // ease in-out
            return new double[]{sx + (tx - sx) * t, sy + (ty - sy) * t,
                                sz + (tz - sz) * t, syaw + (tyaw - syaw) * t};
        }
    }

    private static final java.util.Map<String, Prop> PROPS = new java.util.LinkedHashMap<>();
    static {
        PROPS.put("castle",     new Prop("/assets/mayhem/meshes/castle.bin",     0.5, -54.0, 37.8, 180f));
        PROPS.put("dress_body", new Prop("/assets/mayhem/meshes/dress_body.bin", 0.5, -60.0, 75.0, 180f));
        PROPS.put("curtain_l",  new Prop("/assets/mayhem/meshes/curtain_l.bin",  0.5, -60.0, 75.0, 180f));
        PROPS.put("curtain_r",  new Prop("/assets/mayhem/meshes/curtain_r.bin",  0.5, -60.0, 75.0, 180f));
        PROPS.put("cage",       new Prop("/assets/mayhem/meshes/cage.bin",       0.5, -60.0, 75.0, 180f));
        PROPS.put("cage_door",  new Prop("/assets/mayhem/meshes/cage_door.bin",  0.5, -60.0, 75.0, 180f));
    }

    public static void moveProp(String name, double x, double y, double z, float yaw, int durMs) {
        Prop p = PROPS.get(name);
        if (p != null) p.moveTo(x, y, z, yaw, durMs);
        MayhemShow.LOGGER.info("MOVE {} -> ({}, {}, {}) yaw={} dur={}ms", name, x, y, z, yaw, durMs);
    }

    public static void dumpState() {
        PROPS.forEach((n, p) -> {
            double[] w = p.pos();
            MayhemShow.LOGGER.info("STATE {} pos=({}, {}, {}) yaw={}",
                    n, String.format("%.1f", w[0]), String.format("%.1f", w[1]),
                    String.format("%.1f", w[2]), String.format("%.1f", w[3]));
        });
    }

    private static int program = -1, uMvp, uSun, uCam;
    private static int frameNo;
    private static int fbo = -1, lastColor, lastDepth;
    private static boolean broken;

    public static void render(CameraRenderState cam) {
        DebugShot.tick();
        String mode = System.getProperty("mayhem.mode", "full");
        if (mode.equals("off") || broken || cam.cullFrustum == null) return;
        boolean drawIt = mode.equals("full");
        try {
            if (program == -1) init();
            if (drawIt) draw(cam);
        } catch (Throwable t) {
            broken = true;
            MayhemShow.LOGGER.error("PropRenderer desativado", t);
        }
    }

    private static void draw(CameraRenderState cam) {
        FrustumAccessor fr = (FrustumAccessor) (Object) cam.cullFrustum;

        RenderTarget rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int colorId = ((GlTexture) rt.getColorTexture()).glId();
        int depthId = ((GlTexture) rt.getDepthTexture()).glId();
        if (fbo == -1) fbo = GL33C.glGenFramebuffers();
        int prevDrawFbo = GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL33C.glGetInteger(GL33C.GL_READ_FRAMEBUFFER_BINDING);
        GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, fbo);
        if (colorId != lastColor || depthId != lastDepth) {
            GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0,
                    GL33C.GL_TEXTURE_2D, colorId, 0);
            GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT,
                    GL33C.GL_TEXTURE_2D, depthId, 0);
            lastColor = colorId; lastDepth = depthId;
        }
        int[] prevVp = new int[4];
        GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, prevVp);
        GL33C.glViewport(0, 0, rt.width, rt.height);
        boolean prevScissor = GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);
        GL33C.glDisable(GL33C.GL_SCISSOR_TEST);

        int prevProgram = GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM);
        int prevVao = GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING);
        boolean prevCull = GL33C.glIsEnabled(GL33C.GL_CULL_FACE);
        boolean prevDepth = GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST);
        boolean prevBlend = GL33C.glIsEnabled(GL33C.GL_BLEND);
        int prevDepthFunc = GL33C.glGetInteger(GL33C.GL_DEPTH_FUNC);
        boolean prevDepthMask = GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK);

        GL33C.glUseProgram(program);
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);
        GL33C.glDepthFunc(GL33C.GL_GEQUAL);
        GL33C.glDepthMask(true);
        GL33C.glDisable(GL33C.GL_CULL_FACE);
        GL33C.glDisable(GL33C.GL_BLEND);
        frameNo++;

        for (Prop p : PROPS.values()) {
            double[] w = p.pos();
            float yawNow = (float) w[3];
            Matrix4f mvp = new Matrix4f(fr.mayhem$matrix())
                    .translate((float) (w[0] - fr.mayhem$camX()),
                               (float) (w[1] - fr.mayhem$camY()),
                               (float) (w[2] - fr.mayhem$camZ()))
                    .rotateY((float) Math.toRadians(yawNow));
            // frustum classico vs depth reversed-Z: nega a linha z do clip
            mvp = new Matrix4f().scaling(1f, 1f, -1f).mul(mvp);
            float[] m = new float[16];
            mvp.get(m);
            GL33C.glUniformMatrix4fv(uMvp, false, m);
            double yr = Math.toRadians(yawNow);
            float sxz = (float) Math.cos(yr), szx = (float) Math.sin(yr);
            // sol fixo do mundo levado para o espaco do modelo girado
            GL33C.glUniform3f(uSun, -0.35f * sxz, 0.85f, 0.40f * sxz);
            GL33C.glUniform3f(uCam,
                    (float) ((fr.mayhem$camX() - w[0]) * -sxz),
                    (float) (fr.mayhem$camY() - w[1]),
                    (float) ((fr.mayhem$camZ() - w[2]) * -sxz));
            GL33C.glBindVertexArray(p.vao);
            GL33C.glDrawElements(GL33C.GL_TRIANGLES, p.indexCount, GL33C.GL_UNSIGNED_INT, 0);
        }

        GL33C.glBindVertexArray(prevVao);
        GL33C.glUseProgram(prevProgram);
        if (prevCull) GL33C.glEnable(GL33C.GL_CULL_FACE);
        if (!prevDepth) GL33C.glDisable(GL33C.GL_DEPTH_TEST);
        if (prevBlend) GL33C.glEnable(GL33C.GL_BLEND);
        GL33C.glDepthFunc(prevDepthFunc);
        GL33C.glDepthMask(prevDepthMask);
        GL33C.glViewport(prevVp[0], prevVp[1], prevVp[2], prevVp[3]);
        if (prevScissor) GL33C.glEnable(GL33C.GL_SCISSOR_TEST);
        GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
        GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER, prevReadFbo);
    }

    private static void init() throws Exception {
        for (Prop p : PROPS.values()) loadMesh(p);
        program = buildProgram();
        uMvp = GL33C.glGetUniformLocation(program, "uMvp");
        uCam = GL33C.glGetUniformLocation(program, "uCam");
        uSun = GL33C.glGetUniformLocation(program, "uSun");
        MayhemShow.LOGGER.info("Props carregados: {}", PROPS.keySet());
    }

    private static void loadMesh(Prop p) throws Exception {
        ByteBuffer bin = readResource(p.meshPath);
        bin.order(ByteOrder.LITTLE_ENDIAN);
        if (bin.getInt() != 0x4D534D33) throw new IllegalStateException("magic errado: " + p.meshPath);
        int vcount = bin.getInt();
        p.indexCount = bin.getInt();

        p.vao = GL33C.glGenVertexArrays();
        int vbo = GL33C.glGenBuffers();
        int ebo = GL33C.glGenBuffers();
        GL33C.glBindVertexArray(p.vao);
        ByteBuffer verts = bin.slice(bin.position(), vcount * 40).order(ByteOrder.LITTLE_ENDIAN);
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, verts, GL33C.GL_STATIC_DRAW);
        bin.position(bin.position() + vcount * 40);
        ByteBuffer idx = bin.slice(bin.position(), p.indexCount * 4).order(ByteOrder.LITTLE_ENDIAN);
        GL33C.glBindBuffer(GL33C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL33C.glBufferData(GL33C.GL_ELEMENT_ARRAY_BUFFER, idx, GL33C.GL_STATIC_DRAW);
        GL33C.glEnableVertexAttribArray(0);
        GL33C.glVertexAttribPointer(0, 3, GL33C.GL_FLOAT, false, 40, 0);
        GL33C.glEnableVertexAttribArray(1);
        GL33C.glVertexAttribPointer(1, 3, GL33C.GL_FLOAT, false, 40, 12);
        GL33C.glEnableVertexAttribArray(2);
        GL33C.glVertexAttribPointer(2, 3, GL33C.GL_FLOAT, false, 40, 24);
        GL33C.glEnableVertexAttribArray(3);
        GL33C.glVertexAttribPointer(3, 1, GL33C.GL_FLOAT, false, 40, 36);
        GL33C.glBindVertexArray(0);
    }

    private static int buildProgram() {
        String vs = """
                #version 150
                uniform mat4 uMvp;
                in vec3 aPos; in vec3 aNormal; in vec3 aColor; in float aAo;
                out vec3 vNormal; out vec3 vColor; out vec3 vPos; out float vAo;
                void main() {
                    gl_Position = uMvp * vec4(aPos, 1.0);
                    vNormal = aNormal; vColor = aColor; vPos = aPos; vAo = aAo;
                }""";
        String fs = """
                #version 150
                uniform vec3 uSun; uniform vec3 uCam;
                in vec3 vNormal; in vec3 vColor; in vec3 vPos; in float vAo;
                out vec4 fragColor;
                void main() {
                    vec3 n = normalize(vNormal);
                    // hemisferica (ceu por cima, chao escuro), sol, fill fraca
                    float hemi = mix(0.34, 0.62, n.y * 0.5 + 0.5);
                    float sun  = 0.45 * max(dot(n, normalize(uSun)), 0.0);
                    float fill = 0.16 * max(dot(n, normalize(vec3(-uSun.x, 0.25, -uSun.z))), 0.0);
                    // AO bakeada e o que faz o entalhe aparecer
                    float ao = 0.38 + 0.62 * pow(max(vAo, 0.0), 0.55);
                    // rim descola a silhueta do telao preto
                    vec3 v = normalize(uCam - vPos);
                    float rim = 0.05 * pow(1.0 - max(dot(n, v), 0.0), 4.0);
                    vec3 c = vColor * (hemi + sun + fill) * ao + rim * vec3(0.85, 0.83, 0.78);
                    fragColor = vec4(pow(c, vec3(1.0 / 2.2)), 1.0);
                }""";
        int v = compile(GL33C.GL_VERTEX_SHADER, vs);
        int f = compile(GL33C.GL_FRAGMENT_SHADER, fs);
        int p = GL33C.glCreateProgram();
        GL33C.glAttachShader(p, v);
        GL33C.glAttachShader(p, f);
        GL33C.glBindAttribLocation(p, 0, "aPos");
        GL33C.glBindAttribLocation(p, 1, "aNormal");
        GL33C.glBindAttribLocation(p, 2, "aColor");
        GL33C.glBindAttribLocation(p, 3, "aAo");
        GL33C.glLinkProgram(p);
        if (GL33C.glGetProgrami(p, GL33C.GL_LINK_STATUS) == 0)
            throw new IllegalStateException("link: " + GL33C.glGetProgramInfoLog(p));
        GL33C.glDeleteShader(v);
        GL33C.glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String src) {
        int s = GL33C.glCreateShader(type);
        GL33C.glShaderSource(s, src);
        GL33C.glCompileShader(s);
        if (GL33C.glGetShaderi(s, GL33C.GL_COMPILE_STATUS) == 0)
            throw new IllegalStateException("shader: " + GL33C.glGetShaderInfoLog(s));
        return s;
    }

    private static int loadTexture(String path) throws Exception {
        ByteBuffer png = readResource(path);
        int[] w = new int[1], h = new int[1], ch = new int[1];
        ByteBuffer pixels = STBImage.stbi_load_from_memory(png, w, h, ch, 4);
        if (pixels == null) throw new IllegalStateException("textura: " + STBImage.stbi_failure_reason());
        int t = GL33C.glGenTextures();
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, t);
        GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA8, w[0], h[0], 0,
                GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, pixels);
        GL33C.glGenerateMipmap(GL33C.GL_TEXTURE_2D);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR_MIPMAP_LINEAR);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        STBImage.stbi_image_free(pixels);
        return t;
    }

    private static ByteBuffer readResource(String path) throws Exception {
        try (InputStream in = PropRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("recurso ausente: " + path);
            byte[] data = in.readAllBytes();
            ByteBuffer b = MemoryUtil.memAlloc(data.length);
            b.put(data).flip();
            return b;
        }
    }

    private PropRenderer() {}
}

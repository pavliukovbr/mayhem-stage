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
public final class CastleRenderer {
    /** Ancora no mundo: casa com a ancora do datapack (0,-59,0), castelo centrado. */
    private static final double AX = 0.5, AY = -59.0, AZ = 25.5;

    private static int program = -1, vao, vbo, ebo, tex, uMvp, uSun, uCam;
    private static int indexCount, frameNo;
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
            MayhemShow.LOGGER.error("CastleRenderer desativado", t);
        }
    }

    private static void draw(CameraRenderState cam) {
        FrustumAccessor fr = (FrustumAccessor) (Object) cam.cullFrustum;
        Matrix4f mvp = new Matrix4f(fr.mayhem$matrix())
                .translate((float) (AX - fr.mayhem$camX()),
                           (float) (AY - fr.mayhem$camY()),
                           (float) (AZ - fr.mayhem$camZ()));
        // O GLB do Meshy nasce de imagem: a fachada aponta para +Z.
        // No palco a frente e -Z (plateia); meia-volta na ancora resolve.
        mvp.rotateY((float) Math.PI);
        // A matriz do Frustum e classica (so culling); o depth buffer do jogo
        // e reversed-Z (limpo em 0). Negar a linha z do clip converte:
        // depth_rev = 1 - depth_classico, e o teste vira GEQUAL.
        mvp = new Matrix4f().scaling(1f, 1f, -1f).mul(mvp);
        if ((frameNo++ % 90) == 0) {
            org.joml.Vector4f probe = mvp.transform(new org.joml.Vector4f(0, 23, 0, 1));
            int[] vp = new int[4];
            GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, vp);
            int[] cm = new int[4];
            GL33C.glGetIntegerv(GL33C.GL_COLOR_WRITEMASK, cm);
            MayhemShow.LOGGER.info("PROBE ndc_y={} w={} fboDraw={} viewport={},{},{},{} scissor={} colorMask={},{},{},{} err={}",
                    probe.y / probe.w, probe.w,
                    GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING),
                    vp[0], vp[1], vp[2], vp[3],
                    GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST),
                    cm[0], cm[1], cm[2], cm[3],
                    GL33C.glGetError());
        }

        // estado atual do jogo, para devolver exatamente como estava
        // O mundo e desenhado num RenderTarget fora da tela; desenhar no
        // backbuffer some no blit. Um FBO proprio apontando para o color e o
        // depth do jogo poe o castelo NA CENA, com oclusao correta.
        RenderTarget rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int colorId = ((GlTexture) rt.getColorTexture()).glId();
        int depthId = ((GlTexture) rt.getDepthTexture()).glId();
        if (fbo == -1) fbo = GL33C.glGenFramebuffers();
        int prevDrawFbo = GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL33C.glGetInteger(GL33C.GL_READ_FRAMEBUFFER_BINDING);
        boolean useMainFbo = !"0".equals(System.getProperty("mayhem.fbo", "main"));
        if (useMainFbo) {
            GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, fbo);
            if (colorId != lastColor || depthId != lastDepth) {
                GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0,
                        GL33C.GL_TEXTURE_2D, colorId, 0);
                GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT,
                        GL33C.GL_TEXTURE_2D, depthId, 0);
                lastColor = colorId; lastDepth = depthId;
                int status = GL33C.glCheckFramebufferStatus(GL33C.GL_FRAMEBUFFER);
                MayhemShow.LOGGER.info("FBO status={} (completo={})", status,
                        status == GL33C.GL_FRAMEBUFFER_COMPLETE);
            }
        } else {
            GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, 0);
        }
        int[] prevVp = new int[4];
        GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, prevVp);
        GL33C.glViewport(0, 0, rt.width, rt.height);
        boolean prevScissor = GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);
        GL33C.glDisable(GL33C.GL_SCISSOR_TEST);

        int prevProgram = GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM);
        int prevVao = GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING);
        int prevActive = GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        int prevTex = GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D);
        boolean prevCull = GL33C.glIsEnabled(GL33C.GL_CULL_FACE);
        boolean prevDepth = GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST);
        boolean prevBlend = GL33C.glIsEnabled(GL33C.GL_BLEND);
        int prevDepthFunc = GL33C.glGetInteger(GL33C.GL_DEPTH_FUNC);
        boolean prevDepthMask = GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK);

        GL33C.glUseProgram(program);
        float[] m = new float[16];
        mvp.get(m);
        GL33C.glUniformMatrix4fv(uMvp, false, m);
        // direcoes em espaco do modelo: o mundo gira PI em Y, entao x,z trocam de sinal
        GL33C.glUniform3f(uSun, -0.35f, 0.85f, 0.40f);
        GL33C.glUniform3f(uCam, (float) -(fr.mayhem$camX() - AX), (float) (fr.mayhem$camY() - AY),
                (float) -(fr.mayhem$camZ() - AZ));
        GL33C.glBindVertexArray(vao);
        // 26.x usa reversed-Z: a func de depth que o jogo deixou e a certa
        // para a matriz que estamos usando. Nao tocar nela.
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);
        GL33C.glDepthFunc(GL33C.GL_GEQUAL);
        GL33C.glDepthMask(true);
        GL33C.glDisable(GL33C.GL_CULL_FACE);
        GL33C.glDisable(GL33C.GL_BLEND);

        GL33C.glDrawElements(GL33C.GL_TRIANGLES, indexCount, GL33C.GL_UNSIGNED_INT, 0);
        if (frameNo % 90 == 1) MayhemShow.LOGGER.info("POSDRAW err={} fboUsado={}",
                GL33C.glGetError(), useMainFbo ? fbo : 0);

        GL33C.glBindVertexArray(prevVao);
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, prevTex);
        GL33C.glActiveTexture(prevActive);
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
        ByteBuffer bin = readResource("/assets/mayhem/castle/castle_mesh.bin");
        bin.order(ByteOrder.LITTLE_ENDIAN);
        if (bin.getInt() != 0x4D534D33) throw new IllegalStateException("magic da mesh errado");
        int vcount = bin.getInt();
        indexCount = bin.getInt();

        vao = GL33C.glGenVertexArrays();
        vbo = GL33C.glGenBuffers();
        ebo = GL33C.glGenBuffers();
        GL33C.glBindVertexArray(vao);

        ByteBuffer verts = bin.slice(bin.position(), vcount * 40).order(ByteOrder.LITTLE_ENDIAN);
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, verts, GL33C.GL_STATIC_DRAW);
        bin.position(bin.position() + vcount * 40);
        ByteBuffer idx = bin.slice(bin.position(), indexCount * 4).order(ByteOrder.LITTLE_ENDIAN);
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

        program = buildProgram();
        uMvp = GL33C.glGetUniformLocation(program, "uMvp");
        uCam = GL33C.glGetUniformLocation(program, "uCam");
        uSun = GL33C.glGetUniformLocation(program, "uSun");
        MayhemShow.LOGGER.info("Castelo carregado: {} vertices, {} tris", vcount, indexCount / 3);
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
        try (InputStream in = CastleRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("recurso ausente: " + path);
            byte[] data = in.readAllBytes();
            ByteBuffer b = MemoryUtil.memAlloc(data.length);
            b.put(data).flip();
            return b;
        }
    }

    private CastleRenderer() {}
}

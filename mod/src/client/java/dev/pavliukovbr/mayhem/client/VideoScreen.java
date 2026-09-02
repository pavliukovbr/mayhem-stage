package dev.pavliukovbr.mayhem.client;

import dev.pavliukovbr.mayhem.MayhemShow;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * O telao toca video de verdade: .mvid (sequencia de JPEG) da pasta
 * mayhem_videos do jogo, desenhado emissivo num quad sobre a face do
 * telao, e EMITINDO luz na cena: tres luzes na cor media do frame atual
 * entram no passe diferido a cada frame.
 */
public final class VideoScreen {
    // face do telao em coordenadas do mundo (blocos: x -93..94, y -53..-2, z 54)
    private static final float X0 = -93f, X1 = 94f, Y0 = -53f, Y1 = -2f, Z = 53.94f;

    private static byte[][] frames;
    private static int fps = 10, current = -1;
    private static long startMs;
    private static boolean playing;

    private static int tex = -1, program = -1, vao, uMvp;
    private static int dbg;
    private static float avgR = 0.3f, avgG = 0.05f, avgB = 0.08f;

    public static void handle(String arg) {
        Minecraft.getInstance().execute(() -> {
            if (arg.startsWith("play ")) load(arg.substring(5).trim());
            else { playing = false; ShowLights.DYNAMIC.clear(); }
        });
    }

    private static void load(String name) {
        try {
            Path p = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("mayhem_videos").resolve(name + ".mvid");
            try (InputStream in = Files.newInputStream(p)) {
                DataInputStream d = new DataInputStream(in);
                byte[] magic = new byte[4]; d.readFully(magic);
                if (!new String(magic).equals("MVID")) throw new IllegalStateException("magic");
                fps = Integer.reverseBytes(d.readInt());
                int count = Integer.reverseBytes(d.readInt());
                List<byte[]> fs = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    byte[] buf = new byte[Integer.reverseBytes(d.readInt())];
                    d.readFully(buf); fs.add(buf);
                }
                frames = fs.toArray(new byte[0][]);
            }
            current = -1; startMs = System.currentTimeMillis(); playing = true;
            MayhemShow.LOGGER.info("Video {}: {} frames @ {}fps", name, frames.length, fps);
        } catch (Exception e) {
            MayhemShow.LOGGER.warn("video {} indisponivel: {}", name, e.toString());
            playing = false;
        }
    }

    static void render(Matrix4f frustum, double camX, double camY, double camZ) {
        if (!playing || frames == null) return;
        if (program == -1) init();

        int idx = (int) ((System.currentTimeMillis() - startMs) * fps / 1000) % frames.length;
        if (idx != current) upload(idx);

        Matrix4f mvp = new Matrix4f().scaling(1f, 1f, -1f).mul(new Matrix4f(frustum)
                .translate((float) -camX, (float) -camY, (float) -camZ));
        float[] m = new float[16];
        mvp.get(m);
        GL33C.glUseProgram(program);
        GL33C.glUniformMatrix4fv(uMvp, false, m);
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, tex);
        GL33C.glBindVertexArray(vao);
        GL33C.glEnable(GL33C.GL_DEPTH_TEST);
        GL33C.glDepthFunc(GL33C.GL_GEQUAL);
        GL33C.glDrawArrays(GL33C.GL_TRIANGLES, 0, 6);
        if (dbg++ % 120 == 0) MayhemShow.LOGGER.info("SCREEN draw idx={} err={}", idx, GL33C.glGetError());

        // o telao emite: tres luzes na cor media do frame
        ShowLights.DYNAMIC.clear();
        float it = 1.9f;
        ShowLights.DYNAMIC.add(ShowLights.Light.point(-55, -30, 50, avgR, avgG, avgB, it, 75f));
        ShowLights.DYNAMIC.add(ShowLights.Light.point(0.5, -28, 50, avgR, avgG, avgB, it, 80f));
        ShowLights.DYNAMIC.add(ShowLights.Light.point(56, -30, 50, avgR, avgG, avgB, it, 75f));
    }

    private static void upload(int idx) {
        current = idx;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(frames[idx]));
            int w = img.getWidth(), h = img.getHeight();
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
            long r = 0, g = 0, b = 0;
            for (int p : px) {
                buf.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) 255);
                r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255;
            }
            buf.flip();
            int n = px.length;
            // media em linear-aproximado para a cor da luz emitida
            avgR = (float) Math.pow(r / (double) n / 255.0, 2.2);
            avgG = (float) Math.pow(g / (double) n / 255.0, 2.2);
            avgB = (float) Math.pow(b / (double) n / 255.0, 2.2);
            GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, tex);
            GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA8, w, h, 0,
                    GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, buf);
            MemoryUtil.memFree(buf);
        } catch (Exception e) {
            MayhemShow.LOGGER.warn("frame {} ruim: {}", idx, e.toString());
        }
    }

    private static void init() {
        tex = GL33C.glGenTextures();
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, tex);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);

        String vs = """
                #version 150
                uniform mat4 uMvp;
                in vec3 aPos; in vec2 aUv;
                out vec2 vUv;
                void main() { gl_Position = uMvp * vec4(aPos, 1.0); vUv = aUv; }""";
        String fs = """
                #version 150
                uniform sampler2D uTex;
                in vec2 vUv;
                out vec4 fragColor;
                void main() { fragColor = vec4(texture(uTex, vUv).rgb, 1.0); }""";
        program = ShowLights.compile(vs, fs);
        uMvp = GL33C.glGetUniformLocation(program, "uMvp");

        // o VAO usa os locations REAIS que o link atribuiu
        int locPos = GL33C.glGetAttribLocation(program, "aPos");
        int locUv = GL33C.glGetAttribLocation(program, "aUv");
        vao = GL33C.glGenVertexArrays();
        int vbo = GL33C.glGenBuffers();
        GL33C.glBindVertexArray(vao);
        float[] q = {   // dois triangulos, uv com V invertido (imagem top-down)
            X0, Y0, Z, 0, 1,  X1, Y0, Z, 1, 1,  X1, Y1, Z, 1, 0,
            X0, Y0, Z, 0, 1,  X1, Y1, Z, 1, 0,  X0, Y1, Z, 0, 0 };
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, q, GL33C.GL_STATIC_DRAW);
        GL33C.glEnableVertexAttribArray(locPos);
        GL33C.glVertexAttribPointer(locPos, 3, GL33C.GL_FLOAT, false, 20, 0);
        GL33C.glEnableVertexAttribArray(locUv);
        GL33C.glVertexAttribPointer(locUv, 2, GL33C.GL_FLOAT, false, 20, 12);
        GL33C.glBindVertexArray(0);
    }

    private VideoScreen() {}
}

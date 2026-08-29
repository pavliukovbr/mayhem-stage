package dev.pavliukovbr.mayhem.client;

import dev.pavliukovbr.mayhem.MayhemShow;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Harness de dev: com -Dmayhem.debugshot=1, salva um PNG do framebuffer
 * no frame N e derruba o processo. Serve para validar render sem humano
 * olhando a janela. Nunca ativo fora do runClient.
 */
public final class DebugShot {
    private static int frames;

    public static void tick() {
        if (System.getProperty("mayhem.debugshot") == null) return;
        frames++;
        String cmd = System.getProperty("mayhem.debugshot.cmd");
        if (cmd != null && !cmd.isEmpty()) {
            String[] cmds = cmd.split(";");
            int base = 100;
            var mc = net.minecraft.client.Minecraft.getInstance();
            for (int i = 0; i < cmds.length; i++) {
                if (frames == base + i * 40 && mc.player != null) {
                    mc.player.connection.sendCommand(cmds[i].trim());
                }
            }
        }
        if (frames != Integer.getInteger("mayhem.debugshot.frame", 300)) return;
        PropRenderer.dumpState();
        int[] vp = new int[4];
        GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, vp);
        int w = vp[2], h = vp[3];
        ByteBuffer px = MemoryUtil.memAlloc(w * h * 4);
        GL33C.glReadPixels(0, 0, w, h, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, px);
        STBImageWrite.stbi_flip_vertically_on_write(true);
        String out = System.getProperty("mayhem.debugshot.file", "debugshot.png");
        STBImageWrite.stbi_write_png(out, w, h, 4, px, w * 4);
        MemoryUtil.memFree(px);
        MayhemShow.LOGGER.info("DEBUGSHOT salvo em {} ({}x{})", out, w, h);
        System.exit(0);
    }

    private DebugShot() {}
}

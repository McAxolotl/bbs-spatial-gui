package mcaxolotl.bbsspatialgui;

import mcaxolotl.bbsspatialgui.capture.PixelTransferState;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class BlurShaderChecks {
    static void verify() throws Exception {
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND),
                depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        ByteBuffer mask = BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
        int source = GL11.glGenTextures(),
                output = GL11.glGenTextures(),
                target = GL30.glGenFramebuffers(),
                vertices = GL30.glGenVertexArrays();
        try (PixelTransferState pack = new PixelTransferState(true);
                PixelTransferState unpack = new PixelTransferState(false)) {
            ByteBuffer pixels = BufferUtils.createByteBuffer(16 * 16 * 4);
            for (int i = 0; i < 16 * 16; i++)
                pixels.put((byte) 64).put((byte) 128).put((byte) 192).put((byte) 64);
            pixels.flip();
            for (int tex : new int[] {source, output}) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
                GL11.glTexParameteri(
                        GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(
                        GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(
                        GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(
                        GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexImage2D(
                        GL11.GL_TEXTURE_2D,
                        0,
                        GL11.GL_RGBA8,
                        16,
                        16,
                        0,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        tex == source ? pixels : null);
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target);
            GL30.glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, output, 0);
            CoreTests.check(
                    GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
                            == GL30.GL_FRAMEBUFFER_COMPLETE,
                    "blur test framebuffer incomplete");
            GL11.glViewport(0, 0, 16, 16);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL30.glBindVertexArray(vertices);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, source);
            for (String direction : new String[] {"down", "up"})
                for (boolean adapted : new boolean[] {false, true}) {
                    String name = (adapted ? "bbs_spatial_gui_kawase_" : "bbs_kawase_") + direction;
                    String fragment;
                    try (var in =
                            BlurShaderChecks.class.getResourceAsStream(
                                    "/assets/minecraft/shaders/program/" + name + ".fsh")) {
                        if (in == null)
                            throw new AssertionError("missing actual blur resource " + name);
                        fragment = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    int vertex =
                            compile(
                                    GL20.GL_VERTEX_SHADER,
                                    "#version 150\nout vec2 texCoord; out vec2 oneTexel; void main(){vec2 p=vec2(gl_VertexID==1?3.0:-1.0,gl_VertexID==2?3.0:-1.0);gl_Position=vec4(p,0,1);texCoord=p*0.5+0.5;oneTexel=vec2(1.0/16.0);}");
                    int shader = compile(GL20.GL_FRAGMENT_SHADER, fragment),
                            linked = GL20.glCreateProgram();
                    try {
                        GL20.glAttachShader(linked, vertex);
                        GL20.glAttachShader(linked, shader);
                        GL20.glLinkProgram(linked);
                        CoreTests.check(
                                GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_TRUE,
                                "blur link failed: " + GL20.glGetProgramInfoLog(linked));
                        GL20.glUseProgram(linked);
                        GL20.glUniform1i(GL20.glGetUniformLocation(linked, "DiffuseSampler"), 0);
                        GL20.glUniform1f(GL20.glGetUniformLocation(linked, "Offset"), 1);
                        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
                        ByteBuffer value = BufferUtils.createByteBuffer(4);
                        GL11.glReadPixels(8, 8, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, value);
                        int alpha = value.get(3) & 255;
                        CoreTests.check(
                                Math.abs(alpha - (adapted ? 64 : 255)) <= 1,
                                "unexpected blur alpha in " + name + ": " + alpha);
                    } finally {
                        GL20.glDeleteProgram(linked);
                        GL20.glDeleteShader(vertex);
                        GL20.glDeleteShader(shader);
                    }
                }
            System.out.println(
                    "PASS actual blur shaders: adapted RGBA retains alpha 64; upstream RGB forces alpha 255");
        } finally {
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);
            GL30.glDeleteVertexArrays(vertices);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glDeleteTextures(source);
            GL11.glDeleteTextures(output);
            GL13.glActiveTexture(active);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glDeleteFramebuffers(target);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL11.glColorMask(
                    mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
            if (blend) GL11.glEnable(GL11.GL_BLEND);
            else GL11.glDisable(GL11.GL_BLEND);
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            else GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            String error = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new AssertionError(error);
        }
        return shader;
    }

    private BlurShaderChecks() {}
}

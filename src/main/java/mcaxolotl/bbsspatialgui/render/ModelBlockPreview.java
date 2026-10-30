package mcaxolotl.bbsspatialgui.render;

import mcaxolotl.bbsspatialgui.capture.RecordingBridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ModelBlockPreview {
    private static boolean owned, borrowed;
    private static Method currentMenu, panels, setCustomSize, getTexture, texturedBox, flush;
    private static Field panel,
            batcher,
            menu,
            menuWidth,
            menuHeight,
            textureId,
            textureWidth,
            textureHeight;

    private static void initialize() throws ReflectiveOperationException {
        if (currentMenu != null) return;
        Class<?> screen = Class.forName("mchorse.bbs_mod.ui.framework.UIScreen");
        Method current = screen.getMethod("getCurrentMenu");
        panels = Class.forName("mchorse.bbs_mod.ui.dashboard.UIDashboard").getMethod("getPanels");
        panel = panels.getReturnType().getField("panel");
        Class<?> rendering = Class.forName("mchorse.bbs_mod.client.BBSRendering");
        setCustomSize = rendering.getMethod("setCustomSize", boolean.class);
        getTexture = rendering.getMethod("getTexture");
        Class<?> context = Class.forName("mchorse.bbs_mod.ui.framework.UIContext");
        batcher = context.getField("batcher");
        menu = context.getField("menu");
        menuWidth = menu.getType().getField("width");
        menuHeight = menu.getType().getField("height");
        Class<?> texture = getTexture.getReturnType();
        textureId = texture.getField("id");
        textureWidth = texture.getField("width");
        textureHeight = texture.getField("height");
        texturedBox =
                batcher.getType()
                        .getMethod(
                                "texturedBox",
                                int.class,
                                int.class,
                                float.class,
                                float.class,
                                float.class,
                                float.class,
                                float.class,
                                float.class,
                                float.class,
                                float.class,
                                int.class,
                                int.class);
        flush = batcher.getType().getMethod("flush");
        currentMenu = current;
    }

    public static boolean active() {
        return owned && RenderContext.currentSpatialBBSScreen();
    }

    public static void beforeWorld() {
        if (BackgroundWorldPass.active() || !RenderContext.currentSpatialBBSScreen()) return;
        try {
            initialize();
            Object dashboard = currentMenu.invoke(null);
            if (!panels.getDeclaringClass().isInstance(dashboard)) return;
            Object selected = panel.get(panels.invoke(dashboard));
            if (selected == null
                    || !selected.getClass()
                            .getName()
                            .equals("mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel")) return;
            if (!owned) {
                borrowed = RenderContext.bbsCustomSizeActive();
                if (!borrowed) setCustomSize.invoke(null, true);
                owned = true;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot begin model-block preview capture", e);
        }
    }

    public static void close() {
        if (!owned) return;
        try {
            if (!borrowed && !RecordingBridge.isRecording()) setCustomSize.invoke(null, false);
            owned = false;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot restore model-block capture mode", e);
        }
    }

    public static void draw(Object context) {
        if (!active()) return;
        try {
            Object texture = getTexture.invoke(null);
            Object target = batcher.get(context);
            Object ui = menu.get(context);
            int w = textureWidth.getInt(texture), h = textureHeight.getInt(texture);
            flush.invoke(target);
            texturedBox.invoke(
                    target,
                    textureId.getInt(texture),
                    -1,
                    0F,
                    0F,
                    (float) menuWidth.getInt(ui),
                    (float) menuHeight.getInt(ui),
                    0F,
                    (float) h,
                    (float) w,
                    0F,
                    w,
                    h);
            flush.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "[BBS Spatial GUI] Cannot draw model-block captured preview", e);
        }
    }

    private ModelBlockPreview() {}
}

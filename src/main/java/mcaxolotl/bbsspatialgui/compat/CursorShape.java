package mcaxolotl.bbsspatialgui.compat;

import mcaxolotl.bbsspatialgui.cursor.CursorState;

import java.util.function.IntSupplier;

public final class CursorShape implements IntSupplier {
    @Override
    public int getAsInt() {
        return CursorState.shape();
    }
}

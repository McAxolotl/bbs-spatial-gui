package mcaxolotl.bbsspatialgui;

public final class CoreTests {
    public static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}

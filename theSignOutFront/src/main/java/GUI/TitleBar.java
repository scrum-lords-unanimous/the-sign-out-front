package GUI;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Uses the Windows DWM API via Panama FFM to set the title bar color
 * and dark mode to match the app theme.
 */
public class TitleBar {
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_CAPTION_COLOR = 35;

    public static void apply(long nativeHandle, boolean dark, int r, int g, int b) {
        if (nativeHandle == 0) return;
        try {
            var linker = Linker.nativeLinker();
            SymbolLookup dwmapi = SymbolLookup.libraryLookup("dwmapi", Arena.global());

            MethodHandle dwmSetAttr = linker.downcallHandle(
                    dwmapi.find("DwmSetWindowAttribute").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            MemorySegment hwnd = MemorySegment.ofAddress(nativeHandle);

            try (var arena = Arena.ofConfined()) {
                // Set dark/light mode for title bar icons and text
                var darkVal = arena.allocate(ValueLayout.JAVA_INT, dark ? 1 : 0);
                dwmSetAttr.invoke(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkVal, 4);

                // Set caption color (COLORREF = 0x00BBGGRR)
                var color = arena.allocate(ValueLayout.JAVA_INT, (b << 16) | (g << 8) | r);
                dwmSetAttr.invoke(hwnd, DWMWA_CAPTION_COLOR, color, 4);
            }
        } catch (Throwable e) {
            System.out.println("Could not customize title bar: " + e.getMessage());
        }
    }
}

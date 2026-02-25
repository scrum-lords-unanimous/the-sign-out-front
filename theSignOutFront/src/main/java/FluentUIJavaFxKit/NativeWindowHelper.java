//additional api stuff for windows in windows. 
package FluentUIJavaFxKit;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class NativeWindowHelper {

    private static final int GWL_STYLE = -16;
    private static final long WS_OVERLAPPEDWINDOW = 0x00CF0000L;
    private static final int SWP_FRAMECHANGED = 0x0020;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SW_MAXIMIZE = 3;
    private static final int SW_MINIMIZE = 6;
    private static final int SW_RESTORE = 9;
    private static final int DWMWA_TRANSITIONS_FORCEDISABLED = 3;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWCP_ROUND = 2;
    private static final int DWM_MARGINS_SIZE = 16;
    private static final int DWM_ATTRIBUTE_SIZE = 4;

    static int leftClientWidth = 0;
    static long cachedHwnd;
    static MemorySegment originalWndProc;
    static MethodHandle callWindowProcW, isZoomedHandle, monitorFromWindowHandle;
    static MethodHandle getMonitorInfoHandle, getWindowRectHandle, showWindowHandle, dwmSetAttrHandle;
    static MemorySegment monitorInfoBuffer, windowRectBuffer, intTrueBuffer, intFalseBuffer;

    public static void apply(long nativeHandle) {
        if (nativeHandle == 0) return;
        cachedHwnd = nativeHandle;
        try {
            var linker = Linker.nativeLinker();
            var user32 = SymbolLookup.libraryLookup("user32", Arena.global());
            var dwmapi = SymbolLookup.libraryLookup("dwmapi", Arena.global());
            MemorySegment hwnd = MemorySegment.ofAddress(nativeHandle);

            var getWindowLongPtr = downcall(linker, user32, "GetWindowLongPtrW",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            var setWindowLongPtr = downcall(linker, user32, "SetWindowLongPtrW",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
            var setWindowPos = downcall(linker, user32, "SetWindowPos",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            callWindowProcW = downcall(linker, user32, "CallWindowProcW",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
            isZoomedHandle = downcall(linker, user32, "IsZoomed",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            monitorFromWindowHandle = downcall(linker, user32, "MonitorFromWindow",
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            getMonitorInfoHandle = downcall(linker, user32, "GetMonitorInfoW",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            getWindowRectHandle = downcall(linker, user32, "GetWindowRect",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            showWindowHandle = downcall(linker, user32, "ShowWindow",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            var dwmExtendFrame = downcall(linker, dwmapi, "DwmExtendFrameIntoClientArea",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            dwmSetAttrHandle = downcall(linker, dwmapi, "DwmSetWindowAttribute",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            monitorInfoBuffer = Arena.global().allocate(40);
            windowRectBuffer = Arena.global().allocate(16);
            intTrueBuffer = Arena.global().allocate(ValueLayout.JAVA_INT, 1);
            intFalseBuffer = Arena.global().allocate(ValueLayout.JAVA_INT, 0);

            WindowProcHandler.install(linker, setWindowLongPtr, hwnd);
            long style = (long) getWindowLongPtr.invoke(hwnd, GWL_STYLE);
            setWindowLongPtr.invoke(hwnd, GWL_STYLE, style | WS_OVERLAPPEDWINDOW);
            setWindowPos.invoke(hwnd, MemorySegment.NULL, 0, 0, 0, 0,
                    SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER);

            try (var arena = Arena.ofConfined()) {
                MemorySegment margins = arena.allocate(DWM_MARGINS_SIZE);
                margins.set(ValueLayout.JAVA_INT, 0, 0);
                margins.set(ValueLayout.JAVA_INT, 4, 0);
                margins.set(ValueLayout.JAVA_INT, 8, 0);
                margins.set(ValueLayout.JAVA_INT, 12, 1);
                dwmExtendFrame.invoke(hwnd, margins);
                MemorySegment cornerPref = arena.allocate(ValueLayout.JAVA_INT, DWMWCP_ROUND);
                dwmSetAttrHandle.invoke(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, cornerPref, DWM_ATTRIBUTE_SIZE);
            }
        } catch (Throwable t) {
            System.out.println("Native window tweaks failed: " + t.getMessage());
        }
    }

    private static MethodHandle downcall(Linker linker, SymbolLookup lib, String name, FunctionDescriptor fd) {
        return linker.downcallHandle(lib.find(name).orElseThrow(), fd);
    }

    public static void minimize() { callShowWindow(SW_MINIMIZE); }
    public static void maximize() { callShowWindow(SW_MAXIMIZE); }
    public static void restore() { callShowWindow(SW_RESTORE); }

    public static void restoreQuietly() {
        if (dwmSetAttrHandle == null || cachedHwnd == 0) return;
        try {
            MemorySegment hwnd = MemorySegment.ofAddress(cachedHwnd);
            dwmSetAttrHandle.invoke(hwnd, DWMWA_TRANSITIONS_FORCEDISABLED, intTrueBuffer, DWM_ATTRIBUTE_SIZE);
            showWindowHandle.invoke(hwnd, SW_RESTORE);
            dwmSetAttrHandle.invoke(hwnd, DWMWA_TRANSITIONS_FORCEDISABLED, intFalseBuffer, DWM_ATTRIBUTE_SIZE);
        } catch (Throwable ignored) { }
    }

    private static void callShowWindow(int command) {
        if (showWindowHandle == null || cachedHwnd == 0) return;
        try { showWindowHandle.invoke(MemorySegment.ofAddress(cachedHwnd), command); }
        catch (Throwable ignored) { }
    }
}

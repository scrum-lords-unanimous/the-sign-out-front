//windows helper class for handling window procedures. Stuff like dragging windows to the edge of the screen to change their size. 
package FluentUIJavaFxKit;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class WindowProcHandler {

    private static final int WM_NCCALCSIZE = 0x0083;
    private static final int WM_NCHITTEST = 0x0084;
    private static final int WM_GETMINMAXINFO = 0x0024;
    private static final int WM_NCACTIVATE = 0x0086;
    private static final int WM_NCPAINT = 0x0085;
    private static final int GWLP_WNDPROC = -4;
    private static final int MONITOR_DEFAULTTONEAREST = 2;
    private static final int NCCALCSIZE_PARAMS_SIZE = 56;
    private static final int MINMAXINFO_SIZE = 40;
    private static final int MIN_WINDOW_WIDTH = 600;
    private static final int MIN_WINDOW_HEIGHT = 400;
    private static final int MONITOR_WORK_LEFT = 20;
    private static final int MONITOR_WORK_TOP = 24;
    private static final int MONITOR_WORK_RIGHT = 28;
    private static final int MONITOR_WORK_BOTTOM = 32;

    static void install(Linker linker, MethodHandle setWindowLongPtr, MemorySegment hwnd) throws Throwable {
        var desc = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG);
        var mh = MethodHandles.lookup().findStatic(WindowProcHandler.class, "windowProc",
                MethodType.methodType(long.class, MemorySegment.class, int.class, long.class, long.class));
        MemorySegment stub = linker.upcallStub(mh, desc, Arena.global());
        long prev = (long) setWindowLongPtr.invoke(hwnd, GWLP_WNDPROC, stub.address());
        NativeWindowHelper.originalWndProc = MemorySegment.ofAddress(prev);
    }

    static long windowProc(MemorySegment hwnd, int msg, long wParam, long lParam) {
        if (msg == WM_NCCALCSIZE) return handleNcCalcSize(hwnd, wParam, lParam);
        if (msg == WM_NCACTIVATE) return 1;
        if (msg == WM_NCPAINT) return 0;
        if (msg == WM_NCHITTEST) return HitTestHandler.handle(hwnd, lParam);
        if (msg == WM_GETMINMAXINFO) return handleGetMinMaxInfo(hwnd, msg, wParam, lParam);
        return callDefault(hwnd, msg, wParam, lParam);
    }

    private static long handleNcCalcSize(MemorySegment hwnd, long wParam, long lParam) {
        try {
            if (wParam != 0 && ((int) NativeWindowHelper.isZoomedHandle.invoke(hwnd)) != 0) {
                var monitor = (MemorySegment) NativeWindowHelper.monitorFromWindowHandle.invoke(hwnd, MONITOR_DEFAULTTONEAREST);
                NativeWindowHelper.monitorInfoBuffer.set(ValueLayout.JAVA_INT, 0, 40);
                if (((int) NativeWindowHelper.getMonitorInfoHandle.invoke(monitor, NativeWindowHelper.monitorInfoBuffer)) != 0) {
                    var params = MemorySegment.ofAddress(lParam).reinterpret(NCCALCSIZE_PARAMS_SIZE);
                    params.set(ValueLayout.JAVA_INT, 0, NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_LEFT));
                    params.set(ValueLayout.JAVA_INT, 4, NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_TOP));
                    params.set(ValueLayout.JAVA_INT, 8, NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_RIGHT));
                    params.set(ValueLayout.JAVA_INT, 12, NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_BOTTOM));
                }
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    private static long handleGetMinMaxInfo(MemorySegment hwnd, int msg, long wParam, long lParam) {
        try {
            long result = (long) NativeWindowHelper.callWindowProcW.invoke(
                    NativeWindowHelper.originalWndProc, hwnd, msg, wParam, lParam);
            var info = MemorySegment.ofAddress(lParam).reinterpret(MINMAXINFO_SIZE);
            info.set(ValueLayout.JAVA_INT, 24, MIN_WINDOW_WIDTH);
            info.set(ValueLayout.JAVA_INT, 28, MIN_WINDOW_HEIGHT);
            return result;
        } catch (Throwable ignored) { }
        return callDefault(hwnd, msg, wParam, lParam);
    }

    static long callDefault(MemorySegment hwnd, int msg, long wParam, long lParam) {
        try {
            return (long) NativeWindowHelper.callWindowProcW.invoke(
                    NativeWindowHelper.originalWndProc, hwnd, msg, wParam, lParam);
        } catch (Throwable t) { return 0; }
    }
}

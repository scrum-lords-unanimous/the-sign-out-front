package GUI;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;


public class NativeWindowHelper {

    private static final int GWL_STYLE    = -16;
    private static final int GWLP_WNDPROC = -4;
    private static final long WS_OVERLAPPEDWINDOW = 0x00CF0000L;
    private static final int SWP_FRAMECHANGED = 0x0020;
    private static final int SWP_NOMOVE       = 0x0002;
    private static final int SWP_NOSIZE       = 0x0001;
    private static final int SWP_NOZORDER     = 0x0004;

    private static final int WM_NCCALCSIZE   = 0x0083;
    private static final int WM_NCHITTEST    = 0x0084;
    private static final int WM_GETMINMAXINFO = 0x0024;
    private static final int WM_NCACTIVATE   = 0x0086;
    private static final int WM_NCPAINT     = 0x0085;

    private static final int HTCLIENT      = 1;
    private static final int HTCAPTION     = 2;
    private static final int HTLEFT        = 10;
    private static final int HTRIGHT       = 11;
    private static final int HTTOP         = 12;
    private static final int HTTOPLEFT     = 13;
    private static final int HTTOPRIGHT    = 14;
    private static final int HTBOTTOM      = 15;
    private static final int HTBOTTOMLEFT  = 16;
    private static final int HTBOTTOMRIGHT = 17;

    private static final int SW_MAXIMIZE = 3;
    private static final int SW_MINIMIZE = 6;
    private static final int SW_RESTORE  = 9;

    private static final int DWMWA_TRANSITIONS_FORCEDISABLED = 3;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE  = 33;
    private static final int DWMWCP_ROUND = 2;

    private static final int MONITOR_DEFAULTTONEAREST = 2;

    private static final int RESIZE_MARGIN       = 6;
    private static final int TITLE_BAR_HEIGHT    = 38;
    private static final int CAPTION_BUTTONS_WIDTH = 46 * 3;
    private static final int MIN_WINDOW_WIDTH  = 600;
    private static final int MIN_WINDOW_HEIGHT = 400;

    private static final int MONITOR_INFO_BUFFER_SIZE = 40;
    private static final int WINDOW_RECT_BUFFER_SIZE = 16;
    private static final int NCCALCSIZE_PARAMS_SIZE = 56;
    private static final int MINMAXINFO_SIZE = 40;
    private static final int MINMAXINFO_MIN_TRACK_X_OFFSET = 24;
    private static final int MINMAXINFO_MIN_TRACK_Y_OFFSET = 28;
    private static final int DWM_MARGINS_SIZE = 16;
    private static final int DWM_ATTRIBUTE_SIZE = 4;

    private static final int MONITOR_WORK_LEFT_OFFSET = 20;
    private static final int MONITOR_WORK_TOP_OFFSET = 24;
    private static final int MONITOR_WORK_RIGHT_OFFSET = 28;
    private static final int MONITOR_WORK_BOTTOM_OFFSET = 32;

    private static final int RECT_LEFT_OFFSET = 0;
    private static final int RECT_TOP_OFFSET = 4;
    private static final int RECT_RIGHT_OFFSET = 8;
    private static final int RECT_BOTTOM_OFFSET = 12;

    static int leftClientWidth = 0;

    private static long cachedHwnd;
    private static MemorySegment originalWndProc;
    private static MethodHandle callWindowProcW;
    private static MethodHandle isZoomedHandle;
    private static MethodHandle monitorFromWindowHandle;
    private static MethodHandle getMonitorInfoHandle;
    private static MethodHandle getWindowRectHandle;
    private static MethodHandle showWindowHandle;
    private static MethodHandle dwmSetAttrHandle;

    private static MemorySegment monitorInfoBuffer;
    private static MemorySegment windowRectBuffer;
    private static MemorySegment intTrueBuffer;
    private static MemorySegment intFalseBuffer;

    public static void apply(long nativeHandle) {
        if (nativeHandle == 0) {
            return;
        }
        cachedHwnd = nativeHandle;
        try {
            var linker = Linker.nativeLinker();
            var user32 = SymbolLookup.libraryLookup("user32", Arena.global());
            var dwmapi = SymbolLookup.libraryLookup("dwmapi", Arena.global());
            MemorySegment hwnd = MemorySegment.ofAddress(nativeHandle);

            MethodHandle getWindowLongPtr = linker.downcallHandle(
                    user32.find("GetWindowLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            MethodHandle setWindowLongPtr = linker.downcallHandle(
                    user32.find("SetWindowLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
            MethodHandle setWindowPos = linker.downcallHandle(
                    user32.find("SetWindowPos").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            callWindowProcW = linker.downcallHandle(
                    user32.find("CallWindowProcW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
            isZoomedHandle = linker.downcallHandle(
                    user32.find("IsZoomed").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            monitorFromWindowHandle = linker.downcallHandle(
                    user32.find("MonitorFromWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            getMonitorInfoHandle = linker.downcallHandle(
                    user32.find("GetMonitorInfoW").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            getWindowRectHandle = linker.downcallHandle(
                    user32.find("GetWindowRect").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            showWindowHandle = linker.downcallHandle(
                    user32.find("ShowWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            MethodHandle dwmExtendFrame = linker.downcallHandle(
                    dwmapi.find("DwmExtendFrameIntoClientArea").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            dwmSetAttrHandle = linker.downcallHandle(
                    dwmapi.find("DwmSetWindowAttribute").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            monitorInfoBuffer = Arena.global().allocate(MONITOR_INFO_BUFFER_SIZE);
            windowRectBuffer  = Arena.global().allocate(WINDOW_RECT_BUFFER_SIZE);
            intTrueBuffer  = Arena.global().allocate(ValueLayout.JAVA_INT, 1);
            intFalseBuffer = Arena.global().allocate(ValueLayout.JAVA_INT, 0);

            installCustomWindowProc(linker, setWindowLongPtr, hwnd);

            long currentStyle = (long) getWindowLongPtr.invoke(hwnd, GWL_STYLE);
            currentStyle |= WS_OVERLAPPEDWINDOW;
            setWindowLongPtr.invoke(hwnd, GWL_STYLE, currentStyle);

            setWindowPos.invoke(hwnd, MemorySegment.NULL, 0, 0, 0, 0,
                    SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER);

            extendFrameIntoClientArea(dwmExtendFrame, hwnd);
            setRoundedCorners(hwnd);
        } catch (Throwable throwable) {
            System.out.println("Native window tweaks failed: " + throwable.getMessage());
        }
    }

    private static void installCustomWindowProc(Linker linker, MethodHandle setWindowLongPtr, MemorySegment hwnd) throws Throwable {
        FunctionDescriptor windowProcDescriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG);
        MethodHandle windowProcMethodHandle = MethodHandles.lookup().findStatic(
                NativeWindowHelper.class, "windowProc",
                MethodType.methodType(long.class, MemorySegment.class, int.class, long.class, long.class));
        MemorySegment upcallStub = linker.upcallStub(windowProcMethodHandle, windowProcDescriptor, Arena.global());
        long previousWndProc = (long) setWindowLongPtr.invoke(hwnd, GWLP_WNDPROC, upcallStub.address());
        originalWndProc = MemorySegment.ofAddress(previousWndProc);
    }

    private static void extendFrameIntoClientArea(MethodHandle dwmExtendFrame, MemorySegment hwnd) throws Throwable {
        try (var arena = Arena.ofConfined()) {
            MemorySegment margins = arena.allocate(DWM_MARGINS_SIZE);
            margins.set(ValueLayout.JAVA_INT, 0, 0);
            margins.set(ValueLayout.JAVA_INT, 4, 0);
            margins.set(ValueLayout.JAVA_INT, 8, 0);
            margins.set(ValueLayout.JAVA_INT, 12, 1);
            dwmExtendFrame.invoke(hwnd, margins);
        }
    }

    private static void setRoundedCorners(MemorySegment hwnd) throws Throwable {
        try (var arena = Arena.ofConfined()) {
            MemorySegment cornerPreference = arena.allocate(ValueLayout.JAVA_INT, DWMWCP_ROUND);
            dwmSetAttrHandle.invoke(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, cornerPreference, DWM_ATTRIBUTE_SIZE);
        }
    }

    static long windowProc(MemorySegment hwnd, int message, long wParam, long lParam) {

        if (message == WM_NCCALCSIZE) {
            return handleNcCalcSize(hwnd, wParam, lParam);
        }

        if (message == WM_NCACTIVATE) {
            return 1;
        }

        if (message == WM_NCPAINT) {
            return 0;
        }

        if (message == WM_NCHITTEST) {
            return handleNcHitTest(hwnd, lParam);
        }

        if (message == WM_GETMINMAXINFO) {
            return handleGetMinMaxInfo(hwnd, message, wParam, lParam);
        }

        return callDefaultWindowProc(hwnd, message, wParam, lParam);
    }

    private static long handleNcCalcSize(MemorySegment hwnd, long wParam, long lParam) {
        try {
            if (wParam != 0) {
                int isZoomed = (int) isZoomedHandle.invoke(hwnd);
                if (isZoomed != 0) {
                    MemorySegment monitor = (MemorySegment) monitorFromWindowHandle.invoke(hwnd, MONITOR_DEFAULTTONEAREST);
                    monitorInfoBuffer.set(ValueLayout.JAVA_INT, 0, MONITOR_INFO_BUFFER_SIZE);
                    int success = (int) getMonitorInfoHandle.invoke(monitor, monitorInfoBuffer);
                    if (success != 0) {
                        MemorySegment calcSizeParams = MemorySegment.ofAddress(lParam).reinterpret(NCCALCSIZE_PARAMS_SIZE);
                        calcSizeParams.set(ValueLayout.JAVA_INT, RECT_LEFT_OFFSET, monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_LEFT_OFFSET));
                        calcSizeParams.set(ValueLayout.JAVA_INT, RECT_TOP_OFFSET, monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_TOP_OFFSET));
                        calcSizeParams.set(ValueLayout.JAVA_INT, RECT_RIGHT_OFFSET, monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_RIGHT_OFFSET));
                        calcSizeParams.set(ValueLayout.JAVA_INT, RECT_BOTTOM_OFFSET, monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_BOTTOM_OFFSET));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static long handleNcHitTest(MemorySegment hwnd, long lParam) {
        try {
            int screenX = (short) (lParam & 0xFFFF);
            int screenY = (short) ((lParam >> 16) & 0xFFFF);

            getWindowRectHandle.invoke(hwnd, windowRectBuffer);
            int windowLeft = windowRectBuffer.get(ValueLayout.JAVA_INT, RECT_LEFT_OFFSET);
            int windowTop = windowRectBuffer.get(ValueLayout.JAVA_INT, RECT_TOP_OFFSET);
            int windowRight = windowRectBuffer.get(ValueLayout.JAVA_INT, RECT_RIGHT_OFFSET);
            int windowBottom = windowRectBuffer.get(ValueLayout.JAVA_INT, RECT_BOTTOM_OFFSET);

            int localX = screenX - windowLeft;
            int localY = screenY - windowTop;
            int windowWidth = windowRight - windowLeft;
            int windowHeight = windowBottom - windowTop;
            boolean isZoomed = ((int) isZoomedHandle.invoke(hwnd)) != 0;

            if (!isZoomed) {
                long resizeResult = detectResizeEdge(localX, localY, windowWidth, windowHeight);
                if (resizeResult != HTCLIENT) {
                    return resizeResult;
                }
            }

            return detectCaptionArea(hwnd, localX, localY, windowWidth, windowLeft, windowTop, isZoomed);
        } catch (Throwable throwable) {
            return HTCLIENT;
        }
    }

    private static long detectResizeEdge(int localX, int localY, int windowWidth, int windowHeight) {
        boolean nearTop = localY < RESIZE_MARGIN;
        boolean nearBottom = localY >= windowHeight - RESIZE_MARGIN;
        boolean nearLeft = localX < RESIZE_MARGIN;
        boolean nearRight = localX >= windowWidth - RESIZE_MARGIN;

        if (nearTop && nearLeft) {
            return HTTOPLEFT;
        }
        if (nearTop && nearRight) {
            return HTTOPRIGHT;
        }
        if (nearBottom && nearLeft) {
            return HTBOTTOMLEFT;
        }
        if (nearBottom && nearRight) {
            return HTBOTTOMRIGHT;
        }
        if (nearTop) {
            return HTTOP;
        }
        if (nearBottom) {
            return HTBOTTOM;
        }
        if (nearLeft) {
            return HTLEFT;
        }
        if (nearRight) {
            return HTRIGHT;
        }
        return HTCLIENT;
    }

    private static long detectCaptionArea(MemorySegment hwnd, int localX, int localY, int windowWidth,
                                          int windowLeft, int windowTop, boolean isZoomed) throws Throwable {
        int captionTop = 0;
        int captionLeft = 0;
        int captionRight = windowWidth;

        if (isZoomed) {
            MemorySegment monitor = (MemorySegment) monitorFromWindowHandle.invoke(hwnd, MONITOR_DEFAULTTONEAREST);
            monitorInfoBuffer.set(ValueLayout.JAVA_INT, 0, MONITOR_INFO_BUFFER_SIZE);
            if (((int) getMonitorInfoHandle.invoke(monitor, monitorInfoBuffer)) != 0) {
                captionLeft  = monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_LEFT_OFFSET) - windowLeft;
                captionTop   = monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_TOP_OFFSET) - windowTop;
                captionRight = monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_RIGHT_OFFSET) - windowLeft;
            }
        }

        if (localY >= captionTop && localY < captionTop + TITLE_BAR_HEIGHT) {
            if (localX >= captionRight - CAPTION_BUTTONS_WIDTH) {
                return HTCLIENT;
            }
            if (localX < captionLeft + leftClientWidth) {
                return HTCLIENT;
            }
            return HTCAPTION;
        }
        return HTCLIENT;
    }

    private static long handleGetMinMaxInfo(MemorySegment hwnd, int message, long wParam, long lParam) {
        try {
            long result = (long) callWindowProcW.invoke(originalWndProc, hwnd, message, wParam, lParam);
            MemorySegment minMaxInfo = MemorySegment.ofAddress(lParam).reinterpret(MINMAXINFO_SIZE);
            minMaxInfo.set(ValueLayout.JAVA_INT, MINMAXINFO_MIN_TRACK_X_OFFSET, MIN_WINDOW_WIDTH);
            minMaxInfo.set(ValueLayout.JAVA_INT, MINMAXINFO_MIN_TRACK_Y_OFFSET, MIN_WINDOW_HEIGHT);
            return result;
        } catch (Throwable ignored) {
        }
        return callDefaultWindowProc(hwnd, message, wParam, lParam);
    }

    private static long callDefaultWindowProc(MemorySegment hwnd, int message, long wParam, long lParam) {
        try {
            return (long) callWindowProcW.invoke(originalWndProc, hwnd, message, wParam, lParam);
        } catch (Throwable throwable) {
            return 0;
        }
    }

    public static void minimize() {
        callShowWindow(SW_MINIMIZE);
    }

    public static void maximize() {
        callShowWindow(SW_MAXIMIZE);
    }

    public static void restore() {
        callShowWindow(SW_RESTORE);
    }

    public static void restoreQuietly() {
        if (dwmSetAttrHandle == null || cachedHwnd == 0) {
            return;
        }
        try {
            MemorySegment hwnd = MemorySegment.ofAddress(cachedHwnd);
            dwmSetAttrHandle.invoke(hwnd, DWMWA_TRANSITIONS_FORCEDISABLED, intTrueBuffer, DWM_ATTRIBUTE_SIZE);
            showWindowHandle.invoke(hwnd, SW_RESTORE);
            dwmSetAttrHandle.invoke(hwnd, DWMWA_TRANSITIONS_FORCEDISABLED, intFalseBuffer, DWM_ATTRIBUTE_SIZE);
        } catch (Throwable ignored) {
        }
    }

    private static void callShowWindow(int command) {
        if (showWindowHandle == null || cachedHwnd == 0) {
            return;
        }
        try {
            showWindowHandle.invoke(MemorySegment.ofAddress(cachedHwnd), command);
        } catch (Throwable ignored) {
        }
    }
}

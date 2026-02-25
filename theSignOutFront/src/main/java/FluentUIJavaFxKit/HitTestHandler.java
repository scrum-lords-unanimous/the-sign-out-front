//windows api stuff for moving and resizing windows. 
package FluentUIJavaFxKit;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

class HitTestHandler {

    private static final int HTCLIENT = 1;
    private static final int HTCAPTION = 2;
    private static final int HTLEFT = 10;
    private static final int HTRIGHT = 11;
    private static final int HTTOP = 12;
    private static final int HTTOPLEFT = 13;
    private static final int HTTOPRIGHT = 14;
    private static final int HTBOTTOM = 15;
    private static final int HTBOTTOMLEFT = 16;
    private static final int HTBOTTOMRIGHT = 17;
    private static final int RESIZE_MARGIN = 6;
    private static final int TITLE_BAR_HEIGHT = 38;
    private static final int CAPTION_BUTTONS_WIDTH = 46 * 3;
    private static final int MONITOR_DEFAULTTONEAREST = 2;
    private static final int MONITOR_WORK_LEFT = 20;
    private static final int MONITOR_WORK_TOP = 24;
    private static final int MONITOR_WORK_RIGHT = 28;
    private static final int MONITOR_WORK_BOTTOM = 32;

    static long handle(MemorySegment hwnd, long lParam) {
        try {
            int screenX = (short) (lParam & 0xFFFF);
            int screenY = (short) ((lParam >> 16) & 0xFFFF);

            NativeWindowHelper.getWindowRectHandle.invoke(hwnd, NativeWindowHelper.windowRectBuffer);
            int wLeft = NativeWindowHelper.windowRectBuffer.get(ValueLayout.JAVA_INT, 0);
            int wTop = NativeWindowHelper.windowRectBuffer.get(ValueLayout.JAVA_INT, 4);
            int wRight = NativeWindowHelper.windowRectBuffer.get(ValueLayout.JAVA_INT, 8);
            int wBottom = NativeWindowHelper.windowRectBuffer.get(ValueLayout.JAVA_INT, 12);

            int lx = screenX - wLeft, ly = screenY - wTop;
            int ww = wRight - wLeft, wh = wBottom - wTop;
            boolean zoomed = ((int) NativeWindowHelper.isZoomedHandle.invoke(hwnd)) != 0;

            if (!zoomed) {
                long edge = detectEdge(lx, ly, ww, wh);
                if (edge != HTCLIENT) return edge;
            }
            return detectCaption(hwnd, lx, ly, ww, wLeft, wTop, zoomed);
        } catch (Throwable t) { return HTCLIENT; }
    }

    private static long detectEdge(int lx, int ly, int w, int h) {
        boolean top = ly < RESIZE_MARGIN, bottom = ly >= h - RESIZE_MARGIN;
        boolean left = lx < RESIZE_MARGIN, right = lx >= w - RESIZE_MARGIN;
        if (top && left) return HTTOPLEFT;
        if (top && right) return HTTOPRIGHT;
        if (bottom && left) return HTBOTTOMLEFT;
        if (bottom && right) return HTBOTTOMRIGHT;
        if (top) return HTTOP;
        if (bottom) return HTBOTTOM;
        if (left) return HTLEFT;
        if (right) return HTRIGHT;
        return HTCLIENT;
    }

    private static long detectCaption(MemorySegment hwnd, int lx, int ly, int ww,
                                      int wLeft, int wTop, boolean zoomed) throws Throwable {
        int capTop = 0, capLeft = 0, capRight = ww;
        if (zoomed) {
            var monitor = (MemorySegment) NativeWindowHelper.monitorFromWindowHandle.invoke(hwnd, MONITOR_DEFAULTTONEAREST);
            NativeWindowHelper.monitorInfoBuffer.set(ValueLayout.JAVA_INT, 0, 40);
            if (((int) NativeWindowHelper.getMonitorInfoHandle.invoke(monitor, NativeWindowHelper.monitorInfoBuffer)) != 0) {
                capLeft = NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_LEFT) - wLeft;
                capTop = NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_TOP) - wTop;
                capRight = NativeWindowHelper.monitorInfoBuffer.get(ValueLayout.JAVA_INT, MONITOR_WORK_RIGHT) - wLeft;
            }
        }
        if (ly >= capTop && ly < capTop + TITLE_BAR_HEIGHT) {
            if (lx >= capRight - CAPTION_BUTTONS_WIDTH) return HTCLIENT;
            if (lx < capLeft + NativeWindowHelper.leftClientWidth) return HTCLIENT;
            return HTCAPTION;
        }
        return HTCLIENT;
    }
}

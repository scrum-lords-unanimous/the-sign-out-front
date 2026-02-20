package GUI.Run;

import Data.Slide.Slide;
import java.util.List;

public class SlideViewTracker {

    private final List<Slide> slides;
    private final int[] counts;

    public SlideViewTracker(List<Slide> slides) {
        this.slides = slides;
        this.counts = new int[slides.size()];
    }

    public void recordView(int idx) {
        if (idx >= 0 && idx < counts.length) counts[idx]++;
    }

    public void reset() {
        for (int i = 0; i < counts.length; i++) counts[i] = 0;
    }

    public int[] getRankOrder() {
        Integer[] indices = new Integer[counts.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        java.util.Arrays.sort(indices, (a, b) -> counts[b] - counts[a]);
        int[] result = new int[indices.length];
        for (int i = 0; i < indices.length; i++) result[i] = indices[i];
        return result;
    }

    public int getCount(int i) { return counts[i]; }
    public String getName(int i) { return slides.get(i).getName(); }
    public int total() { int t = 0; for (int c : counts) t += c; return t; }
    public int size() { return slides.size(); }
}

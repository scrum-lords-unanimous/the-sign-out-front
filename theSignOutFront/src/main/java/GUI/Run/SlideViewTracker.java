package GUI.Run;

import Data.Slide.Slide;
import java.util.List;

public class SlideViewTracker {

    private final List<Slide> slides;
    private final int[] viewCounts;

    public SlideViewTracker(List<Slide> slides) {
        this.slides = slides;
        this.viewCounts = new int[slides.size()];
    }

    public void recordView(int slideIndex) {
        if (slideIndex >= 0 && slideIndex < viewCounts.length) {
            viewCounts[slideIndex]++;
        }
    }

    public void reset() {
        for (int index = 0; index < viewCounts.length; index++) {
            viewCounts[index] = 0;
        }
    }

    public int[] getRankOrder() {
        Integer[] indices = new Integer[viewCounts.length];
        for (int index = 0; index < indices.length; index++) {
            indices[index] = index;
        }
        java.util.Arrays.sort(indices, (firstIndex, secondIndex) ->
            viewCounts[secondIndex] - viewCounts[firstIndex]);
        int[] sortedIndices = new int[indices.length];
        for (int index = 0; index < indices.length; index++) {
            sortedIndices[index] = indices[index];
        }
        return sortedIndices;
    }

    public int getCount(int slideIndex) {
        return viewCounts[slideIndex];
    }

    public String getName(int slideIndex) {
        return slides.get(slideIndex).getName();
    }

    public String getId(int slideIndex) {
        return slides.get(slideIndex).getId();
    }

    public int total() {
        int totalViews = 0;
        for (int count : viewCounts) {
            totalViews += count;
        }
        return totalViews;
    }

    public int size() {
        return slides.size();
    }
}

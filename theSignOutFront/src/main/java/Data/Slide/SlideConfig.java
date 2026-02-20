package Data.Slide;

import java.util.List;

public class SlideConfig {

    private int cycleDuration;
    private List<Slide> slides;

    public int getCycleDuration() { return cycleDuration; }
    public List<Slide> getSlides() { return slides; }
    public void setCycleDuration(int cycleDuration) { this.cycleDuration = cycleDuration; }
    public void setSlides(List<Slide> slides) { this.slides = slides; }

}

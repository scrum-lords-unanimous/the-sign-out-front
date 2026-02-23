package GUI.Assets;

import Data.Slide.SlideConfig;
import GUI.Components.EmptyState;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AssetsPage {

    private static final double SPACING = 12;
    private static final double GRID_HORIZONTAL_GAP = 12;
    private static final double GRID_VERTICAL_GAP = 12;

    private AssetsToolbar toolbar;
    private FlowPane slideGrid;
    private StackPane slideGridContainer;

    public VBox build(Label pageTitle) {
        SlideSetStore.ensureDefaults();

        toolbar = new AssetsToolbar(this::rebuildGrid);
        slideGrid = new FlowPane(GRID_HORIZONTAL_GAP, GRID_VERTICAL_GAP);
        slideGridContainer = new StackPane(slideGrid);
        rebuildGrid();

        VBox page = new VBox(SPACING, pageTitle, toolbar, slideGridContainer);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }

    private void rebuildGrid() {
        slideGrid.getChildren().clear();
        slideGridContainer.getChildren().clear();

        String selectedSetName = toolbar.getSelectedSet();
        if (selectedSetName == null) {
            return;
        }

        SlideConfig slideConfig = SlideSetStore.loadSet(selectedSetName);
        slideGridContainer.getChildren().add(slideGrid);

        if (slideConfig != null && slideConfig.getSlides() != null) {
            for (var slide : slideConfig.getSlides()) {
                slideGrid.getChildren().add(new SlideCard(slide, selectedSetName, this::rebuildGrid));
            }
        }

        slideGrid.getChildren().add(new UploadCard(selectedSetName, this::rebuildGrid));
    }
}

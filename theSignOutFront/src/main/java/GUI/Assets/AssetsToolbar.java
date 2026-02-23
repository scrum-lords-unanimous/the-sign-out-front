package GUI.Assets;

import Data.Slide.SlideConfig;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.ArrayList;

public class AssetsToolbar extends HBox {

    private static final double TOOLBAR_SPACING = 8;
    private static final int DEFAULT_CYCLE_DURATION = 10;

    private final ComboBox<String> setSelector;

    public AssetsToolbar(Runnable onSetChanged) {
        super(TOOLBAR_SPACING);
        setAlignment(Pos.CENTER_LEFT);

        setSelector = new ComboBox<>();
        refreshSetList();
        setSelector.setValue(SlideSetStore.getActiveSet());

        Button newSetButton = new Button("New Set");
        newSetButton.setOnAction(event -> {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("New Slide Set");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Set name:");
            nameDialog.showAndWait().ifPresent(enteredName -> {
                if (enteredName.isBlank()) {
                    return;
                }
                SlideConfig newConfig = new SlideConfig();
                newConfig.setCycleDuration(DEFAULT_CYCLE_DURATION);
                newConfig.setSlides(new ArrayList<>());
                SlideSetStore.saveSet(enteredName, newConfig);
                refreshSetList();
                setSelector.setValue(enteredName);
            });
        });

        Button deleteSetButton = new Button("Delete Set");
        deleteSetButton.setOnAction(event -> {
            String selectedName = setSelector.getValue();
            if (selectedName == null) {
                return;
            }
            SlideSetStore.deleteSet(selectedName);
            refreshSetList();
            if (!setSelector.getItems().isEmpty()) {
                setSelector.setValue(setSelector.getItems().get(0));
            }
        });

        setSelector.setOnAction(event -> {
            String selectedName = setSelector.getValue();
            if (selectedName != null) {
                SlideSetStore.setActiveSet(selectedName);
                onSetChanged.run();
            }
        });

        getChildren().addAll(setSelector, newSetButton, deleteSetButton);
    }

    public String getSelectedSet() {
        return setSelector.getValue();
    }

    public void refreshSetList() {
        String previousSelection = setSelector.getValue();
        setSelector.getItems().setAll(SlideSetStore.listSets());
        if (previousSelection != null && setSelector.getItems().contains(previousSelection)) {
            setSelector.setValue(previousSelection);
        }
    }
}

package GUI.SimEditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StepDefaults {

    private static final int DEFAULT_FOR_FROM = 0;
    private static final int DEFAULT_FOR_TO = 10;
    private static final int DEFAULT_FOR_STEP = 1;

    private StepDefaults() {}

    public static void initDefaults(StepModel step) {
        switch (step.getType()) {
            case "for":
                step.setProperty("var", "i");
                step.setProperty("from", DEFAULT_FOR_FROM);
                step.setProperty("to", DEFAULT_FOR_TO);
                step.setProperty("step", DEFAULT_FOR_STEP);
                break;
            case "for-each":
                step.setProperty("collection", "");
                step.setProperty("as", "item");
                break;
            case "while":
                Map<String, Object> whileCondition = new LinkedHashMap<>();
                List<Object> whileOperands = new ArrayList<>();
                whileOperands.add("a");
                whileOperands.add("b");
                whileCondition.put("neq", whileOperands);
                step.setProperty("condition", whileCondition);
                break;
            case "if":
                Map<String, Object> ifCondition = new LinkedHashMap<>();
                List<Object> ifOperands = new ArrayList<>();
                ifOperands.add("a");
                ifOperands.add("b");
                ifCondition.put("=", ifOperands);
                step.setProperty("condition", ifCondition);
                break;
            case "set":
                step.setProperty("target", "");
                step.setProperty("value", "");
                break;
            case "print":
                step.setProperty("value", "");
                break;
            case "spawn":
                step.setProperty("count", "1");
                step.setProperty("as", "");
                break;
        }
    }
}

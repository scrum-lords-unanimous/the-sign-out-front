package GUI.SimEditor;

import java.util.List;
import java.util.Map;

public class NodeUtils {

    private NodeUtils() {}

    public static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map || value instanceof List) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(value);
            } catch (Exception serializationException) {
                return value.toString();
            }
        }
        return value.toString();
    }

    public static Object parseValue(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
        }
        if (text.equals("true")) {
            return true;
        }
        if (text.equals("false")) {
            return false;
        }
        return text;
    }

    public static String compactExpr(Object expression) {
        if (expression instanceof Map) {
            Map<?, ?> expressionMap = (Map<?, ?>) expression;
            if (expressionMap.containsKey("op")) {
                return expressionMap.get("op") + "(...)";
            }
        }
        return stringify(expression);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}

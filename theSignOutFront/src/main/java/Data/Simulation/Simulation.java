package Simulation;

import CLI.InputConfiguration.JsonParser;
import CLI.InputConfiguration.Operations.Operation;
import CLI.UserInput.DoMath.MathIsDone;
import Data.Driveway.Driveway;
import Data.Slide.Slide;
import Data.Slide.SlideConfig;
import Data.Student.Student;

import java.util.*;

@SuppressWarnings("unchecked")
public class Simulation {

Map<String, Object> answers;
Map<String, Double> preComputed;
Map<String, Operation> operations;
Map<String, List<Student>> collections = new HashMap<>();
Map<String, Object> vars = new HashMap<>();
Map<String, Student> currentRefs = new HashMap<>();
MathIsDone math = new MathIsDone();
Random rand = new Random();
Driveway driveway;
SlideConfig slideConfig;
Map<String, Integer> slideViews = new LinkedHashMap<>();

public void theSimulation(Map<String, Object> answers, Map<String, Double> preComputed, Map<String, Operation> operations, Map<String, Object> simConfig) {
    this.answers = answers;
    this.preComputed = preComputed;
    this.operations = operations;
    try {
	slideConfig = JsonParser.parseSlideJSON();
	for (Slide s : slideConfig.getSlides()) slideViews.put(s.getId(), 0);
    } catch (Exception e) {
	throw new RuntimeException("Failed to load slides", e);
    }
    executeSteps((List<Map<String, Object>>) simConfig.get("steps"));
}

private void executeSteps(List<Map<String, Object>> steps) {
    for (Map<String, Object> step : steps) {
	executeStep(step);
    }
}

private void executeStep(Map<String, Object> step) {
    switch ((String) step.get("type")) {
	case "for" -> executeFor(step);
	case "for-each" -> executeForEach(step);
	case "if" -> executeIf(step);
	case "set" -> executeSet(step);
	case "print" -> System.out.println(resolve(step.get("value")));
	case "spawn" -> executeSpawn(step);
	case "precompute-driveway" -> executePrecomputeDriveway(step);
	case "record-slide-view" -> executeRecordSlideView();
	case "slide-report" -> executeSlideReport();
    }
}

private void executeFor(Map<String, Object> step) {
    String var = (String) step.get("var");
    int from = toInt(resolve(step.get("from")));
    int to = toInt(resolve(step.get("to")));
    int inc = step.containsKey("step") ? toInt(resolve(step.get("step"))) : 1;
    if (inc < 1) inc = 1;
    List<Map<String, Object>> steps = (List<Map<String, Object>>) step.get("steps");
    for (int i = from; i < to; i += inc) {
	vars.put(var, i);
	executeSteps(steps);
    }
}

private void executeForEach(Map<String, Object> step) {
    String collection = (String) step.get("collection");
    String as = (String) step.get("as");
    List<Map<String, Object>> steps = (List<Map<String, Object>>) step.get("steps");
    List<Student> list = collections.get(collection);
    if (list == null) return;
    for (Student s : list) {
	currentRefs.put(as, s);
	executeSteps(steps);
    }
    currentRefs.remove(as);
}

private void executeIf(Map<String, Object> step) {
    if (evaluateCondition(step.get("condition"))) {
	executeSteps((List<Map<String, Object>>) step.get("then"));
    } else if (step.containsKey("else")) {
	executeSteps((List<Map<String, Object>>) step.get("else"));
    }
}

private void executeSet(Map<String, Object> step) {
    setField((String) step.get("target"), resolve(step.get("value")));
}

private void executeSpawn(Map<String, Object> step) {
    int count = toInt(resolve(step.get("count")));
    String as = (String) step.get("as");
    List<Student> students = new ArrayList<>();
    int minHours = toInt(answers.get("min-hours-on-campus"));
    int maxHours = toInt(answers.get("max-hours-on-campus"));
    int avgDays = toInt(answers.get("avg-spent-days"));
    String[] allDays = ((String) answers.get("most-populous-days")).split(",\\s*");

    for (int i = 0; i < count; i++) {
	int arrHour = minHours + rand.nextInt(Math.max(1, maxHours - minHours));
	int arrMin = rand.nextInt(60);
	int daysOnCampus = Math.min(avgDays, allDays.length);
	String[] schedule = new String[daysOnCampus];
	System.arraycopy(allDays, 0, schedule, 0, daysOnCampus);
	Student st = new Student(schedule, arrHour, arrMin);

	for (Operation op : operations.values()) {
	    if (op.isPreCompute() && preComputed.containsKey(op.getId())) {
		double base = preComputed.get(op.getId());
		double temp = op.getTemp();
		if (temp > 0) {
		    double variance = base * (temp / 100.0);
		    st.properties.put(op.getId(), base + (rand.nextDouble() * 2 - 1) * variance);
		} else {
		    st.properties.put(op.getId(), base);
		}
	    }
	}
	students.add(st);
    }
    collections.put(as, students);
}

private void executePrecomputeDriveway(Map<String, Object> step) {
    try {
	Map<String, Object> config = JsonParser.parseDrivewayJSON();
	double length = toDouble(answers.get((String) config.get("length")));
	int curveCount = toInt(answers.get((String) config.get("curveCount")));
	double avgRadius = toDouble(answers.get((String) config.get("avgRadius")));
	double straightSpeed = toDouble(answers.get((String) config.get("straightSpeed")));
	double carWeight = toDouble(answers.get((String) config.get("carWeight")));
	driveway = Driveway.build(length, curveCount, avgRadius, straightSpeed, carWeight, math);
    } catch (Exception e) {
	throw new RuntimeException("Failed to precompute driveway", e);
    }
}

private void executeRecordSlideView() {
    int tick = toInt(vars.get("tick"));
    List<Slide> slides = slideConfig.getSlides();
    if (slides.isEmpty()) return;
    int idx = (tick / slideConfig.getCycleDuration()) % slides.size();
    slideViews.merge(slides.get(idx).getId(), 1, Integer::sum);
}

private void executeSlideReport() {
    List<Slide> slides = slideConfig.getSlides();
    Map<String, String> nameMap = new HashMap<>();
    for (Slide s : slides) nameMap.put(s.getId(), s.getName());
    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(slideViews.entrySet());
    sorted.sort((a, b) -> b.getValue() - a.getValue());
    System.out.println("\n=== SLIDE REPORT ===");
    for (Map.Entry<String, Integer> e : sorted) {
	System.out.println(e.getKey() + " (" + nameMap.get(e.getKey()) + ") - " + e.getValue() + " views");
    }
    System.out.println("Most viewed: " + sorted.get(0).getKey() + " (" + nameMap.get(sorted.get(0).getKey()) + ")");
    System.out.println("Least viewed: " + sorted.get(sorted.size() - 1).getKey() + " (" + nameMap.get(sorted.get(sorted.size() - 1).getKey()) + ")");
}

Object resolve(Object value) {
    if (value == null) return null;
    if (value instanceof Number || value instanceof Boolean) return value;
    if (value instanceof Map map) {
	if (map.containsKey("op")) {
	    String op = (String) map.get("op");
	    List<Object> args = (List<Object>) map.get("args");
	    if (op.equals("DRIVEWAY_SPEED")) {
		return driveway.speedAt(toDouble(resolve(args.get(0))));
	    }
	    double in1 = !args.isEmpty() ? toDouble(resolve(args.get(0))) : 0;
	    double in2 = args.size() > 1 ? toDouble(resolve(args.get(1))) : 0;
	    double in3 = args.size() > 2 ? toDouble(resolve(args.get(2))) : 0;
	    return math.ComputeOperatives(in1, in2, in3, op);
	}
    }
    if (value instanceof String path) {
	return resolvePath(path);
    }
    return value;
}

private Object resolvePath(String path) {
    String[] parts = path.split("\\.", 2);
    String root = parts[0];

    if (root.equals("answers") && parts.length > 1) return answers.get(parts[1]);
    if (root.equals("preComputed") && parts.length > 1) return preComputed.get(parts[1]);

    if (currentRefs.containsKey(root) && parts.length > 1) {
	Student s = currentRefs.get(root);
	String field = parts[1];
	if (field.startsWith("props.")) return s.properties.getOrDefault(field.substring(6), 0.0);
	return switch (field) {
	    case "position" -> s.position;
	    case "onDriveway" -> s.onDriveway;
	    case "arrivalHour" -> s.arrivalHour;
	    case "arrivalMinute" -> s.arrivalMintues;
	    default -> s.properties.getOrDefault(field, 0.0);
	};
    }

    if (vars.containsKey(root)) {
	if (parts.length == 1) return vars.get(root);
	int val = toInt(vars.get(root));
	return switch (parts[1]) {
	    case "hour" -> val / 3600;
	    case "minute" -> (val % 3600) / 60;
	    case "second" -> val % 60;
	    default -> vars.get(root);
	};
    }

    return path;
}

private void setField(String target, Object value) {
    String[] parts = target.split("\\.", 2);
    if (currentRefs.containsKey(parts[0]) && parts.length > 1) {
	Student s = currentRefs.get(parts[0]);
	String field = parts[1];
	if (field.startsWith("props.")) {
	    s.properties.put(field.substring(6), toDouble(value));
	    return;
	}
	switch (field) {
	    case "position" -> s.position = toDouble(value);
	    case "onDriveway" -> s.onDriveway = (boolean) value;
	    case "arrivalHour" -> s.arrivalHour = toInt(value);
	    case "arrivalMinute" -> s.arrivalMintues = toInt(value);
	}
    } else {
	vars.put(target, value);
    }
}

/**
 * <h1>evaluateCondition()</h1>
 * <h3>Summary: </h3>
 * Evaluates a condition in Simulation.json
 * <h6></h6>
 * <table>
 *     <tr>
 *         <td><u><strong>Condition</strong></u></td>
 *         <td><u><strong>What it does</strong></u></td>
 *     </tr>
 *     <tr>
 *         <td>{@code eq}</td>
 *         <td>Equals: checks if it is equal to x</td>
 *     </tr>
 *     <tr>
 *         <td>{@code neq}</td>
 *         <td>Not Equal: checks if not equal to x</td>
 *     </tr>
 *     <tr>
 *         <td>{@code gt}‎ ‎ ‎ ‎ ‎ ‎ ‎ ‎‎ ‎ ‎ ‎ ‎ ‎ ‎ ‎ </td>
 *         <td>Greater Than: check if greater than x</td>
 *     </tr>
 *     <tr>
 *         <td>{@code gte}</td>
 *         <td>Greater Than or Equal To: check if greater than or equal to x</td>
 *     </tr>
 *     <tr>
 *         <td>{@code lt}</td>
 *         <td>Less Than: check if less than x</td>
 *     </tr>
 *     <tr>
 *         <td>{@code lte}</td>
 *         <td>Less Than or Equal To: check if less than or equal to x</td>
 *     </tr>
 *     <tr>
 *         <td>{@code and}</td>
 *         <td>And: checks if all conditions are true</td>
 *     </tr>
 *     <tr>
 *         <td>{@code or}</td>
 *         <td>Or: checks if at least one condition is true</td>
 *     </tr>
 * </table>
 * <h6></h6>
 *
 * @param condition The condition object to evaluate.
 * @return True if the condition is met, false otherwise.
 */

private boolean evaluateCondition(Object condition) {
    if (!(condition instanceof Map map)) return false;
    if (map.containsKey("=")) return compareOp((List<Object>) map.get("="), 0);
    if (map.containsKey("neq")) return !compareOp((List<Object>) map.get("neq"), 0);
    if (map.containsKey(">")) return compareOp((List<Object>) map.get(">"), 1);
    if (map.containsKey("ge")) return compareOp((List<Object>) map.get("ge"), 2);
    if (map.containsKey("<")) return compareOp((List<Object>) map.get("<"), -1);
    if (map.containsKey("le")) return compareOp((List<Object>) map.get("le"), -2);
    if (map.containsKey("and")) return ((List<Object>) map.get("and")).stream().allMatch(this::evaluateCondition);
    if (map.containsKey("or")) return ((List<Object>) map.get("or")).stream().anyMatch(this::evaluateCondition);
    return false;
}

private boolean compareOp(List<Object> args, int op) {
    Object left = resolve(args.get(0));
    Object right = resolve(args.get(1));
    if (op == 0) {
	if (left instanceof Number) left = ((Number) left).doubleValue();
	if (right instanceof Number) right = ((Number) right).doubleValue();
	return Objects.equals(left, right);
    }
    return switch (op) {
	case 1 -> toDouble(left) > toDouble(right);
	case 2 -> toDouble(left) >= toDouble(right);
	case -1 -> toDouble(left) < toDouble(right);
	case -2 -> toDouble(left) <= toDouble(right);
	default -> false;
    };
}

private double toDouble(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    if (o instanceof Boolean b) return b ? 1 : 0;
    return 0;
}

private int toInt(Object o) {
    if (o instanceof Number n) return n.intValue();
    return 0;
}
}

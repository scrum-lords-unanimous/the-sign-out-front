package CLI.UserInput.RealOutput;

import CLI.InputConfiguration.JsonParser;
import CLI.InputConfiguration.Operations.Operation;
import CLI.InputConfiguration.Questions.Question;
import CLI.UserInput.DoMath.MathIsDone;
import CLI.UserInput.RealInput.Input;
import Simulation.Simulation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Output {

    Input input = new Input();
    MathIsDone math = new MathIsDone();

    String RED = "\u001B[0;31m";
    String YELLOW = "\u001B[0;33m";
    String RESET = "\u001B[0m";
    String dividerART = YELLOW + "===---===---===---===---===---===" + RESET;
    String smallDividerArt = RED + "===---===" + RESET;

    public void makeDividerFirst() {
        System.out.println(dividerART);
    }

    public void makeDividerLast() {
        System.out.println("\n" + dividerART);
    }

    public void questionCopy(int iteration, Question q) {
        System.out.println(
            "\n" +
                smallDividerArt +
                YELLOW +
                "{ INPUT #" +
                iteration +
                " }" +
                RESET +
                smallDividerArt
        );
        System.out.println("");
        System.out.println(q.getTextContent() + ":");
        System.out.println("");
    }

    public Map<String, Object> questionsPrint(Map<String, Question> questions) {
        Map<String, Object> answers = new HashMap<>();

        if (questions != null) {
            int count = 0;
            for (Map.Entry<String, Question> entry : questions.entrySet()) {
                Question q = entry.getValue();
                count++;
                questionCopy(count, q);
                Input.FunctionResult result = input.makeInput(q.getType(), q.getFormat());
                switch (q.getType()) {
                    case "STRING" -> answers.put(q.getId(), result.message());
                    case "INTEGER" -> answers.put(q.getId(), result.code());
                    case "BOOLEAN" -> answers.put(q.getId(), result.success());
                }
            }
        } else {
            System.out.println("No questions found in the JSON configuration.");
        }

        return answers;
    }

    public Map<String, Double> processOperations(Map<String, Object> answers)
        throws Exception {
        Map<String, Operation> operations = JsonParser.parseOperationJSON();
        Map<String, Double> preComputed = new HashMap<>();
        if (operations == null) return preComputed;

        for (Map.Entry<String, Operation> entry : operations.entrySet()) {
            Operation op = entry.getValue();
            java.util.List<String> operands = op.getOperAnds();

            double in1 = operands.size() > 0
                ? ((Number) answers.getOrDefault(operands.get(0), 0)).doubleValue() : 0;
            double in2 = operands.size() > 1
                ? ((Number) answers.getOrDefault(operands.get(1), 0)).doubleValue() : 0;
            double in3 = operands.size() > 2
                ? ((Number) answers.getOrDefault(operands.get(2), 0)).doubleValue() : 0;

            double result = math.ComputeOperatives(in1, in2, in3, op.getOperation());

            if (op.isPreCompute()) {
                preComputed.put(op.getId(), result);
            }

            System.out.println(op.getId() + ": " + result);
        }

        return preComputed;
    }

    private void saveAnswers(Map<String, Object> answers) throws Exception {
        new ObjectMapper().writeValue(new File("saved-answers.json"), answers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadAnswers() throws Exception {
        return new ObjectMapper().readValue(new File("saved-answers.json"), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDefaultAnswers() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/JsonRoot/InputQuestions/DefaultAnswers.json")) {
            return mapper.readValue(is, Map.class);
        }
    }

    public void makeOutput() throws Exception {
        JsonParser jsonParser = new JsonParser();
        Map<String, Object> answers;

        System.out.println("\n1. New simulation\n2. Run saved simulation\n3. Run default simulation\n");
        String choice = input.stringInfo(null).trim();

        if (choice.equals("2")) {
            File saved = new File("saved-answers.json");
            if (saved.exists()) {
                answers = loadAnswers();
            } else {
                System.out.println("No saved simulation found. Running questions.\n");
                makeDividerFirst();
                answers = questionsPrint(jsonParser.parseQuestionJSON());
                saveAnswers(answers);
            }
        } else if (choice.equals("3")) {
            answers = loadDefaultAnswers();
        } else {
            makeDividerFirst();
            answers = questionsPrint(jsonParser.parseQuestionJSON());
            saveAnswers(answers);
        }

        System.out.println("\n" + dividerART);
        System.out.println("\n" + smallDividerArt + YELLOW + "{ RESULTS }" + RESET + smallDividerArt);
        System.out.println("\n");
        Map<String, Double> preComputed = processOperations(answers);

        Map<String, Operation> operations = JsonParser.parseOperationJSON();
        Map<String, Object> simConfig = JsonParser.parseSimulationJSON();
        Simulation sim = new Simulation();
        sim.theSimulation(answers, preComputed, operations, simConfig);
        makeDividerLast();

    }
}

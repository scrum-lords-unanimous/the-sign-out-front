package CLI.InputConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import CLI.InputConfiguration.Questions.QuestionsConfig;
import CLI.InputConfiguration.Questions.Question;

public class JSONParser {

    public static void parseJSON() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Ensure this file is located in: src/main/resources/JsonRoot/InputQuestions/Questions.json
        String resourcePath = "/JsonRoot/InputQuestions/Questions.json";

        try (InputStream is = JSONParser.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }

            QuestionsConfig config = mapper.readValue(is, QuestionsConfig.class);

            // --- FOR LOOP ADDED BELOW ---
            Map<String, Question> questions = config.getQuestions();

            if (questions != null) {
                for (Map.Entry<String, Question> entry : questions.entrySet()) {
                    String key = entry.getKey();
                    Question q = entry.getValue();

                    // Printing the question key and the text content
                    System.out.println("Question Key: " + key);
                    System.out.println("  Text: " + q.getTextContent());
                    System.out.println("  Type: " + q.getType());
                    System.out.println("  Required: " + q.isRequired());
                    System.out.println("---------------------------");
                }
            } else {
                System.out.println("No questions found in the JSON configuration.");
            }
        }
    }
}
package CLI.InputConfiguration;

import CLI.InputConfiguration.Operations.Operation;
import CLI.InputConfiguration.Operations.OperationsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import CLI.InputConfiguration.Questions.QuestionsConfig;
import CLI.InputConfiguration.Questions.Question;

public class JsonParser {

    /**
     * Parses the Questions.json file located in the classpath resources.
     *
     * @return A Map where keys are question IDs and values are Question objects.
     * @throws Exception If the file is not found or parsing fails.
     *
     * */

    public static Map<String, Question> parseQuestionJSON() throws Exception {

        Map<String, Question> questions;
        ObjectMapper mapper = new ObjectMapper();
        String resourcePath = "/JsonRoot/InputQuestions/Questions.json";

        try (InputStream inputStream = JsonParser.class.getResourceAsStream(resourcePath)) {

            if (inputStream == null) {

                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);

            }

            QuestionsConfig config = mapper.readValue(inputStream, QuestionsConfig.class);
            questions = config.getQuestions();

        }

        return questions;

    }

    public static Map<String, Operation> parseOperationJSON() throws Exception {

        Map<String, Operation> operations;
        ObjectMapper mapper = new ObjectMapper();
        String resourcePath = "/JsonRoot/Operations/Operations.json";

        try (InputStream inputStream = JsonParser.class.getResourceAsStream(resourcePath)) {

            if (inputStream == null) {

                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);

            }

            OperationsConfig config = mapper.readValue(inputStream, OperationsConfig.class);
            operations = config.getOperations();

        }

        return operations;
}

}
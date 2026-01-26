package CLI.InputConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import CLI.InputConfiguration.Questions.QuestionsConfig;
import CLI.InputConfiguration.Questions.Question;

public class JSONParser {

    public static Map<String, Question> parseJSON() throws Exception {
        Map<String, Question> questions;

        ObjectMapper mapper = new ObjectMapper();

        String resourcePath = "/JsonRoot/InputQuestions/Questions.json";

        // obviously "is" is (not this one though, that's just the old is) an abrevviation of "Input Stream"
        try (InputStream is = JSONParser.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }

            QuestionsConfig config = mapper.readValue(is, QuestionsConfig.class);

            questions = config.getQuestions();


        }

        return questions;


    }
}
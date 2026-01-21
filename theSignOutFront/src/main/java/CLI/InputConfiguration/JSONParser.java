package CLI.InputConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import CLI.InputConfiguration.Questions.QuestionsConfig;
import CLI.InputConfiguration.Questions.Question;

public class JSONParser {

    public static void parseJSON() throws Exception {

        String RED = "\u001B[0;31m";
        String YELLOW = "\u001B[0;33m";
        String RESET  = "\u001B[0m";

        String dividerART = YELLOW + "===---===---===---===---===---===" + RESET;

        String smallDividerArt = RED + "===---==="  + RESET;



        ObjectMapper mapper = new ObjectMapper();

        String resourcePath = "/JsonRoot/InputQuestions/Questions.json";

        try (InputStream is = JSONParser.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }

            QuestionsConfig config = mapper.readValue(is, QuestionsConfig.class);

            // --- FOR LOOP ADDED BELOW ---
            Map<String, Question> questions = config.getQuestions();

            if (questions != null) {

                System.out.println("");

                System.out.println(dividerART);

                System.out.println("");



                int count = 0;


                for (Map.Entry<String, Question> entry : questions.entrySet()) {
                    String key = entry.getKey();
                    Question q = entry.getValue();
                    count = count + 1;

                    // Printing the question key and the text content
                    System.out.println( smallDividerArt + YELLOW + "{ INPUT #" + count + " }" + RESET + smallDividerArt);
                    System.out.println("");
                    System.out.println(q.getTextContent() + ":");
                    System.out.println("");
                }

                System.out.println(dividerART);

            } else {
                System.out.println("No questions found in the JSON configuration.");
            }
        }
    }
}
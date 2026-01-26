package CLI.UserInput.RealOutput;

import CLI.InputConfiguration.Questions.Question;
import CLI.InputConfiguration.JSONParser;
import CLI.InputConfiguration.Questions.QuestionsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import CLI.InputConfiguration.Questions.Question;

public class Output {

    /*
     * Formatting and ANSI Color Vars
     * */
    String RED = "\u001B[0;31m";
    String YELLOW = "\u001B[0;33m";
    String RESET = "\u001B[0m";
    String dividerART = YELLOW + "===---===---===---===---===---===" + RESET;
    String smallDividerArt = RED + "===---===" + RESET;


    public void makeDivider() {


        System.out.println("");
        System.out.println(dividerART);
        System.out.println("");

    }

    public void questionCopy(int iteration, Question q) {

        System.out.println(smallDividerArt + YELLOW + "{ INPUT #" + iteration + " }" + RESET + smallDividerArt);
        System.out.println("");
        System.out.println(q.getTextContent() + ":");
        System.out.println("");

    }

    public void questionsPrint(Map<String, Question> questions) {
        if (questions != null) {

            int count = 0;

            for (Map.Entry<String, Question> entry : questions.entrySet()) {
                String key = entry.getKey();
                Question q = entry.getValue();
                count = count + 1;

                questionCopy(count, q);
            }
        }

        else {

            System.out.println("No questions found in the JSON configuration.");

        }
    }

    public void makeOutput() throws Exception {
        JSONParser jsonParser = new JSONParser();

        makeDivider();

        questionsPrint(jsonParser.parseJSON());

        makeDivider();

    }

}

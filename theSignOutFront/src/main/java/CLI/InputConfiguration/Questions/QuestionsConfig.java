package CLI.InputConfiguration.Questions;

import java.util.Map;

public class QuestionsConfig {
    private Map<String, Question> questions;


    public Map<String, Question> getQuestions() {
        return questions; // Return the variable 'questions'
    }

    public void setQuestions(Map<String, Question> questions) {
        this.questions = questions;
    }
}
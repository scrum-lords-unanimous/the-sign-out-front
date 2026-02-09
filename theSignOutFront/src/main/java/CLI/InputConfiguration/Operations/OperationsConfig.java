package CLI.InputConfiguration.Operations;

import CLI.InputConfiguration.Questions.Question;

import java.util.Map;

public class OperationsConfig {
private Map<String, Operation> operation;


public Map<String, Operation> getOperations() {
    return operation; // Return the variable 'questions'
}

public void setQuestions(Map<String, Operation> operation) {
    this.operation = operation;
}
}

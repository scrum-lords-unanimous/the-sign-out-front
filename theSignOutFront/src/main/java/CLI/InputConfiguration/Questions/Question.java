package CLI.InputConfiguration.Questions;

public class Question {

    private String textContent;
    private String id;
    private String type;
    private boolean required;
    private String format;

    public String getId() { return id; }

    public String getTextContent() {
        return textContent;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getFormat() {
        return format;
    }

}

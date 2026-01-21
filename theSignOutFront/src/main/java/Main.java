import CLI.InputConfiguration.JSONParser;

public class Main {
    public static void main(String[] args) {
        try {
            JSONParser jsonParser = new JSONParser();
            jsonParser.parseJSON();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
import CLI.InputConfiguration.JSONParser;

public class Main {
    public static void main(String[] args) {
        try {
            JSONParser JSONParser = new JSONParser();
            CLI.InputConfiguration.JSONParser.parseJSON();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
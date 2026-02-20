package CLI.UserInput.RealInput;
import java.lang.reflect.Field;
import java.util.Scanner;

public class Input {

    String RED = "\u001B[1;30;101m";
    String RESET = "\u001B[0m";

    String nL = "\n";

    Scanner scanner = new Scanner(System.in);

    public record FunctionResult(String message, int code, boolean success) {

        public FunctionResult(String message) {
            this(message, 0, true);
        }

    }

    public FunctionResult makeInput(String type, String format){

        return switch (type) {
            case "STRING" -> new FunctionResult(stringInfo(format), 0, true);
            case "INTEGER" -> new FunctionResult("Int:", intInfo(format), true);
            case "BOOLEAN" -> new FunctionResult("Int:", 0, booleanInfo());
            default -> new FunctionResult("Nothing:", 0, true);
        };


    }

    public String stringInfo(String format) {

        String data = scanner.nextLine();

        while (!validateString(data, format)) {
            System.out.println(nL + RED + "Invalid format! Please try again." + RESET + nL);
            data = scanner.nextLine();
        }

        return data;

    }

    public boolean booleanInfo() {

        while(!scanner.hasNextBoolean()) {

            System.out.println(nL + RED + "That is not a valid boolean! Please try again." + RESET + nL);

            scanner.next();

        }

        boolean data = scanner.nextBoolean();

        scanner.nextLine();

        return data;

    }

    public int intInfo(String format) {

        while(!scanner.hasNextInt()) {

            System.out.println(nL + RED + "That is not a valid number! Please try again." + RESET + nL);

            scanner.next();

        }

        int data = scanner.nextInt();

        scanner.nextLine();

        while (!validateInt(data, format)) {
            System.out.println(nL + RED + "Invalid format! Please try again." + RESET + nL);

            while(!scanner.hasNextInt()) {
                System.out.println(nL + RED + "That is not a valid number! Please try again." + RESET + nL);
                scanner.next();
            }

            data = scanner.nextInt();
            scanner.nextLine();
        }

        return data;

    }

    private boolean validateString(String data, String format) {
        if (format == null) return true;
        return switch (format) {
            case "TIME" -> {
                if (!data.matches("\\d{2}:\\d{2}:\\d{2}")) yield false;
                String[] parts = data.split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int s = Integer.parseInt(parts[2]);
                yield h >= 0 && h <= 23 && m >= 0 && m <= 59 && s >= 0 && s <= 59;
            }
            case "DAY" -> {
                String[] days = data.split(",");
                for (String day : days) {
                    String trimmed = day.trim().toUpperCase();
                    if (!trimmed.matches("SUN|MON|TUE|WED|THU|FRI|SAT")) yield false;
                }
                yield true;
            }
            default -> true;
        };
    }

    private boolean validateInt(int data, String format) {
        if (format == null) return true;
        return switch (format) {
            case "PERCENTAGE" -> data >= 1 && data <= 100;
            case "HOUR" -> data >= 0 && data <= 23;
            case "MONTH" -> data >= 1 && data <= 12;
            default -> true;
        };
    }

    public float floatInfo() {

        while(!scanner.hasNextFloat()) {

            System.out.println(nL + RED + "That is not a valid number! Please try again." + RESET + nL);

            scanner.next();

        }

        float data = scanner.nextFloat();

        scanner.nextLine();

        return data;

    }


}

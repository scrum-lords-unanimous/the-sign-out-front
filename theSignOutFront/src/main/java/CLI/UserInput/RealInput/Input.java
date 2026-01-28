package CLI.UserInput.RealInput;
import java.lang.reflect.Field;
import java.util.Scanner;

public class Input {

    Scanner scanner = new Scanner(System.in);

    public record FunctionResult(String message, int code, boolean success) {

        public FunctionResult(String message) {
            this(message, 0, true);
        }

    }

    public FunctionResult makeInput(String type){

        return switch (type) {
            case "STRING" -> new FunctionResult(stringInfo(), 0, true);
            case "INTEGER" -> new FunctionResult("Int:", intInfo(), true);
            case "BOOLEAN" -> new FunctionResult("Int:", 0, booleanInfo());
            default -> new FunctionResult("Nothing:", 0, true);
        };


    }

    public String stringInfo() {


        String data = scanner.nextLine();


        return data;

    }

    public boolean booleanInfo() {

        while(!scanner.hasNextBoolean()) {

            System.out.println("That is not a valid boolean! Fuck you!");

            scanner.next();

        }

        boolean data = scanner.nextBoolean();

        scanner.nextLine();

        return data;

    }

    public int intInfo() {

        while(!scanner.hasNextInt()) {

            System.out.println("That is not a valid number! Please try again.");

            scanner.next();

        }

        int data = scanner.nextInt();

        scanner.nextLine();

        return data;

    }

    public float floatInfo() {

        while(!scanner.hasNextFloat()) {

            System.out.println("That is not a valid number! Please try again.");

            scanner.next();

        }

        float data = scanner.nextFloat();

        scanner.nextLine();

        return data;

    }


}

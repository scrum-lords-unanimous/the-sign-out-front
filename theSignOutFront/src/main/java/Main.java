import CLI.UserInput.RealOutput.Output;
import GUI.GuiApp;
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        System.out.println("Select interface:");
        System.out.println("1. CLI - Command Line Interface");
        System.out.println("2. GUI - Graphical User Interface");

        int choice = 0;
        try {
            choice = System.in.read() - '0';
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (choice == 2) {
            Application.launch(GuiApp.class, args);
        } else {
            Output outPut = new Output();
            try {
                outPut.makeOutput();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

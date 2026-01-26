import CLI.UserInput.RealOutput.Output;

public class Main {
    public static void main(String[] args) {
        Output outPut = new Output();
        try {
            outPut.makeOutput();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
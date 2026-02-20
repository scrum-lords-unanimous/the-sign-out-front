package CLI.UserInput.DoMath;
import java.lang.Math;

public class MathIsDone {

public double ComputeOperatives (double inputOne, double inputTwo, double inputThree, String typeOfOpp) {

    return switch(typeOfOpp){
	case "ADD" -> inputOne + inputTwo;
	case "SUBTRACT" -> inputOne - inputTwo;
	case "MULTIPLY" -> inputOne * inputTwo;
	case "DIVIDE" -> inputOne / inputTwo;
	case "SQRT" -> Math.sqrt(inputOne);
	case "SIN" -> Math.sin(inputTwo);
	case "CENTRIFUGAL" -> (Math.sqrt(inputOne * inputTwo) / Math.log(inputThree));
	default -> 0;
    };

}

}


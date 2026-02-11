package Data.Student;

import java.util.HashMap;
import java.util.Map;

public class Student {

    public String[] daysOfWeek;
    public int arrivalHour;
    public int arrivalMintues;
    public double position = 0;
    public boolean onDriveway = false;
    public Map<String, Double> properties = new HashMap<>();

    public Student(String[] daysOfWeek, int arrivalHour, int arrivalMintues) {
        this.daysOfWeek = daysOfWeek;
        this.arrivalHour = arrivalHour;
        this.arrivalMintues = arrivalMintues;
    }

    public double get(String operationId) {
        return properties.getOrDefault(operationId, 0.0);
    }

}
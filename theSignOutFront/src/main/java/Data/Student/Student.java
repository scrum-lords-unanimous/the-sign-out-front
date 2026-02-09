import java.time.LocalTime;
public class Student {

    // array for days student is on campus
    public String [] daysOfWeek = {" "," ", " ", " ", " "};
    public String mon;
    public String tues;
    public String wed;
    public String thur;
    public String fri;
    // hour that student arrives on campus
    public int arrivalHour;
    // minute that student arrives on campus
    public int arrivalMintues;
    // speed student is going up driveway
    public int speed;


    Student(String mon, String tues, String wed, String thur, String fri,
            int arrivalHour, int arrivalMintues, int speed){

        this.mon = mon;
        this.tues = tues;
        this.wed = wed;
        this.thur = thur;
        this.fri = fri;
        this.daysOfWeek = {mon, tues, wed, thur, fri};
        this.arrivalHour = arrivalHour;
        this.arrivalMintues = arrivalMintues;
        this.speed = speed;
    }

    /**
     * displays array for days student attends in person classes
     */
    void attendace(){
        System.out.println(daysOfWeek);
    }

    /**
     * sets the clock to the time the student enters for when they arrive on campus
     */
    void arrivalTime() {
        LocalTime startTime = LocalTime.of(arrivalHour, arrivalMintues, 0);
    }

    /**
     * method to fetch student speed
     * @return speed
     */
    public int getSpped(){
        return this.speed;
    }
}
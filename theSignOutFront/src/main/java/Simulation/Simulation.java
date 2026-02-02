package Simulation;

public class Simulation {

    int ROTATION_DURATION_OF_EARTH = 86400;

    int userRateOfAdvancement = 10000;

    public void theSimulation() {

        System.out.println("Your simulation will take" + Math.round(ROTATION_DURATION_OF_EARTH / userRateOfAdvancement) );

        for(int i = 0; i < ROTATION_DURATION_OF_EARTH; i++) {

            //Factoring in Centripetal forces for the maximum safe speed on the curve depending on the radius of said curve: (v= √(r * g * tan(θ)) r is the radius g = gravity and theta is the bank angle. Speed will be reduced then to this at a flat rate then adjusted. gravity equals 9.80665 )




        }


    }



}

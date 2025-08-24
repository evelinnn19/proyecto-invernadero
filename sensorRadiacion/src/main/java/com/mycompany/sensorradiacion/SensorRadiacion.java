package com.mycompany.sensorradiacion;

public class SensorRadiacion {

    public static void main(String[] args) {
        HiloSensadoRadia sensorRad = new HiloSensadoRadia();
        
        sensorRad.start();
    }
}

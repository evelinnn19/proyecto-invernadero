package com.mycompany.sensortemperatura;

public class SensorTemperatura {

    public static void main(String[] args) {
        HiloSensadoTemperatura sensorTemp = new HiloSensadoTemperatura();
        
        sensorTemp.start();
    }
}

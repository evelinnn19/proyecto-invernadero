package sensorhumedad1;

public class SensorHumedad1 {

    public static void main(String[] args) {
        HiloSensadoHumedad sensorHumedad = new HiloSensadoHumedad();
        
        sensorHumedad.start();
        
    }
    
}

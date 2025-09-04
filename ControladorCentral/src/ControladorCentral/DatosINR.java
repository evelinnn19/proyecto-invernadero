package ControladorCentral;

public class DatosINR {
    private double sensorH1;
    private double sensorH2;
    private double sensorH3;
    private double sensorH4;
    private double sensorH5;
    private double sensorTemp;
    private double sensorRad;
    private boolean sensorLluvia;

    
    /*
    La clase "DatosINR" ayudará a manejar el calulo del Indice de Necesidad de Riego.
    Almacena datos relacionados con los sensores, y puede calcular los indices.
    */
    
    /**Calcula el Indice de Necesidad de Riego. Para ello, se pasa un sensor
     *
     * @param sensor
     * @param w1
     * @param w2
     * @param w3
     * @return
     */
    private double calcularINR(double h, double w1, double w2, double w3) {
        if (sensorLluvia) {
            System.out.println("Lluvia detectada, riego inhibido.");
            
            return 0;
        }
        return w1 * (1 - h / 100.0)
             + w2 * (sensorTemp / 40.0)
             + w3 * (sensorRad / 1000.0);
    }

    
    public double calcularINR(int sensor, double w1, double w2, double w3) {
        switch (sensor) {
            case 1: return calcularINR(sensorH1, w1, w2, w3);
            case 2: return calcularINR(sensorH2, w1, w2, w3);
            case 3: return calcularINR(sensorH3, w1, w2, w3);
            case 4: return calcularINR(sensorH4, w1, w2, w3);
            case 5: return calcularINR(sensorH5, w1, w2, w3);
            default: throw new IllegalArgumentException("Número de parcela inválido: " + sensor);
        }
    }
    
    public double getSensorH1() {
        return sensorH1;
    }

    public void setSensorH1(double sensorH1) {
        this.sensorH1 = sensorH1;
    }

    public double getSensorH2() {
        return sensorH2;
    }

    public void setSensorH2(double sensorH2) {
        this.sensorH2 = sensorH2;
    }

    public double getSensorH3() {
        return sensorH3;
    }

    public void setSensorH3(double sensorH3) {
        this.sensorH3 = sensorH3;
    }

    public double getSensorH4() {
        return sensorH4;
    }

    public void setSensorH4(double sensorH4) {
        this.sensorH4 = sensorH4;
    }

    public double getSensorH5() {
        return sensorH5;
    }

    public void setSensorH5(double sensorH5) {
        this.sensorH5 = sensorH5;
    }

    public double getSensorTemp() {
        return sensorTemp;
    }

    public void setSensorTemp(double sensorTemp) {
        this.sensorTemp = sensorTemp;
    }

    public double getSensorRad() {
        return sensorRad;
    }

    public void setSensorRad(double sensorRad) {
        this.sensorRad = sensorRad;
    }

    public boolean isSensorLluvia() {
        return sensorLluvia;
    }

    public void setSensorLluvia(boolean sensorLluvia) {
        this.sensorLluvia = sensorLluvia;
    }
    
    
    
    
}



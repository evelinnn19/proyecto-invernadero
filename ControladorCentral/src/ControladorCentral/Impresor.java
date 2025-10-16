/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ControladorCentral;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alejandro
 */
public class Impresor extends Thread{
    
  private Double h1, h2, h3, h4, h5;
  private Boolean RiegoGeneral, Fertirrigacion;

    
    private double INRV1;
    private double INRV2;
    private double INRV3;
    private double INRV4;
    private double INRV5;
    
    private int tiempo1;
    private int tiempo2;
    private int tiempo3;
    private int tiempo4;
    private int tiempo5;
    
    
    private double temperatura;
    private double rad;
    private boolean lluvia;
    
    
    
    
    public void mensaje(String a){
        System.out.println(a);
    }
    
    
    
public boolean todosListos() {
    return (h1 != null && h2 != null && h3 != null && h4 != null && h5 != null && RiegoGeneral != null && Fertirrigacion != null);
}

    
    @Override
    public void run(){
    while(true){
            if(todosListos()){
                System.out.println("===== ESTADO DEL SISTEMA =====");
                System.out.printf("Radiacion: %.2f%n", rad);
                System.out.printf("Llueve? %s%n", lluvia ? "si" : "no");
                System.out.printf("Temperatura: %.2f%n", temperatura);

                System.out.println("-- RIEGO GENERAL --");
                System.out.printf("Riego General: %s%n", RiegoGeneral ? "si" : "no");

                System.out.println("--- PARCELAS ---");
                System.out.printf("Parcela 1: Humedad %.2f, Regando por %d s%n", h1, tiempo1);
                System.out.printf("Parcela 2: Humedad %.2f, Regando por %d s%n", h2, tiempo2);
                System.out.printf("Parcela 3: Humedad %.2f, Regando por %d s%n", h3, tiempo3);
                System.out.printf("Parcela 4: Humedad %.2f, Regando por %d s%n", h4, tiempo4);
                System.out.printf("Parcela 5: Humedad %.2f, Regando por %d s%n", h5, tiempo5);

                System.out.println("-- FERTIRRIGACION --");
                System.out.printf("Fertirrigacion: %s%n", Fertirrigacion ? "si" : "no");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                // evitar busy-loop mientras esperamos datos
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // el resto de setters/getters igual
    
    public void setINR(int sensor,double INR){
        switch (sensor) {
            case 1 -> this.INRV1 = INR;
            case 2 -> this.INRV2 = INR;
            case 3 -> this.INRV3 = INR;
            case 4 -> this.INRV4 = INR;
            case 5 -> this.INRV5 = INR;
            default -> throw new IllegalArgumentException("Sensor inválido: " + sensor);
        }
          
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    public double getRad() {
        return rad;
    }

    public void setRad(double rad) {
        this.rad = rad;
    }

    public boolean isLluvia() {
        return lluvia;
    }

    public void setLluvia(boolean lluvia) {
        this.lluvia = lluvia;
    }
    
    
    
    
    

    
    
    public double getH1() {
        return h1;
    }

    public void setH1(double h1) {
        this.h1 = h1;
    }

    public double getH2() {
        return h2;
    }

    public void setH2(double h2) {
        this.h2 = h2;
    }

    public double getH3() {
        return h3;
    }

    public void setH3(double h3) {
        this.h3 = h3;
    }

    public double getH4() {
        return h4;
    }

    public void setH4(double h4) {
        this.h4 = h4;
    }

    public double getH5() {
        return h5;
    }

    public void setH5(double h5) {
        this.h5 = h5;
    }

    public Boolean getRiegoGeneral() {
        return RiegoGeneral;
    }

    public void setRiegoGeneral(Boolean RiegoGeneral) {
        this.RiegoGeneral = RiegoGeneral;
    }

    public Boolean getFertirrigacion() {
        return Fertirrigacion;
    }

    public void setFertirrigacion(Boolean Fertirrigacion) {
        this.Fertirrigacion = Fertirrigacion;
    }

    public double getINRV1() {
        return INRV1;
    }

    public void setINRV1(double INRV1) {
        this.INRV1 = INRV1;
    }

    public double getINRV2() {
        return INRV2;
    }

    public void setINRV2(double INRV2) {
        this.INRV2 = INRV2;
    }

    public double getINRV3() {
        return INRV3;
    }

    public void setINRV3(double INRV3) {
        this.INRV3 = INRV3;
    }

    public double getINRV4() {
        return INRV4;
    }

    public void setINRV4(double INRV4) {
        this.INRV4 = INRV4;
    }

    public double getINRV5() {
        return INRV5;
    }

    public void setINRV5(double INRV5) {
        this.INRV5 = INRV5;
    }

    public int getTiempo1() {
        return tiempo1;
    }

    public void setTiempo1(int tiempo1) {
        this.tiempo1 = tiempo1;
    }

    public int getTiempo2() {
        return tiempo2;
    }

    public void setTiempo2(int tiempo2) {
        this.tiempo2 = tiempo2;
    }

    public int getTiempo3() {
        return tiempo3;
    }

    public void setTiempo3(int tiempo3) {
        this.tiempo3 = tiempo3;
    }

    public int getTiempo4() {
        return tiempo4;
    }

    public void setTiempo4(int tiempo4) {
        this.tiempo4 = tiempo4;
    }

    public int getTiempo5() {
        return tiempo5;
    }

    public void setTiempo5(int tiempo5) {
        this.tiempo5 = tiempo5;
    }

    void setTiempos(int parcela, int tiempo) {
        switch (parcela) {
            case 1 -> setTiempo1(tiempo);
            case 2 -> setTiempo2(tiempo);
            case 3 -> setTiempo3(tiempo);
            case 4 -> setTiempo4(tiempo);
            case 5 -> setTiempo5(tiempo);
            
            default -> throw new AssertionError();
        }
    }
    
    
    
    
}

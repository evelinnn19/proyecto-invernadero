package com.mycompany.sensorradiacion;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoRadia extends Thread {

    private Boolean prendido;
    private Double radiacion;
    private Socket socketClienteRadiacion;
    private PrintWriter haciaServer;

    public HiloSensadoRadia(Socket socketClienteRadiacion, PrintWriter haciaServer) {
        prendido = Boolean.TRUE;
        this.socketClienteRadiacion = socketClienteRadiacion;
        this.haciaServer = haciaServer;
    }

    public void generarRadiacionAleatoria() {
        Random random = new Random();
        radiacion = random.nextDouble(0, 1000);
    }

    public void apagar() {
        prendido = Boolean.FALSE;
    }

    public void encender() {
        prendido = Boolean.TRUE;
    }

    public void run() {
        
        try {
            while (prendido) {
                generarRadiacionAleatoria();
                haciaServer.flush();
                haciaServer.println(radiacion);
                System.out.println("La radiacion desde el dispositivo es: " + radiacion);
                                
                Thread.sleep(5000);
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(HiloSensadoRadia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
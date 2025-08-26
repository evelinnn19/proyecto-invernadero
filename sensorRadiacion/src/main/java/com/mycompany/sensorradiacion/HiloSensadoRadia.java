package com.mycompany.sensorradiacion;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoRadia extends Thread {

    private Boolean prendido;
    private Double radiacion;
    private Socket socketClienteRadiacion;
    private DataOutputStream haciaServer;

    public HiloSensadoRadia(Socket socketClienteRadiacion, DataOutputStream haciaServer) {
        prendido = Boolean.TRUE;
        this.socketClienteRadiacion = socketClienteRadiacion;
        this.haciaServer = haciaServer;
    }

    public void HiloSensado() {
        prendido = false;
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
        prendido = Boolean.TRUE;
        //usar prendido como condicion podría ayudar a controlar cuando queremos sensar la humedad.

        try {

            while (prendido) {
                generarRadiacionAleatoria();
                haciaServer.writeDouble(radiacion);
                System.out.println("La radiacion es: " + radiacion);
                haciaServer.flush();
                
                Thread.sleep(5000);
            }

        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HiloSensadoRadia.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

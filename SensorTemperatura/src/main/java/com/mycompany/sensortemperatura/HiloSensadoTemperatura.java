package com.mycompany.sensortemperatura;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

public class HiloSensadoTemperatura extends Thread {

    private Boolean prendido;
    private Double temperatura;
    private Socket clienteTemperaturaEnviar;
    private DataOutputStream haciaServer;

    public HiloSensadoTemperatura(Socket clienteTemperaturaEnviar, DataOutputStream haciaServer) {
        prendido = Boolean.TRUE;
        this.clienteTemperaturaEnviar = clienteTemperaturaEnviar;
        this.haciaServer = haciaServer;
    }
    
    public void generarTempAleatoria() {
        Random random = new Random();
        temperatura = random.nextDouble(-3, 40);
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
                generarTempAleatoria();
                haciaServer.writeDouble(temperatura);
                System.out.println("La temperatura es: " + temperatura);
                haciaServer.flush();
                
                Thread.sleep(5000);
            }

        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HiloSensadoTemperatura.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

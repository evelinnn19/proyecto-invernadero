package com.mycompany.sensorlluvia;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorLluvia {
    private static final int MAX_INTENTOS_RECONEXION = 10;
    private static final int TIEMPO_ESPERA_RECONEXION = 5000; // 5 segundos
    private static HiloSensadoLluvia sensorActual = null;


public static void restablecerConexion() throws InterruptedException {
        int intentos = 0;
        
        while (intentos < MAX_INTENTOS_RECONEXION) {
            try {
                System.out.println("Intento de reconexión #" + (intentos + 1));
                Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
                cliente.setKeepAlive(true); // Mantener conexión activa
                cliente.setSoTimeout(10000); // Timeout de 10 segundos
                
                System.out.println("Cliente reconectado: " + cliente);
                PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);
                outToServer.println("sensorLluvia");
                outToServer.flush();
                
                // Detener el hilo anterior si existe
                if (sensorActual != null && sensorActual.isAlive()) {
                    sensorActual.apagar();
                }
                
                sensorActual = new HiloSensadoLluvia(cliente, outToServer);
                sensorActual.start();
                
                System.out.println("Reconexión exitosa.");
                return; // Salir si la reconexión fue exitosa
                
            } catch (IOException e) {
                intentos++;
                System.out.println("Error al restablecer la conexión (intento " + intentos + "): " + e.getMessage());
                
                if (intentos < MAX_INTENTOS_RECONEXION) {
                    System.out.println("Esperando " + (TIEMPO_ESPERA_RECONEXION/1000) + " segundos antes de reintentar...");
                    Thread.sleep(TIEMPO_ESPERA_RECONEXION);
                } else {
                    System.out.println("Se alcanzó el máximo de intentos de reconexión. Abortando.");
                    Logger.getLogger(HiloSensadoLluvia.class.getName()).log(Level.SEVERE, 
                        "No se pudo restablecer la conexión después de " + MAX_INTENTOS_RECONEXION + " intentos", e);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
             cliente.setKeepAlive(true); // Mantener conexión activa
            cliente.setSoTimeout(10000); // Timeout de 10 segundos para operacion
            System.out.println("Sensor conectado.    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("sensorLluvia");
            outToServer.flush();
            
            HiloSensadoLluvia sensorHumedad = new HiloSensadoLluvia(cliente, outToServer);
            sensorHumedad.start();

        } catch (IOException ex) {
            System.out.println("Error en la conexión inicial: " + ex.getMessage());
            restablecerConexion();
        }
    }
}

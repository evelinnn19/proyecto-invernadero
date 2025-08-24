package sensorhumedad1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoHumedad extends Thread {

    private Boolean prendido;
    private Double humedad;

    public void HiloSensado() {
        prendido = false;
    }

    public void generarHumedadAleatoria() {
        humedad = Math.random() * 100;

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
                //Crea el socket.
                Socket socketCliente = new Socket("localhost", 5678);
                //Crea los input y output referidos al socket.
                DataOutputStream outToServer = new DataOutputStream(socketCliente.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
                
                //Crear el paquete relevante al socket.
                
                
                
                generarHumedadAleatoria();
                
                outToServer.writeDouble(humedad);
                System.out.println("La humedad es: " + humedad);
                
                outToServer.flush();
                
                socketCliente.close();
                Thread.sleep(5000);

            }

        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HiloSensadoHumedad.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

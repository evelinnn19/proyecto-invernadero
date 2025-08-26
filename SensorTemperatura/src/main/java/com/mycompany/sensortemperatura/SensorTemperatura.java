package com.mycompany.sensortemperatura;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorTemperatura {

    public static void main(String[] args) {
        try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            DataOutputStream outToServer = new DataOutputStream(cliente.getOutputStream());

            outToServer.writeChars("sensorTemperatura");
            outToServer.flush();
            
            HiloSensadoTemperatura sensorHumedad = new HiloSensadoTemperatura(cliente, outToServer);
            sensorHumedad.start();

        } catch (IOException ex) {
            Logger.getLogger(SensorTemperatura.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

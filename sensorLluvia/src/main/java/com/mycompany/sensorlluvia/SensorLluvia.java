package com.mycompany.sensorlluvia;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorLluvia {

    public static void main(String[] args) {
        try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            DataOutputStream outToServer = new DataOutputStream(cliente.getOutputStream());

            outToServer.writeBytes("sensorHumedad");
            outToServer.flush();
            
            HiloSensadoLluvia sensorHumedad = new HiloSensadoLluvia(cliente, outToServer);
            sensorHumedad.start();

        } catch (IOException ex) {
            Logger.getLogger(SensorLluvia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

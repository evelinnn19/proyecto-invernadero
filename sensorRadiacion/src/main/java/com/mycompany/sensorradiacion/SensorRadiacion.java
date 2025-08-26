package com.mycompany.sensorradiacion;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SensorRadiacion {

    public static void main(String[] args) throws IOException {

        try {
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Socket creado.    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("sensorRadiacion");
            outToServer.flush();
            
            HiloSensadoRadia sensorRad = new HiloSensadoRadia(cliente, outToServer);
            sensorRad.start();
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(SensorRadiacion.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

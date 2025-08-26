package com.mycompany.sensorradiacion;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SensorRadiacion {

    public static void main(String[] args) throws IOException {

        try {
            System.out.println("Inicio de la conexión.");
            Socket socket = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Socket creado.    " + socket);
            
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            
            outToServer.writeChars("sensorRadiacion");
            outToServer.flush();
            
            HiloSensadoRadia sensorRad = new HiloSensadoRadia(socket, outToServer);
            sensorRad.start();
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(SensorRadiacion.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

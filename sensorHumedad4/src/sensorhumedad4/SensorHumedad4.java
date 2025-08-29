package sensorhumedad4;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorHumedad4 {

    public static void main(String[] args) {
        try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("sensorHumedad4");
            outToServer.flush();
            
            HiloSensadoHumedad sensorHumedad = new HiloSensadoHumedad(cliente, outToServer);
            sensorHumedad.start();

        } catch (IOException ex) {
            Logger.getLogger(SensorHumedad4.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
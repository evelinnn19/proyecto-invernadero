package sensorhumedad1;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorHumedad1 {

    public static void main(String[] args) {

        try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            DataOutputStream outToServer = new DataOutputStream(cliente.getOutputStream());

            outToServer.writeChars("sensorHumedad");
            outToServer.flush();
            
            HiloSensadoHumedad sensorHumedad = new HiloSensadoHumedad(cliente, outToServer);
            sensorHumedad.start();

        } catch (IOException ex) {
            Logger.getLogger(SensorHumedad1.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}

package sensorhumedad1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorHumedad1 {


    public static void restablecerConexion() throws InterruptedException{
        try{
            System.out.println("Restablecimiento de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);
            outToServer.println("sensorHumedad1");
            outToServer.flush();
            HiloSensadoHumedad sensorHumedad = new HiloSensadoHumedad(cliente, outToServer);
            sensorHumedad.start();
        
        } catch (IOException e) {
            System.out.println("Error al restablecer la conexión: " + e.getMessage());
            Thread.sleep(5000); // Esperar 5 segundos antes de reintentar
            restablecerConexion();

        }
    }

    public static void main(String[] args) throws InterruptedException {
        try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("sensorHumedad1");
            outToServer.flush();
            
            HiloSensadoHumedad sensorHumedad = new HiloSensadoHumedad(cliente, outToServer);
            sensorHumedad.start();

        } catch (IOException ex) {
            Logger.getLogger(SensorHumedad1.class.getName()).log(Level.SEVERE, null, ex);
            restablecerConexion();
        }
    }
}
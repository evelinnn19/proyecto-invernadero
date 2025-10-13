
package electrovalvula;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Evelin
 */
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ElectroValvula {
    private final int id;
    private final AtomicBoolean estado; // true: abierta, false: cerrada
    private final String centralHost;
    private final int centralPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean ejecutando;
    private Thread hiloMonitoreo;

    public ElectroValvula(int id, String centralHost, int centralPort) {
        this.id = id;
        this.estado = new AtomicBoolean(false);
        this.centralHost = centralHost;
        this.centralPort = centralPort;
        this.ejecutando = false;
    }


    public void restablecerConexion(){
        try{
            System.out.println("Restablecimiento de la conexion.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);
            outToServer.println("electroValvula1");
            outToServer.flush();
            HiloSensadoElectrovalvula Electrovalvula = new HiloSensadoElectrovalvula(cliente, outToServer);
            Electrovalvula.start();
        
        } catch (IOException e) {
            System.out.println("Error al restablecer la conexion: " + e.getMessage());
            Thread.sleep(5000); // Esperar 5 segundos antes de reintentar
            restablecerConexion();

        }
    }
    public static void main(String[] args) {
        try{
            System.out.println("Inicio de la conexion.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("electroValvula1");
            outToServer.flush();
            
            HiloSensadoElectrovalvula Electrovalvula = new HiloSensadoElectrovalvula(cliente, outToServer);
            Electrovalvula.start();

        } catch (IOException ex) {
            Logger.getLogger(ElectroValvula.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}



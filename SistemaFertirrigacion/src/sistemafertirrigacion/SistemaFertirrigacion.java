package sistemafertirrigacion;

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
public class SistemaFertirrigacion {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
            try{
            System.out.println("Inicio de la conexión.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Sistema de fertirrigación conectado a:    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("sistemaFertirrigacion");
            outToServer.flush();
            
            HiloFertirrigacion ferti = new HiloFertirrigacion(cliente, outToServer);
            ferti.start();

        } catch (IOException ex) {
            Logger.getLogger(SistemaFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

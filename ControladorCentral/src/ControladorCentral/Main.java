package ControladorCentral;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alumnos
 *
 * Clase principal que arranca todo el proyecto del invernadero.
 *
 * Para hacer funcionar este Sistema Distribuido, tendremos un hilo dedicado a
 * aceptar conexiones y reducir posibles timeouts en la forma de
 * HiloManejoCliente.
 *
 * Las clases "HiloReceptorX" se encargarán de escuchar constantemente la
 * información proveniente de los dispositivos (otros proyectos en ejecución).
 */
public class Main {

    public static void main(String[] args) {
        
        //objeto unico DatosINR
        
        DatosINR datos = new DatosINR();
        try {
            System.out.println("Inicio del Invernadero.");
            ServerSocket server = new ServerSocket(20000);
            System.out.println("Socket de conexion iniciado.");
            //cuando este prenddo el servidor
            
            while (true) {
                System.out.println("Esperando a una conexion.");
                Socket s = server.accept();
                HiloManejoCliente cliente = new HiloManejoCliente(s,datos);
                System.out.println("Se detecto una conexión.   " + s);
                
                cliente.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

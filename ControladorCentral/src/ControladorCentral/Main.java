package ControladorCentral;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
 * 
 * 
 */
public class Main {

    public static void main(String[] args) {
        

        try {
        //objeto unico DatosINR
        //Impresor para regular los mensajes
            Impresor impresor = new Impresor();
        
            DatosINR datos = new DatosINR();
            datos.setImp(impresor);
            impresor.start();
            CoordinadorBomba coordinador = new CoordinadorBomba();
            coordinador.setImp(impresor);
            
            System.out.println("Inicio del Invernadero.");
            ServerSocket server = new ServerSocket(20000);
            System.out.println("Socket de conexion iniciado.");
            //cuando este prenddo el servidor
            
            RMIClienteBomba clienteRMI;
            
            while(true){
                try {
                clienteRMI = new RMIClienteBomba("rmi://localhost:10000/servidorCentralEM");
                coordinador.setRmiCliente(clienteRMI);
                System.out.println("Cliente RMI inicializado y conectado al servidor");
                break;
            } catch (RemoteException | MalformedURLException | NotBoundException ex) {
                
            }
            }
            
            
            
            while (true) {
                System.out.println("Esperando a una conexion.");
                Socket s = server.accept();
                HiloManejoCliente cliente = new HiloManejoCliente(s,datos,coordinador,impresor);
                System.out.println("Se detecto una conexión. --->   " + s);
                
                cliente.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

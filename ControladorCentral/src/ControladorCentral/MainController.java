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
public class MainController {

    public static final int PUERTO = 20000;

    // Modo “normal”: crea su propio ServerSocket
    public static void iniciarPrincipal() {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            iniciarConServerSocket(server);
        } catch (IOException ex) {
            Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Modo “promoción”: usa un ServerSocket que ya abrió el backup (al lograr el bind)
    public static void iniciarPrincipal(ServerSocket server) {
        try {
            iniciarConServerSocket(server);
        } catch (IOException ex) {
            Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Lógica original, factorizada para reutilizar
    private static void iniciarConServerSocket(ServerSocket server) throws IOException {
        try {
            Impresor impresor = new Impresor();
            DatosINR datos = new DatosINR();
            datos.setImp(impresor);
            impresor.start();

            CoordinadorBomba coordinador = new CoordinadorBomba();
            coordinador.setImp(impresor);

            System.out.println("Inicio del Invernadero.");
            System.out.println("Socket de conexion iniciado (puerto " + server.getLocalPort() + ").");

            // Conexión al servidor de exclusión mutua (RMI)
            RMIClienteBomba clienteRMI;
            while (true) {
                try {
                    clienteRMI = new RMIClienteBomba("rmi://localhost:10000/servidorCentralEM");
                    coordinador.setRmiCliente(clienteRMI);
                    System.out.println("Cliente RMI inicializado y conectado al servidor");
                    break;
                } catch (RemoteException | MalformedURLException | NotBoundException ex) {
                    // reintento discreto
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
            }

            // Loop de aceptación
            while (true) {
                System.out.println("Esperando a una conexion.");
                Socket s = server.accept();
                System.out.println("Se detecto una conexion. --->   " + s);
                HiloManejoCliente cliente = new HiloManejoCliente(s, datos, coordinador, impresor);
                cliente.start();
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
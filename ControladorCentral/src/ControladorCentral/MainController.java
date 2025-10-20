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
            // Objeto único DatosINR
            Impresor impresor = new Impresor();
            
            DatosINR datos = new DatosINR();
            datos.setImp(impresor);
            impresor.start();
            
            CoordinadorBomba coordinador = new CoordinadorBomba();
            coordinador.setImp(impresor);
            
            System.out.println("Inicio del Sistema de Control de Invernadero");
            
            server = new ServerSocket(20000);
            System.out.println("Socket de conexión iniciado en puerto 20000");
            
            // 🆕 Intentar conectar con cliente RMI con failover
            RMIClienteBomba clienteRMI = null;
            int intentosConexionRMI = 0;
            final int MAX_INTENTOS = 10;
            
            while (clienteRMI == null && intentosConexionRMI < MAX_INTENTOS) {
                try {
                    clienteRMI = new RMIClienteBomba();
                    coordinador.setRmiCliente(clienteRMI);
                    System.out.println("Cliente RMI con failover inicializado");
                    System.out.println("Conectado a: " + clienteRMI.getServidorActual());
                    break;
                    
                } catch (RemoteException | MalformedURLException | NotBoundException ex) {
                    intentosConexionRMI++;
                    System.err.println("Intento " + intentosConexionRMI + "/" + MAX_INTENTOS + 
                                     " - Error al conectar con servidor RMI: " + ex.getMessage());
                    
                    if (intentosConexionRMI < MAX_INTENTOS) {
                        System.out.println("Reintentando en 3 segundos...");
                        Thread.sleep(3000);
                    } else {
                        System.err.println("No se pudo conectar con ningún servidor RMI después de " + 
                                         MAX_INTENTOS + " intentos");
                        System.err.println("El sistema continuará sin exclusión mutua (modo degradado)");
                    }
                }
            }
            
            // Bucle principal de aceptación de clientes
            System.out.println("Sistema listo - Esperando conexiones...\n");
            
            while (true) {
                Socket s = server.accept();
                System.out.println("Nueva conexión detectada: " + s.getInetAddress().getHostAddress());
                
                HiloManejoCliente cliente = new HiloManejoCliente(s, datos, coordinador, impresor);
                cliente.start();
            }
            
        } catch (IOException ex) {
            System.err.println("❌ Error crítico en el servidor: " + ex.getMessage());
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            System.err.println("⚠️ Hilo principal interrumpido");
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
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
public class ControladorCentral {

    public static final int PUERTO = 20000;
    private ServerSocket server;

    public void iniciar() {
        try {
            Impresor impresor = new Impresor();
            DatosINR datos = new DatosINR();
            datos.setImp(impresor);
            impresor.start();

            CoordinadorBomba coordinador = new CoordinadorBomba();
            coordinador.setImp(impresor);

            System.out.println("Inicio del Sistema de Control de Invernadero");
            server = new ServerSocket(PUERTO);
            System.out.println("Socket de conexión iniciado en puerto " + PUERTO);

            RMIClienteBomba clienteRMI = null;
            int intentosConexionRMI = 0;
            final int MAX_INTENTOS = 10;

            while (clienteRMI == null && intentosConexionRMI < MAX_INTENTOS) {
                try {
                    clienteRMI = new RMIClienteBomba();
                    coordinador.setRmiCliente(clienteRMI);
                    System.out.println("Cliente RMI inicializado y conectado al servidor");
                    break;

                } catch (RemoteException | MalformedURLException | NotBoundException ex) {
                    intentosConexionRMI++;
                    System.err.println("Intento " + intentosConexionRMI + "/" + MAX_INTENTOS +
                            " - Error al conectar con servidor RMI: " + ex.getMessage());
                    Thread.sleep(3000);
                }
            }

            System.out.println("Sistema listo - Esperando conexiones...\n");

            while (true) {
                Socket s = server.accept();
                System.out.println("Nueva conexión detectada: " + s.getInetAddress().getHostAddress());

                HiloManejoCliente cliente = new HiloManejoCliente(s, datos, coordinador, impresor);
                cliente.start();
            }

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ControladorCentral.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
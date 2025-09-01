
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
import java.net.*;
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

    public void iniciar() {
        //inicia o arranca la ElectroValvula
        ejecutando = true;
        System.out.println("ElectroVálvula " + id + " iniciada. Estado inicial: CERRADA");
        
        // Conectar al ControlCentral
        if (conectarAlCentral()) {
            // Iniciar hilo de monitoreo del estado
            hiloMonitoreo = new Thread(this::monitorearEstado);
            hiloMonitoreo.start();
        }
    }

    public void detener() {
        
        //  Se desea apagar la ElectroValvula
        ejecutando = false;
        System.out.println("ElectroVálvula " + id + " deteniéndose...");
        
        // Cerrar conexión
        cerrarConexion();
        
        // Esperar a que el hilo termine
        if (hiloMonitoreo != null && hiloMonitoreo.isAlive()) {
            try {
                hiloMonitoreo.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("ElectroVálvula " + id + " detenida");
    }

    private boolean conectarAlCentral() {
        try {
            // Conectar al puerto conocido del ControlCentral
            //completar datos de la central
            socket = new Socket(centralHost, centralPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Identificarse como ElectroVálvula
            out.println("ElectroValvula_" + id);
            
            // Leer respuesta del ControlCentral
            String respuesta = in.readLine();
            if (respuesta != null && respuesta.startsWith("ACK")) {
                System.out.println("ElectroVálvula " + id + " conectada al ControlCentral");
                return true;
            } else {
                System.out.println("Error en conexión: " + respuesta);
                cerrarConexion();
                return false;
            }
            
        } catch (IOException e) {
            System.out.println("Error conectando al ControlCentral: " + e.getMessage());
            return false;
        }
    }

    private void monitorearEstado() {
        System.out.println("ElectroVálvula " + id + " monitoreando estado...");
        
        while (ejecutando) {
            try {
                // Leer comandos del ControlCentral
                String comando = in.readLine();
                if (comando == null) {
                    // Conexión cerrada
                    System.out.println("Conexión cerrada por el ControlCentral");
                    reconectar();
                    continue;
                }
                
                procesarComando(comando);
                
                // Pequeña pausa para no saturar
                Thread.sleep(100);
                
            } catch (IOException e) {
                System.out.println("Error de comunicación: " + e.getMessage());
                reconectar();
            } catch (InterruptedException e) {
                System.out.println("Monitoreo interrumpido");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void procesarComando(String comando) {
        if (comando.equals("ABRIR")) {
            abrir();
        } else if (comando.equals("CERRAR")) {
            cerrar();
        } else if (comando.equals("ESTADO")) {
            enviarEstado();
        } else if (comando.equals("PING")) {
            out.println("PING_" + id);
        } else {
            System.out.println("Comando desconocido: " + comando);
        }
    }

    public synchronized void abrir() {
        if (!estado.get()) {
            estado.set(true);
            System.out.println("ElectroVálvula " + id + " ABIERTA");
            out.println("ESTADO_ABIERTA");
        }
    }

    public synchronized void cerrar() {
        if (estado.get()) {
            estado.set(false);
            System.out.println("ElectroVálvula " + id + " CERRADA");
            out.println("ESTADO_CERRADA");
        }
    }

    private void enviarEstado() {
        String estadoActual = estado.get() ? "ABIERTA" : "CERRADA";
        out.println("ESTADO_" + estadoActual);
    }

    private void reconectar() {
        System.out.println("ElectroVálvula " + id + " intentando reconectar...");
        
        cerrarConexion();
        
        // Intentar reconexión cada 5 segundos
        while (ejecutando) {
            try {
                Thread.sleep(5000);
                if (conectarAlCentral()) {
                    // Reanudar monitoreo después de reconectar
                    new Thread(this::monitorearEstado).start();
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void cerrarConexion() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error cerrando conexión: " + e.getMessage());
        }
    }

    public boolean getEstado() {
        return estado.get();
    }

    public int getId() {
        return id;
    }

    public static void main(String[] args) {
        try{
            System.out.println("Inicio de la conexion.");
            Socket cliente = new Socket(InetAddress.getByName("localhost"), 20000);
            System.out.println("Cliente conectado.    " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);

            outToServer.println("electroValvula3");
            outToServer.flush();
            
            HiloSensadoElectrovalvula Electrovalvula = new HiloSensadoElectrovalvula(cliente, outToServer);
            Electrovalvula.start();

        } catch (IOException ex) {
            Logger.getLogger(ElectroValvula.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}



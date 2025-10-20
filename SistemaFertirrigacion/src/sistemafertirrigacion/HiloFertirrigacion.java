package sistemafertirrigacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloFertirrigacion extends Thread {
    private PrintWriter out;
    private BufferedReader in;
    private Socket cliente;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;
    private Thread hiloHeartbeat;
    
    public HiloFertirrigacion(Socket cliente, PrintWriter out) {
        this.cliente = cliente;
        this.ejecutando = true;
        
        try {
            this.in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = out;
            
            // Configurar socket para detectar desconexiones
            cliente.setKeepAlive(true);
            cliente.setSoTimeout(60000); // 60 segundos timeout (más tiempo porque enviamos heartbeats)
            cliente.setTcpNoDelay(true);
            
        } catch (IOException ex) {
            System.out.println("Error al configurar socket: " + ex.getMessage());
            Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // 🆕 Hilo que envía PING cada 10 segundos
    private void iniciarHeartbeat() {
        hiloHeartbeat = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10000); // Cada 10 segundos
                    
                    if (ejecutando && !verificarEstadoSocket()) {
                        out.println("PING");
                        out.flush();
                        
                        if (out.checkError()) {
                            System.out.println("Error al enviar PING - Servidor caído");
                            manejarDesconexion();
                            break;
                        }
                        
                        System.out.println("Heartbeat enviado al servidor");
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Heartbeat interrumpido");
            }
        });
        
        hiloHeartbeat.setDaemon(true);
        hiloHeartbeat.start();
    }
    
    public void detener() {
        ejecutando = false;
        if (hiloMonitorConexion != null) {
            hiloMonitorConexion.interrupt();
        }
        if (hiloHeartbeat != null) {
            hiloHeartbeat.interrupt();
        }
    }
    
    // Monitor que verifica el estado de la conexión
    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Conexión perdida");
                        manejarDesconexion();
                        break;
                    }
                    Thread.sleep(5000); // Verificar cada 5 segundos
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor de conexión interrumpido");
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }
    
    private boolean verificarEstadoSocket() {
        try {
            return cliente.isClosed() || 
                   !cliente.isConnected() || 
                   cliente.isInputShutdown() || 
                   cliente.isOutputShutdown();
        } catch (Exception e) {
            return true;
        }
    }
    
    private void manejarDesconexion() {
        System.out.println("SERVIDOR DESCONECTADO");
        ejecutando = false;
        cerrarRecursos();
        reconectar();
    }
    
    private void cerrarRecursos() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos: " + e.getMessage());
        }
    }
    
    private void reconectar() {
        try {
            System.out.println("Iniciando proceso de reconexión...");
            SistemaFertirrigacion.restablecerConexion();
        } catch (InterruptedException e) {
            Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    @Override
    public void run() {
        try {
            // 🆕 Iniciar heartbeat
            iniciarHeartbeat();
            iniciarMonitorConexion();
            
            System.out.println("Sistema de Fertirrigación operativo (con heartbeat)");
            
            while (ejecutando) {
                // Verificar conexión antes de leer
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida en ciclo principal");
                    manejarDesconexion();
                    break;
                }
                
                try {
                    // Leer orden del servidor (puede ser tiempo o PONG)
                    String orden = in.readLine();
                    
                    if (orden == null) {
                        System.out.println("Servidor cerró la conexión (orden null)");
                        manejarDesconexion();
                        break;
                    }
                    
                    // Ignorar respuestas PONG del servidor
                    if (orden.equals("PONG")) {
                        System.out.println("PONG recibido del servidor");
                        continue;
                    }
                    
                    // Parsear tiempo de fertirrigación
                    int tiempo = Integer.parseInt(orden);
                    System.out.println("Fertirrigación iniciada por " + (tiempo/1000) + " segundos");
                    
                    // Simular proceso de fertirrigación
                    Thread.sleep(tiempo);
                    
                    System.out.println("Fertirrigación finalizada");
                    
                    // Enviar confirmación al servidor
                    out.println("FERTI_OK");
                    out.flush();
                    
                    if (out.checkError()) {
                        System.out.println("Error al enviar confirmación. Servidor caído.");
                        manejarDesconexion();
                        break;
                    }
                    
                } catch (NumberFormatException nfe) {
                    System.out.println("⚠️ Mensaje no numérico recibido (puede ser comando): " + nfe.getMessage());
                    
                } catch (SocketException se) {
                    System.out.println("SocketException: Conexión perdida.");
                    Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, se);
                    manejarDesconexion();
                    break;
                    
                } catch (IOException ioe) {
                    System.out.println("IOException: Error de comunicación.");
                    Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, ioe);
                    manejarDesconexion();
                    break;
                    
                } catch (InterruptedException ie) {
                    System.out.println("Fertirrigación interrumpida");
                    Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, ie);
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado: " + ex.getMessage());
            Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo fertirrigación finalizado");
        }
    }
}
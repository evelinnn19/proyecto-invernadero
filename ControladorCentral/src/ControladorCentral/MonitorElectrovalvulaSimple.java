package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Monitor para electroválvulas simples (EV1 y EV2) que solo reciben comandos ON/OFF.
 * Ahora con soporte de heartbeat para mantener la conexión activa.
 */
public class MonitorElectrovalvulaSimple extends Thread {
    
    private Socket cliente;
    private BufferedReader br;
    private PrintWriter out; // 🆕 Necesario para enviar PING
    private String nombreValvula;
    private CoordinadorBomba bomba;
    private volatile boolean ejecutando;
    private boolean esValvulaFerti; // true = ferti, false = general
    private Thread hiloHeartbeat; // 🆕
    
    public MonitorElectrovalvulaSimple(Socket cliente, String nombre, CoordinadorBomba bomba, boolean esFerti) {
        this.cliente = cliente;
        this.nombreValvula = nombre;
        this.bomba = bomba;
        this.esValvulaFerti = esFerti;
        this.ejecutando = true;
        
        try {
            // Ahora necesitamos también el PrintWriter para enviar PING
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = new PrintWriter(cliente.getOutputStream(), true);
            
            // Configurar socket
            cliente.setKeepAlive(true);
            cliente.setSoTimeout(60000); // 🔧 60 segundos (enviaremos PING cada 15s)
            
        } catch (IOException e) {
            System.out.println("Error al configurar monitor " + nombreValvula + ": " + e.getMessage());
        }
    }
    
    // 🆕 Hilo que envía PING cada 15 segundos
    private void iniciarHeartbeat() {
        hiloHeartbeat = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    hiloHeartbeat.sleep(50000); // Cada 15 segundos
                    
                    if (ejecutando && !verificarEstadoSocket()) {
                        out.println("PING");
                        out.flush();
                        
                        if (out.checkError()) {
                            System.out.println("Error al enviar PING - " + nombreValvula);
                            manejarDesconexion();
                            break;
                        }
                        
                        System.out.println("Heartbeat enviado a " + nombreValvula);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Heartbeat interrumpido - " + nombreValvula);
            }
        });
        
        hiloHeartbeat.setDaemon(true);
        hiloHeartbeat.start();
    }
    
    @Override
    public void run() {
        try {
            // 🆕 Iniciar heartbeat
            iniciarHeartbeat();
            
            System.out.println("Monitor iniciado para " + nombreValvula + " (con heartbeat)");
            
            while (ejecutando) {
                try {
                    // Intentar leer del socket
                    // Si la válvula se desconecta, readLine() retornará null
                    String mensaje = br.readLine();
                    
                    if (mensaje == null) {
                        System.out.println(nombreValvula + " desconectada (readLine null)");
                        manejarDesconexion();
                        break;
                    }
                    
                    // Procesar mensajes del cliente
                    if (mensaje.equals("PING")) {
                        // La válvula envía PING para mantener conexión activa
                        out.println("PONG");
                        out.flush();
                        
                        if (out.checkError()) {
                            System.out.println("Error al enviar PONG a " + nombreValvula);
                            manejarDesconexion();
                            break;
                        }
                        
                        System.out.println("?PONG enviado a " + nombreValvula);
                        
                    } else if (mensaje.equals("PONG")) {
                        // Respuesta a nuestro PING
                        System.out.println("PONG recibido de " + nombreValvula);
                        
                    } else if (mensaje.startsWith("ACK_")) {
                        System.out.println(nombreValvula + " confirmó: " + mensaje);
                        
                    } else if (!mensaje.isEmpty()) {
                        System.out.println("Mensaje de " + nombreValvula + ": " + mensaje);
                    }
                    
                } catch (SocketException se) {
                    System.out.println("SocketException en " + nombreValvula + ": " + se.getMessage());
                    manejarDesconexion();
                    break;
                } catch (IOException ioe) {
                    System.out.println("IOException en " + nombreValvula + ": " + ioe.getMessage());
                    manejarDesconexion();
                    break;
                }
                
                // Verificar estado del socket periódicamente
                if (verificarEstadoSocket()) {
                    System.out.println("Socket cerrado - " + nombreValvula);
                    manejarDesconexion();
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en monitor " + nombreValvula + ": " + ex.getMessage());
            Logger.getLogger(MonitorElectrovalvulaSimple.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Monitor finalizado para " + nombreValvula);
        }
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
        System.out.println("⚠️ " + nombreValvula.toUpperCase() + " DESCONECTADA");
        
        ejecutando = false;
        
        // Notificar al CoordinadorBomba que la válvula se desconectó
        if (esValvulaFerti) {
            try {
                bomba.notificarDesconexionValvulaFerti();
            } catch (InterruptedException ex) {
                Logger.getLogger(MonitorElectrovalvulaSimple.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                bomba.notificarDesconexionValvulaGeneral();
            } catch (InterruptedException ex) {
                Logger.getLogger(MonitorElectrovalvulaSimple.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        cerrarRecursos();
    }
    
    private void cerrarRecursos() {
        try {
            if (br != null) {
                br.close();
            }
            if (out != null) {
                out.close();
            }
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos monitor " + nombreValvula + ": " + e.getMessage());
        }
    }
    
    public void detener() {
        ejecutando = false;
        if (hiloHeartbeat != null) {
            hiloHeartbeat.interrupt();
        }
        this.interrupt();
    }
}
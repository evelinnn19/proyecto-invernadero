package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Monitor para electroválvulas simples (EV1 y EV2) que solo reciben comandos ON/OFF.
 * No envía comandos, solo monitorea si la conexión sigue activa.
 */
public class MonitorElectrovalvulaSimple extends Thread {
    
    private Socket cliente;
    private BufferedReader br;
    private String nombreValvula;
    private CoordinadorBomba bomba;
    private volatile boolean ejecutando;
    private boolean esValvulaFerti; // true = ferti, false = general
    
    public MonitorElectrovalvulaSimple(Socket cliente, String nombre, CoordinadorBomba bomba, boolean esFerti) {
        this.cliente = cliente;
        this.nombreValvula = nombre;
        this.bomba = bomba;
        this.esValvulaFerti = esFerti;
        this.ejecutando = true;
        
        try {
            // Solo necesitamos leer para detectar desconexión
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            
            // Configurar socket
            cliente.setKeepAlive(true);
            cliente.setSoTimeout(0); // Sin timeout, usamos readLine() para detectar cierre
            
        } catch (IOException e) {
            System.out.println("Error al configurar monitor " + nombreValvula + ": " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            System.out.println("Monitor iniciado para " + nombreValvula);
            
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
                    
                    // Procesar mensajes opcionales del cliente (ACK, PING, etc.)
                    if (mensaje.equals("PING")) {
                        // La válvula envía PING para mantener conexión activa
                        //System.out.println("Heartbeat recibido de " + nombreValvula);
                    } else if (mensaje.startsWith("ACK_")) {
                        System.out.println( nombreValvula + " confirmó: " + mensaje);
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

        System.out.println(nombreValvula.toUpperCase() + " DESCONECTADA");

        
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
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos monitor " + nombreValvula + ": " + e.getMessage());
        }
    }
    
    public void detener() {
        ejecutando = false;
        this.interrupt();
    }
}
package electrovalvula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoElectrovalvula extends Thread {
    private volatile Boolean prendido;
    private Boolean estadoValvula; // true = abierta, false = cerrada
    private Socket clienteHumedadEnviar;
    private PrintWriter haciaServer;
    private BufferedReader br;
    private Thread hiloMonitorConexion;
    private Thread hiloHeartbeat; // 🆕
    
    public HiloSensadoElectrovalvula(Socket che, PrintWriter dos) {
        prendido = Boolean.TRUE;
        estadoValvula = Boolean.FALSE; // Inicialmente cerrada
        this.clienteHumedadEnviar = che;
        this.haciaServer = dos;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(che.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void apagar() {
        prendido = Boolean.FALSE;
        if (hiloMonitorConexion != null) {
            hiloMonitorConexion.interrupt();
        }
        if (hiloHeartbeat != null) {
            hiloHeartbeat.interrupt();
        }
    }
    
    public void encender() {
        prendido = Boolean.TRUE;
    }
    
    // 🆕 Hilo que envía PING cada 10 segundos
    private void iniciarHeartbeat() {
        hiloHeartbeat = new Thread(() -> {
            try {
                while (prendido && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10000); // Cada 10 segundos
                    
                    if (prendido && !verificarEstadoSocket()) {
                        haciaServer.println("PING");
                        haciaServer.flush();
                        
                        if (haciaServer.checkError()) {
                            System.out.println("Error al enviar PING. Servidor caído.");
                            manejarDesconexion();
                            break;
                        }
                        
                        System.out.println("?Heartbeat enviado al servidor");
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Heartbeat interrumpido.");
            }
        });
        
        hiloHeartbeat.setDaemon(true);
        hiloHeartbeat.start();
    }
    
    // Hilo monitor que verifica periódicamente el estado de la conexión
    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (prendido && !Thread.currentThread().isInterrupted()) {
                    // Verificar estado del socket cada 2 segundos
                    if (verificarEstadoSocket()) {
                        System.out.println("Monitor detectó desconexión.");
                        manejarDesconexion();
                        break;
                    }
                    
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor de conexión interrumpido.");
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }
    
    // Verifica múltiples condiciones del socket
    private boolean verificarEstadoSocket() {
        try {
            if (clienteHumedadEnviar.isClosed()) {
                System.out.println("Socket cerrado detectado.");
                return true;
            }
            
            if (!clienteHumedadEnviar.isConnected()) {
                System.out.println("Socket no conectado detectado.");
                return true;
            }
            
            if (clienteHumedadEnviar.isInputShutdown() || clienteHumedadEnviar.isOutputShutdown()) {
                System.out.println("Input/Output shutdown detectado.");
                return true;
            }
            
        } catch (Exception e) {
            System.out.println("Error al verificar estado del socket: " + e.getMessage());
            return true;
        }
        
        return false;
    }
    
    @Override
    public void run() {
        try {
            // 🆕 Iniciar heartbeat
            iniciarHeartbeat();
            // Iniciar el monitor de conexión en paralelo
            iniciarMonitorConexion();
            
            System.out.println("Electroválvula lista para recibir comandos (con heartbeat).");
            
            while (prendido) {
                // Verificar conexión antes de intentar leer
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida en el ciclo principal.");
                    manejarDesconexion();
                    break;
                }
                
                // Leer comando del servidor (bloqueante)
                String orden = br.readLine();
                
                // Si readLine() retorna null = servidor cerró la conexión
                if (orden == null) {
                    System.out.println("=== SERVIDOR CERRÓ LA CONEXIÓN (orden null) ===");
                    manejarDesconexion();
                    break;
                }
                
                // 🆕 Ignorar los PONG del servidor (respuesta a nuestros PING)
                if (orden.equals("PONG")) {
                    System.out.println("PONG recibido del servidor");
                    continue;
                }
                
                // Procesar comandos
                procesarComando(orden);
            }
            
        } catch (SocketException ex) {
            System.out.println("SocketException: Conexión perdida.");
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } catch (IOException ex) {
            System.out.println("IOException: Error de comunicación.");
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } catch (Exception ex) {
            System.out.println("Error inesperado: " + ex.getMessage());
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
        }
    }
    
    private void procesarComando(String orden) {
        System.out.println("Comando recibido: " + orden);
        
        if (orden.equalsIgnoreCase("ON")) {
            estadoValvula = true;
            System.out.println("Electroválvula ABIERTA (Riego Parcela activado)");
            
            // Confirmar al servidor
            haciaServer.println("ACK_ON");
            haciaServer.flush();
            
        } else if (orden.equalsIgnoreCase("OFF")) {
            estadoValvula = false;
            System.out.println("Electroválvula CERRADA (Riego Parcela detenido)");
            
            // Confirmar al servidor
            haciaServer.println("ACK_OFF");
            haciaServer.flush();
            
        } else if (orden.equalsIgnoreCase("STATUS")) {
            // Responder con el estado actual
            String estado = estadoValvula ? "OPEN" : "CLOSED";
            haciaServer.println("STATUS:" + estado);
            haciaServer.flush();
            System.out.println("Estado enviado: " + estado);
            
        } else if (orden.equals("PING")) {
            // El servidor también puede enviarnos PING
            haciaServer.println("PONG");
            haciaServer.flush();
            System.out.println("PONG enviado al servidor");
            
        } else {
            System.out.println("Comando desconocido: " + orden);
            haciaServer.println("ERROR:UNKNOWN_COMMAND");
            haciaServer.flush();
        }
        
        // Verificar si hubo error al enviar respuesta
        if (haciaServer.checkError()) {
            System.out.println("Error al enviar respuesta al servidor.");
            manejarDesconexion();
        }
    }
    
    private void manejarDesconexion() {
        System.out.println("=== SERVIDOR CAÍDO DETECTADO ===");
        System.out.println("Cerrando válvula por seguridad...");
        estadoValvula = false;
        prendido = Boolean.FALSE;
        cerrarRecursos();
        reconectar();
    }
    
    private void cerrarRecursos() {
        try {
            if (br != null) {
                br.close();
            }
            if (haciaServer != null) {
                haciaServer.close();
            }
            if (clienteHumedadEnviar != null && !clienteHumedadEnviar.isClosed()) {
                clienteHumedadEnviar.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos: " + e.getMessage());
        }
    }
    
    private void reconectar() {
        try {
            System.out.println("Iniciando proceso de reconexión...");
            ElectroValvula.restablecerConexion();
        } catch (InterruptedException e) {
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorElectrovalvula extends Thread {

    private Socket cliente;
    private BufferedReader br;
    private PrintWriter out;
    private double humedad;
    private int parcela;
    private DatosINR datos;
    private CoordinadorBomba bomba;
    private boolean esFerti;
    private Impresor imp;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;
    private Thread hiloHeartbeat; // 🆕

    public Impresor getImp() {
        return imp;
    }

    public void setImp(Impresor imp) {
        this.imp = imp;
    }

    public HiloReceptorElectrovalvula(Socket ch, DatosINR datos, int parcela, CoordinadorBomba bomba, boolean fer) {
        this.datos = datos;
        cliente = ch;
        this.bomba = bomba;
        this.parcela = parcela;
        this.esFerti = fer;
        this.ejecutando = true;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = new PrintWriter(cliente.getOutputStream(), true);
            
            // Configurar socket para detectar desconexiones
            cliente.setKeepAlive(true);
            cliente.setSoTimeout(60000); // 🔧 60 segundos (enviaremos PING cada 15s)
            cliente.setTcpNoDelay(true);
            
        } catch (IOException e) {
            System.out.println("Error al configurar socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int tiempoRiegoParcela(int parcela, double v1, double v2, double v3) {
        double INR = this.datos.calcularINR(parcela, v1, v2, v3);

        if (!this.datos.isSensorLluvia()) {
            if (INR > 0.7 && INR < 0.8) {
                System.out.println("Se regará por 5 minutos");
                return 5000;
            }
            if (INR >= 0.8 && INR < 0.9) {
                System.out.println("Se regará por 7 minutos");
                return 7000;
            } else {
                if (INR >= 0.9) {
                    System.out.println("Se regará por 10 minutos");
                    return 10000;
                }
            }
        }
        return 0;
    }

    // 🆕 Hilo que envía PING periódicamente
    private void iniciarHeartbeat() {
        hiloHeartbeat = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(15000); // Cada 15 segundos
                    
                    if (ejecutando && !verificarEstadoSocket()) {
                        out.println("PING");
                        out.flush();
                        
                        if (out.checkError()) {
                            System.out.println("Error al enviar PING - Parcela " + parcela);
                            manejarDesconexion();
                            break;
                        }
                        
                        System.out.println("Heartbeat enviado a parcela " + parcela);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Heartbeat interrumpido - Parcela " + parcela);
            }
        });
        
        hiloHeartbeat.setDaemon(true);
        hiloHeartbeat.start();
    }

    // Monitor que escucha mensajes entrantes del cliente (PING, ACK, etc.)
    private void iniciarMonitorMensajes() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    // Verificar si hay datos disponibles para leer
                    if (br.ready()) {
                        String mensaje = br.readLine();
                        
                        if (mensaje == null) {
                            System.out.println("Cliente desconectado (null) - Parcela " + parcela);
                            manejarDesconexion();
                            break;
                        }
                        
                        procesarMensaje(mensaje);
                    }
                    
                    // Verificar estado del socket
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Parcela " + parcela);
                        manejarDesconexion();
                        break;
                    }
                    
                    Thread.sleep(500); // Verificar cada medio segundo
                }
            } catch (IOException e) {
                System.out.println("Error de I/O en monitor - Parcela " + parcela + ": " + e.getMessage());
                manejarDesconexion();
            } catch (InterruptedException e) {
                System.out.println("Monitor de mensajes interrumpido - Parcela " + parcela);
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }

    private void procesarMensaje(String mensaje) {
        if (mensaje.equals("PING")) {
            // Responder al heartbeat del cliente
            out.println("PONG");
            out.flush();
            
            if (out.checkError()) {
                System.out.println("Error al enviar PONG - Parcela " + parcela);
                manejarDesconexion();
            } else {
                System.out.println("?PONG enviado a parcela " + parcela);
            }
            
        } else if (mensaje.startsWith("ACK_")) {
            // Confirmación recibida
            //System.out.println("Comando confirmado en parcela " + parcela + ": " + mensaje);
            
        } else if (mensaje.startsWith("STATUS:")) {
            // Estado de la válvula
            String estado = mensaje.substring(7);
            System.out.println("Estado válvula parcela " + parcela + ": " + estado);
            
        } else if (mensaje.startsWith("ERROR:")) {
            // Error reportado por el cliente
            System.out.println("Error en parcela " + parcela + ": " + mensaje);
            
        } else {
            System.out.println("Mensaje desconocido de parcela " + parcela + ": " + mensaje);
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
        System.out.println("CLIENTE DESCONECTADO - Parcela " + parcela);
        
        ejecutando = false;
        
        // Asegurar que la válvula esté cerrada
        try {
            if (bomba != null) {
                bomba.terminarRiego(parcela);
                System.out.println("Riego detenido por seguridad en parcela " + parcela);
            }
        } catch (Exception e) {
            System.out.println("Error al detener riego: " + e.getMessage());
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
            System.out.println("Error al cerrar recursos: " + e.getMessage());
        }
    }

    public void detener() {
        ejecutando = false;
        if (hiloMonitorConexion != null) {
            hiloMonitorConexion.interrupt();
        }
        if (hiloHeartbeat != null) {
            hiloHeartbeat.interrupt();
        }
        cerrarRecursos();
    }

    @Override
    public void run() {
        try {
            // 🆕 Iniciar heartbeat
            iniciarHeartbeat();
            // Iniciar el monitor de mensajes entrantes
            iniciarMonitorMensajes();
            
            System.out.println("Servidor listo para controlar parcela " + parcela + " (con heartbeat)");
            
            int tiempo;
            
            while (ejecutando) {
                // Verificar conexión antes de operar
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida en ciclo principal - Parcela " + parcela);
                    manejarDesconexion();
                    break;
                }

                try {
                    if (this.datos.isSensorLluvia()) {
                        if (imp != null) {
                            imp.setTiempos(this.parcela, 0);
                        }
                        System.out.println("Lloviendo - No se riega parcela " + parcela);
                        out.println("OFF");
                        Thread.sleep(5000);
                        
                    } else {
                        tiempo = tiempoRiegoParcela(this.parcela, 0.5, 0.1, 0.5);
                        
                        if (imp != null) {
                            imp.setTiempos(this.parcela, tiempo);
                        }
                        
                        if (tiempo > 0) {
                            this.bomba.iniciarRiego(parcela);
                            
                            // Enviar comando ON
                            out.println("ON");
                            out.flush();
                            
                            // Verificar si se envió correctamente
                            if (out.checkError()) {
                                System.out.println("Error al enviar ON - Parcela " + parcela);
                                manejarDesconexion();
                                break;
                            }
                            
                            System.out.println("Riego iniciado en parcela " + parcela + " por " + (tiempo/1000) + " segundos");
                            
                            Thread.sleep(tiempo);
                            
                            this.bomba.terminarRiego(parcela);
                            
                            // Enviar comando OFF
                            out.println("OFF");
                            out.flush();
                            
                            // Verificar si se envió correctamente
                            if (out.checkError()) {
                                System.out.println("Error al enviar OFF - Parcela " + parcela);
                                manejarDesconexion();
                                break;
                            }
                            
                            System.out.println("Riego finalizado en parcela " + parcela);
                            
                            // Espera un tiempo hasta volver a regar
                            Thread.sleep(5000);
                        } else {
                            // No hay necesidad de riego
                            Thread.sleep(5000);
                        }
                    }

                } catch (InterruptedException ex) {
                    System.out.println("Hilo interrumpido - Parcela " + parcela);
                    Logger.getLogger(HiloReceptorElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en parcela " + parcela + ": " + ex.getMessage());
            Logger.getLogger(HiloReceptorElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo receptor finalizado - Parcela " + parcela);
        }
    }
}
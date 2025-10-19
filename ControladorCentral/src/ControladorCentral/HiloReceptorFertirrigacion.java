package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorFertirrigacion extends Thread {

    private Socket cliente;
    private BufferedReader br;
    private PrintWriter out;
    private CoordinadorBomba bomba;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;
    private volatile boolean fertirrigacionActiva;

    public HiloReceptorFertirrigacion(Socket ch, CoordinadorBomba bomba) {
        this.cliente = ch;
        this.bomba = bomba;
        this.ejecutando = true;
        this.fertirrigacionActiva = false;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = new PrintWriter(cliente.getOutputStream(), true);
            
            // Configurar socket para detectar desconexiones
            cliente.setKeepAlive(true);
            cliente.setSoTimeout(20000); // 20 segundos timeout
            cliente.setTcpNoDelay(true);
            
        } catch (IOException e) {
            System.out.println("Error al configurar socket fertirrigación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Monitor que escucha mensajes entrantes del cliente (PING, ACK, etc.)
    private void iniciarMonitorMensajes() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    // Verificar si hay datos disponibles para leer sin bloquear el hilo principal
                    if (br.ready()) {
                        // El hilo principal ya está leyendo, no hacemos nada aquí
                        // Solo verificamos el estado del socket
                    }
                    
                    // Verificar estado del socket
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Sistema Fertirrigación");
                        manejarDesconexion();
                        break;
                    }
                    
                    Thread.sleep(1000); // Verificar cada segundo
                }
            } catch (IOException e) {
                System.out.println("Error de I/O en monitor fertirrigación: " + e.getMessage());
                manejarDesconexion();
            } catch (InterruptedException e) {
                System.out.println("Monitor de mensajes interrumpido - Fertirrigación");
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
        System.out.println("SISTEMA FERTIRRIGACIÓN DESCONECTADO");
        
        ejecutando = false;
        
        // CRÍTICO: Si estaba fertirrigando, terminar el proceso
        if (fertirrigacionActiva) {
            System.out.println("Terminando fertirrigación por seguridad...");
            try {
                bomba.terminaFertirrigacion();
                fertirrigacionActiva = false;
                System.out.println("Fertirrigación detenida por desconexión");
            } catch (Exception e) {
                System.out.println("Error al detener fertirrigación: " + e.getMessage());
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
            System.out.println("Error al cerrar recursos fertirrigación: " + e.getMessage());
        }
    }

    public void detener() {
        ejecutando = false;
        if (hiloMonitorConexion != null) {
            hiloMonitorConexion.interrupt();
        }
        if (fertirrigacionActiva) {
            bomba.terminaFertirrigacion();
            fertirrigacionActiva = false;
        }
        cerrarRecursos();
    }

    @Override
    public void run() {
        try {
            // Iniciar el monitor de mensajes entrantes
            iniciarMonitorMensajes();
            
            System.out.println("Receptor de Sistema Fertirrigación iniciado");
            
            while (ejecutando) {
                // Verificar conexión antes de operar
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida en ciclo principal - Fertirrigación");
                    manejarDesconexion();
                    break;
                }
                
                try {
                    System.out.println("Solicitando inicio de fertirrigación...");
                    
                    // Iniciar fertirrigación (puede bloquearse esperando recursos)
                    this.bomba.iniciarFertirrigacion();
                    fertirrigacionActiva = true;
                    
                    // Enviar tiempo de fertirrigación al cliente
                    out.println(10000);
                    out.flush();
                    
                    // Verificar si se envió correctamente
                    if (out.checkError()) {
                        System.out.println("Error al enviar tiempo de fertirrigación");
                        manejarDesconexion();
                        break;
                    }
                    
                    System.out.println("Tiempo de fertirrigación enviado: 10 segundos");
                    
                    // Esperar confirmación del cliente
                    String respuesta = br.readLine();
                    
                    // Verificar si el cliente se desconectó
                    if (respuesta == null) {
                        System.out.println("Cliente fertirrigación envió null (desconectado)");
                        manejarDesconexion();
                        break;
                    }
                    
                    if ("FERTI_OK".equals(respuesta)) {
                        this.bomba.terminaFertirrigacion();
                        fertirrigacionActiva = false;
                        System.out.println("Fertirrigación completada exitosamente");
                        System.out.println("Pausa fertirrigación por: 20 segundos");
                        Thread.sleep(20000);
                    } else {
                        System.out.println("Respuesta inesperada del cliente: " + respuesta);
                        // Aún así terminamos la fertirrigación por seguridad
                        if (fertirrigacionActiva) {
                            this.bomba.terminaFertirrigacion();
                            fertirrigacionActiva = false;
                        }
                    }
                    
                } catch (InterruptedException ex) {
                    System.out.println("Hilo fertirrigación interrumpido");
                    Logger.getLogger(HiloReceptorFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
                    if (fertirrigacionActiva) {
                        bomba.terminaFertirrigacion();
                        fertirrigacionActiva = false;
                    }
                    break;
                } catch (SocketException se) {
                    System.out.println("SocketException en fertirrigación: " + se.getMessage());
                    manejarDesconexion();
                    break;
                } catch (IOException ioe) {
                    System.out.println("IOException en fertirrigación: " + ioe.getMessage());
                    manejarDesconexion();
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en fertirrigación: " + ex.getMessage());
            Logger.getLogger(HiloReceptorFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo receptor fertirrigación finalizado");
        }
    }
}
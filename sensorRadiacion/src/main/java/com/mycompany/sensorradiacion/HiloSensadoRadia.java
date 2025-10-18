package com.mycompany.sensorradiacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoRadia extends Thread {


    
  
    private volatile Boolean prendido;
    private Double radiacion;
    private Socket socketClienteRadiacion;
    private PrintWriter haciaServer;
    private BufferedReader desdeServer;
    private Thread hiloMonitorConexion;
    
    

    public HiloSensadoRadia(Socket socketClienteRadiacion, PrintWriter haciaServer) {
        prendido = Boolean.TRUE;
        this.socketClienteRadiacion = socketClienteRadiacion;
        this.haciaServer = haciaServer;
        try {
            // Crear reader para detectar cuando el servidor cierra la conexión
        this.desdeServer = new BufferedReader(new InputStreamReader(this.socketClienteRadiacion.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error al crear BufferedReader: " + e.getMessage());
        }
        
    }

    public void generarRadiacionAleatoria() {
        Random random = new Random();
        radiacion = random.nextDouble(0, 1000);
    }

    public void apagar() {
        prendido = Boolean.FALSE;
        if (hiloMonitorConexion != null) {
            hiloMonitorConexion.interrupt();
        }
    }

    public void encender() {
        prendido = Boolean.TRUE;
    }
    
    // Hilo que monitorea constantemente la conexión
    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (prendido && !Thread.currentThread().isInterrupted()) {
                    // Intentar leer del servidor (se bloqueará hasta recibir algo o detectar cierre)
                    if (desdeServer.ready()) {
                        String respuesta = desdeServer.readLine();
                        if (respuesta == null) {
                            // Si readLine() retorna null, el servidor cerró la conexión
                            System.out.println("El servidor cerró la conexión (respuesta null).");
                            manejarDesconexion();
                            break;
                        }
                        System.out.println("Mensaje del servidor: " + respuesta);
                    }
                    
                    // Verificar estado del socket periódicamente
                    if (verificarEstadoSocket()) {
                        manejarDesconexion();
                        break;
                    }
                    
                    Thread.sleep(1000); // Verificar cada segundo
                }
            } catch (IOException e) {
                System.out.println("Error de I/O en monitor: " + e.getMessage());
                manejarDesconexion();
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
            if (socketClienteRadiacion.isClosed()) {
                System.out.println("Socket cerrado detectado.");
                return true;
            }
            
            if (!socketClienteRadiacion.isConnected()) {
                System.out.println("Socket no conectado detectado.");
                return true;
            }
            
            if (socketClienteRadiacion.isInputShutdown() || socketClienteRadiacion.isOutputShutdown()) {
                System.out.println("Input/Output shutdown detectado.");
                return true;
            }
            
            // Enviar un byte "vacío" para verificar si la conexión está viva
            // El PrintWriter checkError() es más confiable para esto
            
        } catch (Exception e) {
            System.out.println("Error al verificar estado del socket: " + e.getMessage());
            return true;
        }
        
        return false;
    }
    
    
    
        private void manejarDesconexion() {
        System.out.println("=== SERVIDOR CAÍDO DETECTADO ===");
        prendido = Boolean.FALSE;
        cerrarRecursos();
        reconectar();
    }
    
    private void cerrarRecursos() {
        try {
            if (desdeServer != null) {
                desdeServer.close();
            }
            if (haciaServer != null) {
                haciaServer.close();
            }
            if (socketClienteRadiacion != null && !socketClienteRadiacion.isClosed()) {
                socketClienteRadiacion.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos: " + e.getMessage());
        }
    }
    
    private void reconectar() {
        try {
            System.out.println("Iniciando proceso de reconexión...");
            SensorRadiacion.restablecerConexion();
        } catch (InterruptedException e) {
            Logger.getLogger(SensorRadiacion.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    
    public void run() {
        try {
            // Iniciar el hilo que monitorea la conexión constantemente
            iniciarMonitorConexion();
            
            while (prendido) {
                // Verificar conexión antes de enviar
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida detectada en el ciclo principal.");
                    manejarDesconexion();
                    break;
                }
                
                generarRadiacionAleatoria();
                haciaServer.println(radiacion);
                haciaServer.flush();
                
                // CRÍTICO: checkError() detecta si hubo problemas al escribir
                if (haciaServer.checkError()) {
                    System.out.println("Error al enviar datos. Servidor posiblemente caído.");
                    manejarDesconexion();
                    break;
                }
                
                System.out.println("La humedad desde el dispositivo es: " + radiacion);
                Thread.sleep(5000);
            }
        } catch (InterruptedException ex) {
            System.out.println("Hilo interrumpido.");
            Logger.getLogger(HiloSensadoRadia.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            System.out.println("Error inesperado: " + ex.getMessage());
            Logger.getLogger(HiloSensadoRadia.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
        }
    }
}
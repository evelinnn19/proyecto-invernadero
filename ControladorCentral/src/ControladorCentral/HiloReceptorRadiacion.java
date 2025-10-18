package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorRadiacion extends Thread {

    private Socket clienteRadiacion;
    private BufferedReader br;
    private PrintWriter out;
    private double radiacion;
    private DatosINR datos;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;

    public HiloReceptorRadiacion(Socket ch, DatosINR datos) {
        this.datos = datos;
        clienteRadiacion = ch;
        this.ejecutando = true;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteRadiacion.getInputStream()));
            this.out = new PrintWriter(clienteRadiacion.getOutputStream(), true);
            
            // Configurar socket para detectar desconexiones
            clienteRadiacion.setKeepAlive(true);
            clienteRadiacion.setSoTimeout(20000);
            clienteRadiacion.setTcpNoDelay(true);
            
        } catch (IOException e) {
            System.out.println("Error al configurar socket sensor radiación: " + e.getMessage());
        }
    }

    public double getRadiacion() {
        return radiacion;
    }

    public void setRadiacion(double radiacion) {
        this.radiacion = radiacion;
    }

    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Sensor Radiación");
                        manejarDesconexion();
                        break;
                    }
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor de conexión interrumpido - Sensor Radiación");
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }

    private boolean verificarEstadoSocket() {
        try {
            return clienteRadiacion.isClosed() || 
                   !clienteRadiacion.isConnected() || 
                   clienteRadiacion.isInputShutdown() || 
                   clienteRadiacion.isOutputShutdown();
        } catch (Exception e) {
            return true;
        }
    }

    private void manejarDesconexion() {
        System.out.println("SENSOR RADIACIÓN DESCONECTADO");

        
        ejecutando = false;
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
            if (clienteRadiacion != null && !clienteRadiacion.isClosed()) {
                clienteRadiacion.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos sensor radiación: " + e.getMessage());
        }
    }

    public void detener() {
        ejecutando = false;
        if (hiloMonitorConexion != null) {
            hiloMonitorConexion.interrupt();
        }
        cerrarRecursos();
    }

    @Override
    public void run() {
        try {
            iniciarMonitorConexion();
            System.out.println("Receptor de Sensor Radiación iniciado");
            
            while (ejecutando) {
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida - Sensor Radiación");
                    manejarDesconexion();
                    break;
                }
                
                try {
                    String entrada = br.readLine();
                    
                    if (entrada == null) {
                        System.out.println("Sensor Radiación envió null (desconectado)");
                        manejarDesconexion();
                        break;
                    }
                    
                    setRadiacion(Double.parseDouble(entrada));
                    this.datos.setSensorRad(radiacion);
                    
                    // System.out.println("☀️ Radiación: " + radiacion + " W/m²");
                    
                } catch (NumberFormatException nfe) {
                    System.out.println("Dato inválido de sensor radiación: " + nfe.getMessage());
                } catch (SocketException se) {
                    System.out.println("SocketException en sensor radiación: " + se.getMessage());
                    manejarDesconexion();
                    break;
                } catch (IOException ioe) {
                    System.out.println("IOException en sensor radiación: " + ioe.getMessage());
                    manejarDesconexion();
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en sensor radiación: " + ex.getMessage());
            Logger.getLogger(HiloReceptorRadiacion.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo receptor radiación finalizado");
        }
    }
}
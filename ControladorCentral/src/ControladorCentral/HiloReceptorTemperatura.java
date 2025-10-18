package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorTemperatura extends Thread {

    private Socket clienteTemperatura;
    private BufferedReader br;
    private PrintWriter out;
    private double temperatura;
    private DatosINR datos;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;

    public HiloReceptorTemperatura(Socket ch, DatosINR datos) {
        this.datos = datos;
        clienteTemperatura = ch;
        this.ejecutando = true;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteTemperatura.getInputStream()));
            this.out = new PrintWriter(clienteTemperatura.getOutputStream(), true);
            
            // Configurar socket para detectar desconexiones
            clienteTemperatura.setKeepAlive(true);
            clienteTemperatura.setSoTimeout(20000);
            clienteTemperatura.setTcpNoDelay(true);
            
        } catch (IOException e) {
            System.out.println("Error al configurar socket sensor temperatura: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Sensor Temperatura");
                        manejarDesconexion();
                        break;
                    }
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor de conexión interrumpido - Sensor Temperatura");
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }

    private boolean verificarEstadoSocket() {
        try {
            return clienteTemperatura.isClosed() || 
                   !clienteTemperatura.isConnected() || 
                   clienteTemperatura.isInputShutdown() || 
                   clienteTemperatura.isOutputShutdown();
        } catch (Exception e) {
            return true;
        }
    }

    private void manejarDesconexion() {
        System.out.println("SENSOR TEMPERATURA DESCONECTADO");
  
        
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
            if (clienteTemperatura != null && !clienteTemperatura.isClosed()) {
                clienteTemperatura.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos sensor temperatura: " + e.getMessage());
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
            System.out.println("Receptor de Sensor Temperatura iniciado");
            
            while (ejecutando) {
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida - Sensor Temperatura");
                    manejarDesconexion();
                    break;
                }
                
                try {
                    String entrada = br.readLine();
                    
                    if (entrada == null) {
                        System.out.println("Sensor Temperatura envió null (desconectado)");
                        manejarDesconexion();
                        break;
                    }
                    
                    setTemperatura(Double.parseDouble(entrada));
                    this.datos.setSensorTemp(temperatura);
                    
                    // System.out.println("🌡️ Temperatura: " + temperatura + "°C");
                    
                } catch (NumberFormatException nfe) {
                    System.out.println("Dato inválido de sensor temperatura: " + nfe.getMessage());
                } catch (SocketException se) {
                    System.out.println("SocketException en sensor temperatura: " + se.getMessage());
                    manejarDesconexion();
                    break;
                } catch (IOException ioe) {
                    System.out.println("IOException en sensor temperatura: " + ioe.getMessage());
                    manejarDesconexion();
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en sensor temperatura: " + ex.getMessage());
            Logger.getLogger(HiloReceptorTemperatura.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo receptor temperatura finalizado");
        }
    }
}
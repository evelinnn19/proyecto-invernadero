package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorHumedad extends Thread {

    private int parcela;
    private double humedad;
    private Socket clienteHumedad;
    private BufferedReader br;
    private PrintWriter out;
    private DatosINR datos;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;

    public HiloReceptorHumedad(Socket ch, DatosINR datos, int parcela) {
        this.datos = datos;
        this.clienteHumedad = ch;
        this.parcela = parcela;
        this.ejecutando = true;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteHumedad.getInputStream()));
            this.out = new PrintWriter(clienteHumedad.getOutputStream(), true);
            
            // Configurar socket para detectar desconexiones
            clienteHumedad.setKeepAlive(true);
            clienteHumedad.setSoTimeout(20000); // 20 segundos timeout
            clienteHumedad.setTcpNoDelay(true);
            
        } catch (IOException e) {
            System.out.println("Error al configurar socket sensor humedad " + parcela + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getParcela() {
        return parcela;
    }

    public double getHumedad() {
        return humedad;
    }

    public void setHumedad(double humedad) {
        this.humedad = humedad;
    }

    public void setDatosSensorHumedad(int parcela) {
        switch (parcela) {
            case 1:
                this.datos.setSensorH1(humedad);
                break;
            case 2:
                this.datos.setSensorH2(humedad);
                break;
            case 3:
                this.datos.setSensorH3(humedad);
                break;
            case 4:
                this.datos.setSensorH4(humedad);
                break;
            case 5:
                this.datos.setSensorH5(humedad);
                break;
        }
    }

    // Monitor que verifica el estado de la conexión
    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    // Verificar estado del socket periódicamente
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Sensor Humedad " + parcela);
                        manejarDesconexion();
                        break;
                    }
                    
                    Thread.sleep(2000); // Verificar cada 2 segundos
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor de conexión interrumpido - Sensor Humedad " + parcela);
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }

    private boolean verificarEstadoSocket() {
        try {
            return clienteHumedad.isClosed() || 
                   !clienteHumedad.isConnected() || 
                   clienteHumedad.isInputShutdown() || 
                   clienteHumedad.isOutputShutdown();
        } catch (Exception e) {
            return true;
        }
    }

    private void manejarDesconexion() {
        System.out.println("SENSOR HUMEDAD " + parcela + " DESCONECTADO");

        
        ejecutando = false;
        
        // Opcional: Establecer un valor por defecto o flag
        // this.datos.setSensorH(parcela, -1.0); // Indicar sensor desconectado
        
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
            if (clienteHumedad != null && !clienteHumedad.isClosed()) {
                clienteHumedad.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos sensor humedad " + parcela + ": " + e.getMessage());
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
            // Iniciar el monitor de conexión
            iniciarMonitorConexion();
            
            System.out.println("Receptor de Sensor Humedad " + parcela + " iniciado");
            
            while (ejecutando) {
                // Verificar conexión antes de leer
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida en ciclo principal - Sensor Humedad " + parcela);
                    manejarDesconexion();
                    break;
                }
                
                try {
                    String entrada = br.readLine();
                    
                    // CRÍTICO: Verificar si el sensor se desconectó
                    if (entrada == null) {
                        System.out.println("Sensor Humedad " + parcela + " envió null (desconectado)");
                        manejarDesconexion();
                        break;
                    }
                    
                    // Parsear y actualizar datos
                    setHumedad(Double.parseDouble(entrada));
                    setDatosSensorHumedad(parcela);
                    
                    // Log opcional (comentar si es muy verbose)
                    // System.out.println("📊 Humedad parcela " + parcela + ": " + humedad + "%");
                    
                } catch (NumberFormatException nfe) {
                    System.out.println("Dato inválido de sensor humedad " + parcela + ": " + nfe.getMessage());
                    // Continuar ejecutando, puede ser un error temporal
                } catch (SocketException se) {
                    System.out.println("SocketException en sensor humedad " + parcela + ": " + se.getMessage());
                    manejarDesconexion();
                    break;
                } catch (IOException ioe) {
                    System.out.println("IOException en sensor humedad " + parcela + ": " + ioe.getMessage());
                    manejarDesconexion();
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en sensor humedad " + parcela + ": " + ex.getMessage());
            Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo receptor humedad " + parcela + " finalizado");
        }
    }
}
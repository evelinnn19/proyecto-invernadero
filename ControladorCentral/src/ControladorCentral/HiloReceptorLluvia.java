package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorLluvia extends Thread {

    private Socket clienteLluvia;
    private BufferedReader br;
    private PrintWriter out;
    private boolean llueve;
    private DatosINR datos;
    private volatile boolean ejecutando;
    private Thread hiloMonitorConexion;

    public HiloReceptorLluvia(Socket ch, DatosINR datos) {
        this.datos = datos;
        clienteLluvia = ch;
        this.ejecutando = true;
        
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteLluvia.getInputStream()));
            this.out = new PrintWriter(clienteLluvia.getOutputStream(), true);
            
            // Configurar socket para detectar desconexiones
            clienteLluvia.setKeepAlive(true);
            clienteLluvia.setSoTimeout(30000); // 30 segundos (más tiempo porque envía cada 25s)
            clienteLluvia.setTcpNoDelay(true);
            
        } catch (IOException e) {
            System.out.println("Error al configurar socket sensor lluvia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean getLlueve() {
        return llueve;
    }

    public void setLlueve(boolean llueve) {
        this.llueve = llueve;
    }

    private void iniciarMonitorConexion() {
        hiloMonitorConexion = new Thread(() -> {
            try {
                while (ejecutando && !Thread.currentThread().isInterrupted()) {
                    if (verificarEstadoSocket()) {
                        System.out.println("Socket cerrado - Sensor Lluvia");
                        manejarDesconexion();
                        break;
                    }
                    Thread.sleep(3000); // Verificar cada 3 segundos
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor de conexión interrumpido - Sensor Lluvia");
            }
        });
        
        hiloMonitorConexion.setDaemon(true);
        hiloMonitorConexion.start();
    }

    private boolean verificarEstadoSocket() {
        try {
            return clienteLluvia.isClosed() || 
                   !clienteLluvia.isConnected() || 
                   clienteLluvia.isInputShutdown() || 
                   clienteLluvia.isOutputShutdown();
        } catch (Exception e) {
            return true;
        }
    }

    private void manejarDesconexion() {
        System.out.println("⚠️  SENSOR LLUVIA DESCONECTADO");

        ejecutando = false;
        
        // IMPORTANTE: Establecer estado seguro (asumir que NO llueve al desconectar)
        // Para evitar que el riego se detenga innecesariamente
        // O podrías poner true si prefieres pecar de precavido
        this.datos.setSensorLluvia(false);
        
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
            if (clienteLluvia != null && !clienteLluvia.isClosed()) {
                clienteLluvia.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos sensor lluvia: " + e.getMessage());
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
            System.out.println("Receptor de Sensor Lluvia iniciado");
            
            while (ejecutando) {
                if (verificarEstadoSocket()) {
                    System.out.println("Conexión perdida - Sensor Lluvia");
                    manejarDesconexion();
                    break;
                }
                
                try {
                    String entrada = br.readLine();
                    
                    if (entrada == null) {
                        System.out.println("Sensor Lluvia envió null (desconectado)");
                        manejarDesconexion();
                        break;
                    }
                    
                    setLlueve(Boolean.parseBoolean(entrada));
                    this.datos.setSensorLluvia(llueve);
                    
                    if (llueve) {
                        System.out.println("¡LLUVIA DETECTADA! - Riego inhibido");
                    }
                    
                } catch (SocketException se) {
                    System.out.println("SocketException en sensor lluvia: " + se.getMessage());
                    manejarDesconexion();
                    break;
                } catch (IOException ioe) {
                    System.out.println("IOException en sensor lluvia: " + ioe.getMessage());
                    manejarDesconexion();
                    break;
                }
            }
            
        } catch (Exception ex) {
            System.out.println("Error inesperado en sensor lluvia: " + ex.getMessage());
            Logger.getLogger(HiloReceptorLluvia.class.getName()).log(Level.SEVERE, null, ex);
            manejarDesconexion();
        } finally {
            cerrarRecursos();
            System.out.println("Hilo receptor lluvia finalizado");
        }
    }
}
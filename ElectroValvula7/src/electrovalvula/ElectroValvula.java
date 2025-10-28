package electrovalvula;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElectroValvula {
    private static final int MAX_INTENTOS_RECONEXION = Integer.MAX_VALUE;
    private static final int TIEMPO_ESPERA_RECONEXION = 5000;
    private static final int TIMEOUT_CONEXION = 3000;
    private static final int TIMEOUT_SOCKET = 15000;
    private static HiloSensadoElectrovalvula electrovalvulaActual = null;
    private static volatile boolean sistemaActivo = true;
    
    public static void restablecerConexion() throws InterruptedException {
        int intentos = 0;
        
        while (sistemaActivo && intentos < MAX_INTENTOS_RECONEXION) {
            try {
                intentos++;
                System.out.println("\nI----------------------------------------I");
                System.out.println("I  Intento de reconexión #" + intentos + "I");
                System.out.println("I-----------------------------------------I");
                
                // Crear socket con timeout de conexión
                Socket cliente = new Socket();
                cliente.connect(new java.net.InetSocketAddress(
                    InetAddress.getByName("localhost"), 20000), TIMEOUT_CONEXION);
                
                // Configurar socket para detectar problemas rápidamente
                cliente.setKeepAlive(true);
                cliente.setSoTimeout(TIMEOUT_SOCKET);
                cliente.setTcpNoDelay(true);
                
                System.out.println("Socket creado: " + cliente);
                
                PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);
                outToServer.println("electroValvula7");
                outToServer.flush();
                
                // Verificar que el mensaje se envió correctamente
                if (outToServer.checkError()) {
                    System.out.println("Error al enviar identificación al servidor.");
                    cliente.close();
                    throw new IOException("No se pudo enviar identificación");
                }
                
                System.out.println("✓ Identificación enviada al servidor");
                
                // Detener el hilo anterior si existe y está vivo
                if (electrovalvulaActual != null && electrovalvulaActual.isAlive()) {
                    System.out.println("Deteniendo hilo anterior...");
                    electrovalvulaActual.apagar();
                    electrovalvulaActual.join(2000);
                }
                
                // Crear y arrancar nuevo hilo
                electrovalvulaActual = new HiloSensadoElectrovalvula(cliente, outToServer);
                electrovalvulaActual.start();
                
                System.out.println("Reconexión exitosa. Electroválvula operativa.\n");
                return;
                
            } catch (ConnectException e) {
                System.out.println("Servidor no disponible: " + e.getMessage());
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout al conectar: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error de I/O: " + e.getMessage());
                Logger.getLogger(ElectroValvula.class.getName()).log(Level.WARNING, 
                    "Error en intento de reconexión #" + intentos, e);
            }
            
            // Esperar antes de reintentar (backoff exponencial)
            if (sistemaActivo) {
                int tiempoEspera = Math.min(TIEMPO_ESPERA_RECONEXION * (1 + intentos / 10), 30000);
                System.out.println("Esperando " + (tiempoEspera/1000) + " segundos antes de reintentar...\n");
                Thread.sleep(tiempoEspera);
            }
        }
        
        if (!sistemaActivo) {
            System.out.println("Sistema detenido por el usuario.");
        } else {
            System.out.println("No se pudo restablecer la conexión después de " + intentos + " intentos.");
        }
    }
    
    public static void detenerSistema() {
        sistemaActivo = false;
        if (electrovalvulaActual != null) {
            electrovalvulaActual.apagar();
        }
    }
    
    public static void main(String[] args) {
        // Hook para manejar cierre graceful con Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nSeñal de cierre recibida. Deteniendo sistema...");
            detenerSistema();
        }));
        
        try {
            System.out.println("I-----------------------------------------I");
            System.out.println("I    ELECTROVÁLVULA - INICIANDO           I");
            System.out.println("I-----------------------------------------I\n");
            
            Socket cliente = new Socket();
            cliente.connect(new java.net.InetSocketAddress(
                InetAddress.getByName("localhost"), 20000), TIMEOUT_CONEXION);
            
            cliente.setKeepAlive(true);
            cliente.setSoTimeout(TIMEOUT_SOCKET);
            cliente.setTcpNoDelay(true);
            
            System.out.println("Cliente conectado: " + cliente);
            
            PrintWriter outToServer = new PrintWriter(cliente.getOutputStream(), true);
            outToServer.println("electroValvula7");
            outToServer.flush();
            
            if (outToServer.checkError()) {
                System.out.println("Error al enviar identificación inicial.");
                throw new IOException("Error en identificación inicial");
            }
            
            System.out.println("Identificación enviada");
            System.out.println("Sistema operativo\n");
            
            electrovalvulaActual = new HiloSensadoElectrovalvula(cliente, outToServer);
            electrovalvulaActual.start();
            
        } catch (ConnectException ex) {
            System.out.println("No se pudo conectar al servidor: " + ex.getMessage());
            System.out.println("Iniciando modo de reconexión automática...\n");
            try {
                restablecerConexion();
            } catch (InterruptedException e) {
                Logger.getLogger(ElectroValvula.class.getName()).log(Level.SEVERE, null, e);
            }
        } catch (SocketTimeoutException ex) {
            System.out.println("Timeout en conexión inicial: " + ex.getMessage());
            try {
                restablecerConexion();
            } catch (InterruptedException e) {
                Logger.getLogger(ElectroValvula.class.getName()).log(Level.SEVERE, null, e);
            }
        } catch (IOException ex) {
            System.out.println("Error en la conexión inicial: " + ex.getMessage());
            Logger.getLogger(ElectroValvula.class.getName()).log(Level.SEVERE, null, ex);
            try {
                restablecerConexion();
            } catch (InterruptedException e) {
                Logger.getLogger(ElectroValvula.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
}
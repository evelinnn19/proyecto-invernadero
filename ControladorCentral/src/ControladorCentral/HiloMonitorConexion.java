/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ControladorCentral;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author Evelin
 * Se encarga de monitorear la conexion del controlador central (principal) --> puerto 5050
 * Si no responde, ejecuta un failover.
 */
public class HiloMonitorConexion extends Thread {
    private final String host;
    private final int puerto;
    private final Runnable onFallo;
    private volatile boolean ejecutando = true;

    public HiloMonitorConexion(String host, int puerto, Runnable onFallo) {
        this.host = host;
        this.puerto = puerto;
        this.onFallo = onFallo;
    }
    
      
    @Override
    public void run() {
        System.out.println("Iniciando monitoreo del controlador principal (" + host + ":" + puerto + ")");
        while (ejecutando) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, puerto), 1000); // timeout 1s
                socket.close();
                Thread.sleep(3000);
            } catch (IOException e) {
                System.out.println("El controlador principal no responde. Activando controlador de backup...");
                onFallo.run(); // dispara la acción de recuperación
                break;
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void detener() {
        ejecutando = false;
        this.interrupt();
    }
    
    
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ControladorCentral;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

/**
 *
 * @author Evelin
 */
public class BackupController {
     private static final String HOST_MAESTRO = "localhost";
    private static final int puertoPrincipal = 20000;
    private static final int puertoBackup = 20001;
    private static volatile boolean soyMaestro = false;

    public static void iniciarBackup() throws IOException {
        System.out.println("Backup del Controlador Central en espera...");

        while (true) {
            try {
                // Intentamos tomar el puerto del maestro
                ServerSocket takeover = new ServerSocket(puertoPrincipal);
                System.out.println("Maestro caído (puerto libre). Asumiendo control...");
                // Ejecutamos el Controlador Central principal usando este socket
                MainController.iniciarPrincipal(takeover);
                // Si retorna (por error grave), dormimos un poco y volvemos a vigilar
                Thread.sleep(2000);
            } catch (BindException e) {
                // Puerto en uso → maestro activo
                System.out.print(".");
                try {
                    Thread.sleep(3000); // esperamos 3 segundos antes de volver a probar
                } catch (InterruptedException ignored) {}
            } catch (IOException e) {
                System.err.println("Error de IO: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}



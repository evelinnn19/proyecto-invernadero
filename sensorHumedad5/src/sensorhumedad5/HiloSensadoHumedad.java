package sensorhumedad5;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoHumedad extends Thread {

    private Boolean prendido;
    private Double humedad;
    private Socket clienteHumedadEnviar;
    private PrintWriter haciaServer;

    public HiloSensadoHumedad(Socket che, PrintWriter dos) {
        prendido = Boolean.TRUE;
        this.clienteHumedadEnviar = che;
        this.haciaServer = dos;
    }

    public void generarHumedadAleatoria() {
        humedad = Math.random() * 100;
    }

    public void apagar() {
        prendido = Boolean.FALSE;
    }

    public void encender() {
        prendido = Boolean.TRUE;
    }

    public void run() {
        //usar prendido como condicion podría ayudar a controlar cuando queremos sensar la humedad.
        try {
            while (prendido) {
                generarHumedadAleatoria();
                haciaServer.flush();
                haciaServer.println(humedad);
                System.out.println("La humedad desde el dispositivo es: " + humedad);
                                
                Thread.sleep(5000);
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(HiloSensadoHumedad.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
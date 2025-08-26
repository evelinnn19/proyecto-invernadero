package sensorhumedad1;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoHumedad extends Thread {

    private Boolean prendido;
    private Double humedad;
    private Socket clienteHumedadEnviar;
    private DataOutputStream haciaServer;

    public HiloSensadoHumedad(Socket che, DataOutputStream dos) {
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
                haciaServer.writeDouble(humedad);
                System.out.println("La humedad es: " + humedad);
                haciaServer.flush();
                
                Thread.sleep(5000);
            }

        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HiloSensadoHumedad.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

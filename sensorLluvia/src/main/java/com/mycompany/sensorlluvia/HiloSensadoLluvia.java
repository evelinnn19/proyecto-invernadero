package com.mycompany.sensorlluvia;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoLluvia extends Thread {

    private Boolean prendido;
    private boolean llueve;
    private Socket clienteLluviaEnviar;
    private PrintWriter haciaServer;

    public HiloSensadoLluvia(Socket clienteLluviaEnviar, PrintWriter haciaServer) {
        prendido = Boolean.TRUE;
        this.clienteLluviaEnviar = clienteLluviaEnviar;
        this.haciaServer = haciaServer;
    }

    public void generarLluviaAleatoria() {
       Random random = new Random();
        double probabilidadLluvia = 0.2; // 20% de chance
        llueve = random.nextDouble() < probabilidadLluvia;
    }

    public void apagar() {
        prendido = Boolean.FALSE;
    }

    public void encender() {
        prendido = Boolean.TRUE;
    }

    @Override
    public void run() {
        prendido = Boolean.TRUE;
        //usar prendido como condicion podría ayudar a controlar cuando queremos sensar la humedad.
        try {
            while (prendido) {
                generarLluviaAleatoria();
                haciaServer.println(llueve);
                System.out.println("Llueve?: " + llueve);
                haciaServer.flush();
                
                Thread.sleep(25000);
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(HiloSensadoLluvia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

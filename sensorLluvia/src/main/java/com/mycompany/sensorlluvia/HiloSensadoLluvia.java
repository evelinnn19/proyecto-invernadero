package com.mycompany.sensorlluvia;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloSensadoLluvia extends Thread {

    private Boolean prendido;
    private boolean llueve;
    private Socket clienteLluviaEnviar;
    private DataOutputStream haciaServer;

    public HiloSensadoLluvia(Socket clienteLluviaEnviar, DataOutputStream haciaServer) {
        prendido = Boolean.TRUE;
        this.clienteLluviaEnviar = clienteLluviaEnviar;
        this.haciaServer = haciaServer;
    }

    public void generarLluviaAleatoria() {
        Random random = new Random();
        llueve = random.nextBoolean();

    }

    public void apagar() {
        prendido = Boolean.FALSE;
    }

    public void encender() {
        prendido = Boolean.TRUE;
    }

    public void run() {
        prendido = Boolean.TRUE;
        //usar prendido como condicion podría ayudar a controlar cuando queremos sensar la humedad.
        try {
            while (prendido) {
                generarLluviaAleatoria();
                haciaServer.writeBoolean(llueve);
                System.out.println("Llueve?: " + llueve);
                haciaServer.flush();
                
                Thread.sleep(5000);
            }

        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HiloSensadoLluvia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

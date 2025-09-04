/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package electrovalvula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alejandro
 */
public class HiloSensadoElectrovalvula extends Thread {

    private Boolean prendido;
    private Double humedad;
    private Socket clienteHumedadEnviar;
    private PrintWriter haciaServer;
    private BufferedReader br;

    public HiloSensadoElectrovalvula(Socket che, PrintWriter dos) {
        prendido = Boolean.TRUE;
        this.clienteHumedadEnviar = che;
        this.haciaServer = dos;
        try {
            this.br = new BufferedReader(new InputStreamReader(che.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void apagar() {
        prendido = Boolean.FALSE;
    }

    public void encender() {
        prendido = Boolean.TRUE;
    }

    @Override
    public void run() {
        //usar prendido como condicion podría ayudar a controlar cuando queremos sensar;
        try {
            while (prendido) {

                try {
                    int TiempoEspera = Integer.parseInt(br.readLine());
                    System.out.println("La eletrovalvula estara abierta por " + TiempoEspera);

                    Thread.sleep(TiempoEspera);

                    System.out.println("Electroválvula cerrada después de " + TiempoEspera + " segundos");

                } catch (IOException ex) {
                    Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        } catch (InterruptedException ex) {
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

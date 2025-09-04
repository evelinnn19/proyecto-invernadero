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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alejandro
 */
public class HiloSensadoElectrovalvula extends Thread{
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
    public void run(){
        //usar prendido como condicion podría ayudar a controlar cuando queremos sensar;
        try {
            while (prendido) {
                
        String orden = br.readLine();
            if (orden == null) break;

            if (orden.equals("ON")) {
                System.out.println("Electrovalvula2 abierta (riego general)");
                // acá simulás apertura
            } else if (orden.equals("OFF")) {
                System.out.println("Electrovalvula2 cerrada (riego general)");
                // acá simulás cierre
            }
                
           
                                
                
            }

        } catch (IOException ex) {
            Logger.getLogger(HiloSensadoElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorLluvia extends Thread{
    
    Socket clienteLluvia;
    BufferedReader br;
    PrintWriter out;
    boolean llueve;
    
    public HiloReceptorLluvia(Socket ch){
        clienteLluvia = ch;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteLluvia.getInputStream()));
            this.out = new PrintWriter(clienteLluvia.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean getLlueve() {
        return llueve;
    }

    public void setLlueve(boolean llueve) {
        this.llueve = llueve;
    }
    
    @Override
    public void run(){
        while (true) {            
            try {
                String Entrada = br.readLine();
                setLlueve(Boolean.parseBoolean(Entrada));
                System.out.println("El servidor recibio la siguiente informacion de lluvia: " + getLlueve());
                
            } catch (IOException ex) {
                Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }   
    }
}
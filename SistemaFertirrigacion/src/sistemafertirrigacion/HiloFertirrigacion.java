/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemafertirrigacion;

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
public class HiloFertirrigacion extends Thread{
    private PrintWriter out;
    private BufferedReader in;
    private Socket cliente;
    
    public HiloFertirrigacion(Socket cliente,PrintWriter out){
        
        this.cliente = cliente;
        try {
            this.in =  new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = out;
        } catch (IOException ex) {
            Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
    
        
        
        
        
        
    }
    
    @Override
    public void run(){
        
        while(true){
            try {
                String orden = in.readLine();
                if (orden == null) break;

                int tiempo = Integer.parseInt(orden);
                System.out.println("Fertirrigación iniciada por " + tiempo/1000 + " segundos");

                Thread.sleep(tiempo);

                System.out.println("Fertirrigación finalizada");
                out.println("FERTI_OK");  // avisar al controlador
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(HiloFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
        }
        
    
    
}

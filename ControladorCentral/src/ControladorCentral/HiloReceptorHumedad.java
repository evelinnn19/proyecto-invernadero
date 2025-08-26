/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alumnos
 */
public class HiloReceptorHumedad extends Thread{
    Socket ClienteHumedad;
    BufferedReader br;
    double humedad;
    
    public HiloReceptorHumedad(Socket ch){
        ClienteHumedad = ch;
        try {
             BufferedReader in = new BufferedReader(new InputStreamReader(ClienteHumedad.getInputStream()));
             PrintWriter out = new PrintWriter(ClienteHumedad.getOutputStream(), true);
        } catch (Exception e) {
        }
    }
    
    public void recibirHumedad(){
        
    }
    
    @Override
    public void run(){
        while (true) {            
            try {
                String Entrada = br.readLine();
                setHumedad(Double.parseDouble(Entrada));
                
                
            } catch (IOException ex) {
                Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }

    public double getHumedad() {
        return humedad;
    }

    public void setHumedad(double humedad) {
        this.humedad = humedad;
    }
    
}

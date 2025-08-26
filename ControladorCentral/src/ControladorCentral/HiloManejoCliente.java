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
public class HiloManejoCliente extends Thread{
    
    
    Socket Cliente;
    BufferedReader in;
    PrintWriter out;
    
    
        public HiloManejoCliente(Socket ch){
        Cliente = ch;
        try {
             in = new BufferedReader(new InputStreamReader(Cliente.getInputStream()));
             out = new PrintWriter(Cliente.getOutputStream(), true);
        } catch (Exception e) {
        }
    }
        
        @Override
        public void run(){
            
        try {
            String tipoDispositivo = "";
            tipoDispositivo = in.readLine();
            switch (tipoDispositivo) {
                case "sensorHumedad":
                    HiloReceptorHumedad hrh = new HiloReceptorHumedad(Cliente);
                    hrh.start();
                    break;
                    
                default:
                    throw new AssertionError();
            }
        } catch (IOException ex) {
            Logger.getLogger(HiloManejoCliente.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        }
    
}

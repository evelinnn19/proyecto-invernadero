/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alumnos
 */
public class Main {
    public static void main(String[] args) {
        String tipoDispositivo = "";
        
        try {
            ServerSocket server = new ServerSocket(2000);
            //cuando este prenddo el servidor
            while (true){
            Socket s = server.accept(); //crea un socket cuando recibe la conexion
            //Arrancamos la vida del receptorHumedad
            
            HiloManejoCliente cliente = new HiloManejoCliente(s);
            
             
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}


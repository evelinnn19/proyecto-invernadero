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
 * @author Alejandro
 */
public class HiloReceptorFertirrigacion extends Thread{
    
    
    
  
    private Socket cliente;
    private BufferedReader br;
    private PrintWriter out;
    private CoordinadorBomba bomba;
    
    

    public HiloReceptorFertirrigacion(Socket ch,CoordinadorBomba bomba) {
        this.cliente = ch;
        this.bomba = bomba;
        try {
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = new PrintWriter(cliente.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run(){
            while (true) {
            try {
                System.out.println("Solicitando Fertirrigado");
                this.bomba.iniciarFertirrigacion();
                out.println(10000);
                String espera = br.readLine();
                if("FERTI_OK".equals(espera)){
                    this.bomba.terminaFertirrigacion();
                    System.out.println("Pausa fertirrigacion por: 20 segundos");
                    Thread.sleep(20000);
                }
                
            }   catch (InterruptedException | IOException ex) {
                    Logger.getLogger(HiloReceptorFertirrigacion.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
    }
    
}

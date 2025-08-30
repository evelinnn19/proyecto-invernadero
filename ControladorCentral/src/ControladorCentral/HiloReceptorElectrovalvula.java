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
public class HiloReceptorElectrovalvula extends Thread{
    Socket cliente;
    BufferedReader br;
    PrintWriter out;
    double humedad;
    int parcela;
    DatosINR datos;

    public HiloReceptorElectrovalvula(Socket ch,DatosINR datos,int parcela) {
        this.datos = datos;
        cliente = ch;
       this.parcela = parcela;
        try {
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = new PrintWriter(cliente.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int tiempoRiegoParcela(int parcela,double v1,double v2,double v3){
        double INR = this.datos.calcularINR(parcela, v1, v2, v3);
        
        if(!this.datos.isSensorLluvia()){
            if(INR > 0.7 && INR <0.8){
            System.out.println("Se regara por 5 minutos");
            return 5000;
        }
        if(INR > 0.8 && INR < 0.9){
            System.out.println("Se regará por 7 minutos");
            return 7000;
        }else{
            if(INR > 0.9){
                System.out.println("se regara por 10 minutos");
                return 10000;
            }
        }
            
            
        }
        
        return 0;
        
        
        
        
        
    }
    
    
    @Override
    public void run(){
            while (true) {
                int tiempo = tiempoRiegoParcela(this.parcela,0.5,0.1,0.5);
                out.println(tiempo);
                out.flush();
                System.out.println("➡️ Enviado tiempo de riego: " + tiempo + " segundos a parcela " + this.parcela);
                try {
                    Thread.sleep(tiempo);
                } catch (InterruptedException ex) {
                    Logger.getLogger(HiloReceptorElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
                }
                
        }
        
    }
}

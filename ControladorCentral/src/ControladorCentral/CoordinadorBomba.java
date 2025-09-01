/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ControladorCentral;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author Alejandro
 */
public class CoordinadorBomba {
    private int valvulasRiegoActivas = 0;
    private boolean fertirrigando = false;
    private boolean riegoGeneral = false;
    private PrintWriter outGeneral; // para enviar a EV2
    
    
    //Inicia un riego (si se esta fertirrigando, lo bloquea)
    public synchronized void iniciarRiego(int parcela) throws InterruptedException{
        while(fertirrigando){
            wait();
        }
        valvulasRiegoActivas++;
        System.out.println("Se inicio el riego en parcela " + parcela);
        System.out.println(valvulasRiegoActivas + "Valvulas de riego activas");
        if(valvulasRiegoActivas == 1){
            iniciarRiegoGeneral();
        }
        
    }
    
    
    //Comunicación valvula general
    public synchronized void setElectrovalvulaGeneral(Socket cliente) {
    try {
        this.outGeneral = new PrintWriter(cliente.getOutputStream(), true);
    } catch (IOException e) {
    }
}
    
    //Termina el riego
    public synchronized void terminarRiego(int parcela){
        valvulasRiegoActivas--;
        System.out.println("La parcela " + parcela + "termino el riego");
        System.out.println(valvulasRiegoActivas + "Valvulas de riego activas");
        
        if(valvulasRiegoActivas == 0){
            terminarRiegoGeneral();
            notifyAll();
        }
        
        
    }
    
    
    
    //Riego general
    private void iniciarRiegoGeneral() {
        riegoGeneral = true;
        System.out.println("💧 Electrovalvula2 abierta (riego general)");
        if (outGeneral != null) outGeneral.println("ON");
    }

    private void terminarRiegoGeneral() {
        riegoGeneral = false;
        System.out.println("💧 Electrovalvula2 cerrada (riego general)");
         if (outGeneral != null) outGeneral.println("OFF");
    }
    
    
    //Inicia fertirrigado
    public synchronized void iniciarFertirrigacion() throws InterruptedException{
        while (valvulasRiegoActivas > 0 || riegoGeneral) {
            wait(); // espera hasta que nadie esté regando
        }
        fertirrigando = true;
        System.out.println("🚰 Bomba asignada a FERTIRRIGACIÓN");
    }
    
    //termina fertirrigación
    public synchronized void terminaFertirrigacion(){
        fertirrigando = false;
        System.out.println("Fertirrigacion terminada");
        notifyAll();
    }
    
    

}


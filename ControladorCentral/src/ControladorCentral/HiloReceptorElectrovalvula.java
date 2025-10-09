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
public class HiloReceptorElectrovalvula extends Thread {

    private Socket cliente;
    private BufferedReader br;
    private PrintWriter out;
    private double humedad;
    private int parcela;
    private DatosINR datos;
    private CoordinadorBomba bomba;
    private boolean esFerti;

    public HiloReceptorElectrovalvula(Socket ch, DatosINR datos, int parcela, CoordinadorBomba bomba, boolean fer) {
        this.datos = datos;
        cliente = ch;
        this.bomba = bomba;
        this.parcela = parcela;
        this.esFerti = fer;
        try {
            this.br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            this.out = new PrintWriter(cliente.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int tiempoRiegoParcela(int parcela, double v1, double v2, double v3) {
        double INR = this.datos.calcularINR(parcela, v1, v2, v3);

        if (!this.datos.isSensorLluvia()) {
            if (INR > 0.7 && INR < 0.8) {
                System.out.println("Se regara por 5 minutos");
                return 5000;
            }
            if (INR >= 0.8 && INR < 0.9) {
                System.out.println("Se regará por 7 minutos");
                return 7000;
            } else {
                if (INR > 0.9) {
                    System.out.println("se regara por 10 minutos");
                    return 10000;
                }
            }
        }
        return 0;
    }

    
    /*
    El metodo run de este receptor de electrovalvula calcula el tiempo de riego
    de parcela en base al Indice de Necesidad de Riego. Se pregunta si llueve,
    en cuyo caso se indica que está lloviendo y que no se regara. Caso contrario,
    se calcula el tiempo en base al INR, y si es mayor que 0 (en ms), se envía
    un mensaje indicando el tiempo para abrir.
    */
    @Override
    public void run() {
        int tiempo;
        while (true) {
            try {
                if (this.datos.isSensorLluvia()) {
                    System.out.println("Lluvia detectada. Riego inhibido.");
                    Thread.sleep(25000);
                } else {
                    tiempo = tiempoRiegoParcela(this.parcela, 0.5, 0.1, 0.5);
                    if (tiempo > 0) {
                        this.bomba.iniciarRiego(parcela);
                        out.println("ON");
                        out.flush();
                        System.out.println("➡️ Enviado tiempo de riego: " + tiempo + " segundos a parcela " + this.parcela);
                        Thread.sleep(tiempo);
                        this.bomba.terminarRiego(parcela);
                        out.println("OFF");
                        out.flush();
                        //Espera un tiempo hasta volver a regar
                        Thread.sleep(10000);
                    }
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(HiloReceptorElectrovalvula.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }
}

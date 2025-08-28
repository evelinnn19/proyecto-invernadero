package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorTemperatura extends Thread {

    Socket clienteTemperatura;
    BufferedReader br;
    PrintWriter out;
    double temperatura;
    DatosINR datos;

    public HiloReceptorTemperatura(Socket ch,DatosINR datos) {
        this.datos = datos;
        clienteTemperatura = ch;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteTemperatura.getInputStream()));
            this.out = new PrintWriter(clienteTemperatura.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String Entrada = br.readLine();
                setTemperatura(Double.parseDouble(Entrada));
                System.out.println("El servidor recibio la temperatura: " + getTemperatura() + "Celsius.");
                this.datos.setSensorTemp(temperatura);

            } catch (IOException ex) {
                Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
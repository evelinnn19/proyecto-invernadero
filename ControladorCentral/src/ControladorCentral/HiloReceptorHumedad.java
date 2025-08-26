package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorHumedad extends Thread {

    Socket clienteHumedad;
    BufferedReader br;
    PrintWriter out;
    double humedad;

    public HiloReceptorHumedad(Socket ch) {
        clienteHumedad = ch;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteHumedad.getInputStream()));
            this.out = new PrintWriter(clienteHumedad.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getHumedad() {
        return humedad;
    }

    public void setHumedad(double humedad) {
        this.humedad = humedad;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String Entrada = br.readLine();
                setHumedad(Double.parseDouble(Entrada));
                System.out.println("El servidor recibió la humedad: " + getHumedad());

            } catch (IOException ex) {
                Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
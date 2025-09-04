package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorRadiacion extends Thread {

    private Socket clienteRadiacion;
    private BufferedReader br;
    private PrintWriter out;
    private double radiacion;
    private DatosINR datos;

    public HiloReceptorRadiacion(Socket ch,DatosINR datos) {
        this.datos = datos;
        clienteRadiacion = ch;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteRadiacion.getInputStream()));
            this.out = new PrintWriter(clienteRadiacion.getOutputStream(), true);
        } catch (IOException e) {
            Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public double getRadiacion() {
        return radiacion;
    }

    public void setRadiacion(double radiacion) {
        this.radiacion = radiacion;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String Entrada = br.readLine();
                setRadiacion(Double.parseDouble(Entrada));
                //System.out.println("El servidor recibio la radiacion: " + getRadiacion());
                this.datos.setSensorRad(radiacion);

            } catch (IOException ex) {
                Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
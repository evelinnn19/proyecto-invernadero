package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HiloReceptorHumedad extends Thread {

    private int parcela;
    private double humedad;
    private Socket clienteHumedad;
    private BufferedReader br;
    private PrintWriter out;
    private DatosINR datos;
    

    public HiloReceptorHumedad(Socket ch, DatosINR datos, int parcela) {
        this.datos = datos;
        this.clienteHumedad = ch;
        this.parcela = parcela;
        try {
            this.br = new BufferedReader(new InputStreamReader(clienteHumedad.getInputStream()));
            this.out = new PrintWriter(clienteHumedad.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getParcela() {
        return parcela;
    }

    public double getHumedad() {
        return humedad;
    }

    public void setHumedad(double humedad) {
        this.humedad = humedad;
    }

    public void setDatosSensorHumedad(int parcela) {
        switch (parcela) {
            case 1:
                this.datos.setSensorH1(humedad);
                break;
            case 2:
                this.datos.setSensorH2(humedad);
                break;
            case 3:
                this.datos.setSensorH3(humedad);
                break;
            case 4:
                this.datos.setSensorH4(humedad);
                break;
            case 5:
                this.datos.setSensorH5(humedad);
                break;
        }

    }

    @Override
    public void run() {
        while (true) {
            try {
                String Entrada = br.readLine();
                setHumedad(Double.parseDouble(Entrada));
                System.out.println("El servidor recibió la humedad: " + getHumedad() + " del sensor Numero " + parcela);
                setDatosSensorHumedad(parcela);
            } catch (IOException ex) {
                Logger.getLogger(HiloReceptorHumedad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

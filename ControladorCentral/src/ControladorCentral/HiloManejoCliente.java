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
 *
 * La clase que manejará la aceptación de clientes en un hilo.
 *
 *
 */
public class HiloManejoCliente extends Thread {

    Socket cliente;
    BufferedReader in;
    PrintWriter out;
    DatosINR datos;

    public HiloManejoCliente(Socket ch,DatosINR datos) {
        cliente = ch;
        this.datos = datos;
        try {
            in = new BufferedReader(new InputStreamReader(ch.getInputStream()));
            out = new PrintWriter(cliente.getOutputStream(), true);
        } catch (Exception e) {
        }
    }

    @Override
    public void run() {
        try {
            //System.out.println("Prueba anterior al readLine");
            String tipoDispositivo = (in.readLine()).trim();
            //System.out.println("Prueba posterior al readLine");
            //System.out.println(tipoDispositivo);

            switch (tipoDispositivo) {
                case "sensorHumedad":
                    System.out.println("Se detectó una conexion para el sensor de humedad.");
                    HiloReceptorHumedad hrh = new HiloReceptorHumedad(cliente);
                    hrh.start();
                    break;
                case "sensorTemperatura":
                    System.out.println("Se detectó una conexion para el sensor de temperatura.");
                    HiloReceptorTemperatura hrt = new HiloReceptorTemperatura(cliente);
                    hrt.start();
                    break;
                case "sensorRadiacion":
                    System.out.println("Se detectó una conexion para el sensor de temperatura.");
                    HiloReceptorRadiacion hrr = new HiloReceptorRadiacion(cliente);
                    hrr.start();
                    break;
                case "sensorLluvia":
                    System.out.println("Se detectó una conexion para el sensor de lluvia.");
                    HiloReceptorLluvia hrll = new HiloReceptorLluvia(cliente);
                    hrll.start();
                    break;
                default:
                    System.out.println("SE DETECTO UNA CONEXION NO ORIGINARIA DE LOS SENSORES.");
                    break;
            }

        } catch (IOException ex) {
            Logger.getLogger(HiloManejoCliente.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}

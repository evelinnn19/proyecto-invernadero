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
 * Simplemente se encarga de determinar la conexión para un sensor.
 *
 */
public class HiloManejoCliente extends Thread {

    private Socket cliente;
    private BufferedReader in;
    private PrintWriter out;
    private DatosINR datos;

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
                case "sensorHumedad1":
                    System.out.println("Se detectó una conexion para el sensor de humedad numero 1.");
                    HiloReceptorHumedad hrh1 = new HiloReceptorHumedad(cliente,datos,1);
                    hrh1.start();
                    break;
                case "sensorHumedad2":
                    System.out.println("Se detectó una conexion para el sensor de humedad numero 2.");
                    HiloReceptorHumedad hrh2 = new HiloReceptorHumedad(cliente,datos,2);
                    hrh2.start();
                    break;
                case "sensorHumedad3":
                    System.out.println("Se detectó una conexion para el sensor de humedad numero 3.");
                    HiloReceptorHumedad hrh3 = new HiloReceptorHumedad(cliente,datos,3);
                    hrh3.start();
                    break;
                case "sensorHumedad4":
                    System.out.println("Se detectó una conexion para el sensor de humedad numero 4.");
                    HiloReceptorHumedad hrh4 = new HiloReceptorHumedad(cliente,datos,4);
                    hrh4.start();
                    break;
                case "sensorHumedad5":
                    System.out.println("Se detectó una conexion para el sensor de humedad numero 5.");
                    HiloReceptorHumedad hrh5 = new HiloReceptorHumedad(cliente,datos,5);
                    hrh5.start();
                    break;
                    
                case "sensorTemperatura":
                    System.out.println("Se detectó una conexion para el sensor de temperatura.");
                    HiloReceptorTemperatura hrt = new HiloReceptorTemperatura(cliente,datos);
                    hrt.start();
                    break;
                case "sensorRadiacion":
                    System.out.println("Se detectó una conexion para el sensor de radiacion.");
                    HiloReceptorRadiacion hrr = new HiloReceptorRadiacion(cliente,datos);
                    hrr.start();
                    break;
                case "sensorLluvia":
                    System.out.println("Se detectó una conexion para el sensor de lluvia.");
                    HiloReceptorLluvia hrll = new HiloReceptorLluvia(cliente,datos);
                    hrll.start();
                    break;
                case "electroValvula":
                    System.out.println("Se detectó una conexion para una electro valvula");
                    HiloReceptorElectrovalvula hre1 = new HiloReceptorElectrovalvula(cliente,datos,1);
                    hre1.start();
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

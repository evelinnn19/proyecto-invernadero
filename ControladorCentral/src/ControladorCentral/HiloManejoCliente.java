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
    private CoordinadorBomba bomba;
    private Impresor impresora;

    public HiloManejoCliente(Socket ch,DatosINR datos,CoordinadorBomba bomba, Impresor imp) {
        cliente = ch;
        impresora = imp;
        this.datos = datos;
        this.bomba = bomba;
        try {
            in = new BufferedReader(new InputStreamReader(ch.getInputStream()));
            out = new PrintWriter(cliente.getOutputStream(), true);
        } catch (Exception e) {
        }
    }

    @Override
    public void run() {
        try {

            String tipoDispositivo = in.readLine().trim();
            if (tipoDispositivo == null) {
                System.out.println("Cliente se desconectó antes de identificarse");
                cerrarConexion();
                return;
            }

            switch (tipoDispositivo) {
                case "sensorHumedad1":
                    System.out.println("Se detectó una conexion para el sensor de humedad numero 1.");
                    HiloReceptorHumedad hrh1 = new HiloReceptorHumedad(cliente,datos,1);
                    hrh1.start();
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
                case "sistemaFertirrigacion":
                    System.out.println("Se detectó una conexion con el sistema de fertirrigacion.");
                    HiloReceptorFertirrigacion hrf = new HiloReceptorFertirrigacion(cliente,bomba);
                    hrf.start();
                    break;
                    //Este es para el sistema de fertirrigación
                case "electroValvula1":
                    System.out.println("Se detectó una conexion para una electro valvula");
                    bomba.setElectroValvulaFerti(cliente);
                    MonitorElectrovalvulaSimple ferti = new MonitorElectrovalvulaSimple(cliente, tipoDispositivo, bomba, true);
                    ferti.start();
                    break;
                    //Este es la Valvula de riego
                case "electroValvula2":
                    System.out.println("Se detectó una conexion para una electro valvula");
                    bomba.setElectrovalvulaGeneral(cliente);
                    MonitorElectrovalvulaSimple general = new MonitorElectrovalvulaSimple(cliente, tipoDispositivo, bomba, false);
                    general.start();
                    break;
                    //Apartir de aqui es para las parcelas 1 a 5
                case "electroValvula3":
                    System.out.println("Se detectó una conexion para una electro valvula");
                    HiloReceptorElectrovalvula hre3 = new HiloReceptorElectrovalvula(cliente,datos,1,bomba,false);
                    hre3.setImp(impresora);
                    hre3.start();
                    break;
                default:
                    System.out.println("SE DETECTO UNA CONEXION NO ORIGINARIA DE LOS SENSORES.");
                    break;
            }

        } catch (IOException ex) {
            Logger.getLogger(HiloManejoCliente.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    
    private void cerrarConexion() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

}

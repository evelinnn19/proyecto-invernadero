package com.sistdist.servidorexclusionmutua;

import com.sistdist.interfaces.IDetectorFalla;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Detector de fallos mejorado que puede actuar como maestro o respaldo.
 */
public class DetectorFallo extends UnicastRemoteObject implements IDetectorFalla {

    boolean llegoMensaje;
    String estado;
    String nombre;
    boolean esMaestro;
    private int contadorFallos = 0;
    private static final int MAX_FALLOS_CONSECUTIVOS = 3;
    
    public DetectorFallo() throws RemoteException {
        super();
        llegoMensaje = false;
        estado = "no sospechoso";
        esMaestro = true;
    }
    
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEstado() {
        return estado;
    }
    
    public boolean isEsMaestro() {
        return esMaestro;
    }

    public void setEsMaestro(boolean esMaestro) {
        this.esMaestro = esMaestro;
    }

    public int getContadorFallos() {
        return contadorFallos;
    }
    
    @Override
    public void DameMensaje(IDetectorFalla cliente, String mensaje) throws RemoteException {
        
        if (mensaje.equals("vivo?")) {
            cliente.DameMensaje(this, "si");
            System.out.println("Respondiendo heartbeat: SI");
        }
        
        if (mensaje.equals("si")) {
            llegoMensaje = true;
            contadorFallos = 0; // Resetear contador
            System.out.println("Heartbeat recibido del servidor maestro");
        }
    }
    
    public void chequearRespuesta() {
        if (llegoMensaje) {
            estado = "no sospechoso";
            contadorFallos = 0;
        } else {
            contadorFallos++;
            
            if (contadorFallos >= MAX_FALLOS_CONSECUTIVOS) {
                estado = "CAÍDO";
                System.out.println("SERVIDOR MAESTRO CAÍDO - " + contadorFallos + " fallos consecutivos");
            } else {
                estado = "sospechoso";
                System.out.println("Servidor sospechoso - Fallo " + contadorFallos + "/" + MAX_FALLOS_CONSECUTIVOS);
            }
        }
        
        // Reset para próxima verificación
        llegoMensaje = false;
    }
    
    public boolean maestroCaido() {
        return "CAÍDO".equals(estado);
    }
}
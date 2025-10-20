package com.sistdist.servidorexclusionmutua;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Servidor de exclusión mutua mejorado con identificación.
 */
public class ServerExclusionMutuaRMI extends UnicastRemoteObject implements IServicioExclusionMutua {

    boolean token;
    Queue<IClienteEM> clientes;
    String nombreServidor;
    
    public ServerExclusionMutuaRMI() throws RemoteException {
        super();
        token = true;
        clientes = new ArrayDeque<>();
        nombreServidor = "Desconocido";
    }
    
    public void setNombreServidor(String nombre) {
        this.nombreServidor = nombre;
    }
    
    @Override
    public synchronized void ObtenerRecurso(IClienteEM cliente) throws RemoteException {
        System.out.println("📨 [" + nombreServidor + "] Solicitud de recurso recibida");
        
        if (token) {
            token = false;
            cliente.RecibirToken();
            System.out.println("🔑 [" + nombreServidor + "] Token entregado inmediatamente");
        } else {
            clientes.add(cliente);
            System.out.println("⏳ [" + nombreServidor + "] Cliente encolado. Cola: " + clientes.size());
        }
    }

    @Override
    public synchronized void DevolverRecurso() throws RemoteException {
        System.out.println("🔄 [" + nombreServidor + "] Recurso devuelto");
        
        if (!clientes.isEmpty()) {
            IClienteEM cliente = clientes.poll();
            cliente.RecibirToken();
            System.out.println("🔑 [" + nombreServidor + "] Token entregado al siguiente cliente. Cola: " + clientes.size());
        } else {
            token = true;
            System.out.println("✅ [" + nombreServidor + "] Token libre (sin clientes esperando)");
        }
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistdist.servidorexclusionmutua;

import com.sistdist.interfaces.IClienteEM;
import com.sistdist.interfaces.IServicioExclusionMutua;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 *
 * @author lesca
 */
public class ServerExclusionMutuaRMI extends UnicastRemoteObject implements IServicioExclusionMutua {

    boolean token;
    Queue<IClienteEM> clientes;
    
    public ServerExclusionMutuaRMI() throws RemoteException{
        super();
        token = true;
        clientes = new ArrayDeque<>();
    }
    
    @Override
    public void ObtenerRecurso(IClienteEM cliente) throws RemoteException {
        if (token){
            token = false;
            cliente.RecibirToken();            
        } else {
            clientes.add(cliente);
        }
    }

    @Override
    public void DevolverRecurso() throws RemoteException {
        if (!clientes.isEmpty()){
            IClienteEM cliente = clientes.poll();
            cliente.RecibirToken();
        } else {
            token = true;
        }
    }
    
}

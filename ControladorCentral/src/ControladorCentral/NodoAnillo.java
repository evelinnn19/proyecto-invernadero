/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ControladorCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Evelin
 */
public class NodoAnillo {
    private final int id;
    private final int puertoAnillo;
    private final int puertoSiguiente;
    private int lider;
    private AtomicBoolean esLider;
    private ControladorCentral controlador;
    private ServerSocket serverAnillo;
    private Thread hiloEscucha;
    private Thread hiloControlador;
    private volatile long ultimoLatido;
    
    public NodoAnillo(int id, int puertoAnillo, int puertoSiguiente) {
        this.id = id;
        this.puertoAnillo = puertoAnillo;
        this.puertoSiguiente = puertoSiguiente;
        this.lider = -1;
        this.esLider = new AtomicBoolean(false);
        this.ultimoLatido = System.currentTimeMillis();
    }
    
    public void iniciar() {
        try {
            serverAnillo = new ServerSocket(puertoAnillo);
            System.out.println("═══════════════════════════════════════════");
            System.out.println("  Nodo " + id + " iniciado (Puerto Anillo: " + puertoAnillo + ")");
            System.out.println("═══════════════════════════════════════════");
            
            // Hilo que escucha mensajes del anillo
            hiloEscucha = new Thread(this::escucharAnillo);
            hiloEscucha.setDaemon(true);
            hiloEscucha.start();
            
            // Esperar a que otros nodos se inicien
            Thread.sleep(2000);
            
            // Iniciar elección
            iniciarEleccion();
            
            // Hilo que monitorea al líder
            Thread monitoreo = new Thread(this::monitorearLider);
            monitoreo.setDaemon(true);
            monitoreo.start();
            
            // Mantener el programa corriendo
            Thread.currentThread().join();
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error al iniciar nodo: " + e.getMessage());
        }
    }
    
    private void escucharAnillo() {
        while (true) {
            try {
                Socket socket = serverAnillo.accept();
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                
                String mensaje = in.readLine();
                if (mensaje != null) {
                    procesarMensaje(mensaje);
                }
                
                in.close();
                socket.close();
            } catch (IOException e) {
                if (!serverAnillo.isClosed()) {
                    System.err.println("Error en escucha del anillo: " + e.getMessage());
                }
            }
        }
    }
    
    private void procesarMensaje(String mensaje) {
        String[] partes = mensaje.split(":");
        String tipo = partes[0];
        
        switch (tipo) {
            case "ELECCION":
                int idCandidato = Integer.parseInt(partes[1]);
                procesarEleccion(idCandidato);
                break;
                
            case "COORDINADOR":
                int nuevoLider = Integer.parseInt(partes[1]);
                procesarCoordinador(nuevoLider);
                break;
                
            case "LATIDO":
                ultimoLatido = System.currentTimeMillis();
                // Reenviar latido
                if (lider != id) {
                    enviarMensaje("LATIDO:" + partes[1]);
                }
                break;
        }
    }
    
    private void iniciarEleccion() {
        System.out.println("[ELECCION] Nodo " + id + " inicia elección");
        if (!enviarMensajeConTimeout("ELECCION:" + id, 2000)) {
            // Si no puedo enviar, significa que soy el único vivo
            System.out.println("[ELECCION] No hay respuesta, asumo liderazgo");
            System.out.println("\n═══════════════════════════════════════════");
            System.out.println("  ★★★ NODO " + id + " ELEGIDO COMO LÍDER ★★★");
            System.out.println("═══════════════════════════════════════════\n");
            lider = id;
            esLider.set(true);
            activarControlador();
        }
    }
    
    private void procesarEleccion(int idCandidato) {
        if (idCandidato > id) {
            System.out.println("[ELECCION] Reenvía candidato: " + idCandidato);
            if (!enviarMensajeConTimeout("ELECCION:" + idCandidato, 2000)) {
                // El siguiente nodo no responde, envío mi propio ID
                System.out.println("[ELECCION] Nodo siguiente no responde, usando ID propio");
                enviarMensajeConTimeout("ELECCION:" + id, 2000);
            }
        } else if (idCandidato < id) {
            System.out.println("[ELECCION] Reemplaza con ID propio: " + id);
            enviarMensajeConTimeout("ELECCION:" + id, 2000);
        } else {
            // El mensaje volvió a mí, soy el líder
            System.out.println("\n═══════════════════════════════════════════");
            System.out.println("  ★★★ NODO " + id + " ELEGIDO COMO LÍDER ★★★");
            System.out.println("═══════════════════════════════════════════\n");
            lider = id;
            esLider.set(true);
            enviarMensaje("COORDINADOR:" + id);
            activarControlador();
        }
    }
    
    private void procesarCoordinador(int nuevoLider) {
        if (nuevoLider != id) {
            if (lider != nuevoLider) {
                System.out.println("[ANILLO] Líder reconocido: Nodo " + nuevoLider);
                lider = nuevoLider;
                esLider.set(false);
                ultimoLatido = System.currentTimeMillis();
                
                // Si tenía el controlador activo, lo desactivo
                if (controlador != null) {
                    System.out.println("[BACKUP] Pasando a modo backup...");
                    desactivarControlador();
                }
            }
            
            // Reenviar mensaje de coordinador
            enviarMensaje("COORDINADOR:" + nuevoLider);
        }
    }
    
    private void activarControlador() {
        if (controlador == null) {
            controlador = new ControladorCentral();
            
            hiloControlador = new Thread(() -> {
                System.out.println("\n╔═══════════════════════════════════════════╗");
                System.out.println("║  ACTIVANDO CONTROLADOR CENTRAL           ║");
                System.out.println("║  Puerto clientes: " + ControladorCentral.PUERTO + "                       ║");
                System.out.println("╚═══════════════════════════════════════════╝\n");
                controlador.iniciar();
            });
            hiloControlador.start();
            
            // Enviar latidos periódicos
            Thread latidos = new Thread(this::enviarLatidos);
            latidos.setDaemon(true);
            latidos.start();
        }
    }
    
    private void desactivarControlador() {
        if (controlador != null && hiloControlador != null) {
            // El controlador se cerrará cuando se detenga su ServerSocket
            controlador = null;
            if (hiloControlador.isAlive()) {
                hiloControlador.interrupt();
            }
        }
    }
    
    private void enviarLatidos() {
        while (esLider.get()) {
            try {
                Thread.sleep(3000);
                // Intentar enviar latido, pero no mostrar error si falla
                enviarMensajeSilencioso("LATIDO:" + id);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void monitorearLider() {
        while (true) {
            try {
                Thread.sleep(5000);
                
                // Solo monitorear si NO soy el líder
                if (!esLider.get() && lider != -1) {
                    long tiempoSinLatido = System.currentTimeMillis() - ultimoLatido;
                    
                    if (tiempoSinLatido > 10000) { // 10 segundos sin latido
                        System.out.println("\n╔═══════════════════════════════════════════╗");
                        System.out.println("║  ⚠ LÍDER CAÍDO - Iniciando elección     ║");
                        System.out.println("╚═══════════════════════════════════════════╝\n");
                        lider = -1;
                        iniciarEleccion();
                        ultimoLatido = System.currentTimeMillis();
                    }
                }
                
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void enviarMensaje(String mensaje) {
        new Thread(() -> {
            enviarMensajeConTimeout(mensaje, 5000);
        }).start();
    }
    
    private void enviarMensajeSilencioso(String mensaje) {
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", puertoSiguiente), 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(mensaje);
                out.flush();
                socket.close();
            } catch (IOException e) {
                // Silencioso - no imprimir errores de latidos
            }
        }).start();
    }
    
    private boolean enviarMensajeConTimeout(String mensaje, int timeoutMs) {
        int intentos = 0;
        boolean enviado = false;
        
        while (intentos < 3 && !enviado) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", puertoSiguiente), timeoutMs);
                
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(mensaje);
                out.flush();
                
                socket.close();
                enviado = true;
                
            } catch (IOException e) {
                intentos++;
                if (intentos >= 3) {
                    System.err.println("[ERROR] No se pudo enviar mensaje al puerto " + 
                        puertoSiguiente + " después de " + intentos + " intentos");
                    return false;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }
        return enviado;
    }
}
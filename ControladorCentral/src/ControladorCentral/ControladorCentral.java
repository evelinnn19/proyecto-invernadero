package ControladorCentral;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ControladorCentral {
    public void iniciarServidor(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("🌐 Controlador esperando en puerto " + port);

            while (true) {
                Socket cliente = server.accept();
                System.out.println("🔗 Cliente conectado: " + cliente);

                new Thread(() -> manejarCliente(cliente)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void manejarCliente(Socket cliente) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
             PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)) {

            // En este ejemplo: apenas se conecta, le ordeno iniciar fertirrigación
            out.println("INICIAR_FERTI");

            String respuesta = in.readLine();
            System.out.println("Respuesta del cliente: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

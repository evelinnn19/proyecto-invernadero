package ControladorCentral;

import java.io.IOException;

/**
 * @author Alumnos
 * 
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java ControladorCentral.Main <id> <puerto_anillo> <puerto_siguiente>");
            return;
        }
        
        int id = Integer.parseInt(args[0]);
        int puertoAnillo = Integer.parseInt(args[1]);
        int puertoSiguiente = Integer.parseInt(args[2]);
        
        NodoAnillo nodo = new NodoAnillo(id, puertoAnillo, puertoSiguiente);
        nodo.iniciar();
    }

}

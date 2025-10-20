package ControladorCentral;

import java.io.IOException;

/**
 * @author Alumnos
 * 
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equalsIgnoreCase("backup")) {
            System.out.println("Iniciando Controlador Central en modo BACKUP...");
            BackupController.iniciarBackup();
        } else {
            System.out.println("Iniciando Controlador Central en modo PRINCIPAL...");
            MainController.iniciarPrincipal();
        }
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Archivos;
import java.io.*;
import java.nio.file.Files;
/**
 *
 * @author fabri
 */
public class Archivo {

    public static boolean crear(String nombre, String contenido) {
        File archivo = new File(nombre + ".txt");
        try (PrintWriter escribir = new PrintWriter(archivo)) {
            escribir.print(contenido);
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public static boolean guardar(String nombre, String contenido) {
        File archivo = new File(nombre);
        try (PrintWriter escribir = new PrintWriter(archivo)) {
            escribir.print(contenido);
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public static String leer(File archivo) throws IOException {
        return Files.readString(archivo.toPath());
    }
}

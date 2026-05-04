/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Archivos;
import java.io.*;
import javax.swing.JOptionPane;
/**
 *
 * @author fabri
 */
public class Archivo {
    
    public static void crear(String nombre,String contenido){
        File archivo=new File(nombre+".txt");
        PrintWriter escribir;
        try {
            escribir =new PrintWriter(archivo);
            escribir.print(contenido);
            escribir.close();
            JOptionPane.showMessageDialog(null, "ARCHIVO CREADO CON EXITO");
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "ERROR AL CREAR ARCHIVO");
        }
    }
    public static void guardar(String nombre,String contenido){
        File archivo=new File(nombre);
        PrintWriter escribir;
        try {
            escribir =new PrintWriter(archivo);
            escribir.print(contenido);
            escribir.close();
            JOptionPane.showMessageDialog(null, "ARCHIVO GUARDADO CON EXITO");
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "ERROR AL GUARDARR ARCHIVO");
        }
    }
    
    public static String leer(File archivo){
        String resultado="";
        try ( BufferedReader entrada = new BufferedReader(new FileReader(archivo))) {
            resultado= entrada.readAllAsString();
            entrada.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);

        }
        
        return resultado;
    }
}

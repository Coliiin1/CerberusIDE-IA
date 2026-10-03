/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorSintactico;

import java.util.ArrayList;

/**
 *
 * @author fabri
 */
public class Documentador {
    ArrayList<String> lista;

    public Documentador() {
        lista=new ArrayList<>();
    }
    
    public void agregar(String str){
        lista.add(str+"\n");
    }
    
    public String imprimir(){
        String res="";
        for(String str: lista){
            res=res+str;
        }
        return res;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UI;

import Archivos.Archivo;
import codigo.AnalizadorLexico;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Image;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
/**
 *
 * @author fabri
 */
public class InterfzaPrincipal extends JFrame implements ActionListener{

    AnalizadorLexico lex=new AnalizadorLexico();
    File f;
    
    Color gris=new Color(203,203,203);
    Color negro=new Color(143,143,143);
    Color grisOscuro=new Color(70,70,70);
    Color negroOscuro=new Color(50,50,50);
    Color blanco=new Color(255,255,255);
    Color fullNegro=new Color(0,0,0);
    
    JButton btnCompilar;
    
    
    //editor
    JScrollPane scroll;
    JTextArea txt;
    
    JTextArea txtArchivo;
    
    JTextArea txt1;
    JScrollPane scroll2;
    //Menu
    JMenuBar barraMenu;
    JMenu menArchivo;
    JMenu menOpciones;
    JMenuItem itemArchivoAbrir;
    JMenuItem itemOpcionesSalir;
    JRadioButtonMenuItem radioOpcionesOscuro;
    
    JFileChooser selector=new JFileChooser();
    FileNameExtensionFilter filtro = new FileNameExtensionFilter("Texto (.txt)", "txt");
    
    public InterfzaPrincipal() {
        setTitle("CERBERUS IDE");
        setLayout(null);
        setSize(1400,800);

        //setResizable(false);
        getContentPane().setBackground(gris);
        setDefaultCloseOperation(EXIT_ON_CLOSE);  
    }

    public void iniciarComponentes(){
        
 
       btnCompilar = new JButton("COMPILAR");
       btnCompilar.setBounds(1050, 50, 100, 30);
       btnCompilar.addActionListener(this);
       add(btnCompilar);
        //inicio de editor
        txt=new JTextArea("");
        scroll=new JScrollPane(txt);
        scroll.setBorder(null);
        scroll.setBounds(10,50,1000,400);
        add(scroll);
        
        txtArchivo=new JTextArea("ARCHIVO: ");
        txtArchivo.setBounds(10, 460, 1000, 20);
        txtArchivo.setEditable(false);
        txtArchivo.setFont(new Font("Arial",Font.BOLD,20));
        add(txtArchivo);
        
        txt1=new JTextArea("");
        scroll2=new JScrollPane(txt1);
        scroll2.setBorder(null);
        scroll2.setBounds(10, 500, 1000, 200);
        add(scroll2);
        //inicio de meus 
        barraMenu=new JMenuBar();
        menArchivo=new JMenu("Archivo");
        menOpciones=new JMenu("Opciones");
        
        itemArchivoAbrir=new JMenuItem("Abrir");
        itemArchivoAbrir.addActionListener(this);
        menArchivo.add(itemArchivoAbrir);
        
        itemOpcionesSalir=new JMenuItem("SALIR");
        itemOpcionesSalir.addActionListener(this);
        menOpciones.add(itemOpcionesSalir);
        
        radioOpcionesOscuro=new JRadioButtonMenuItem("Modo Oscuro");
        radioOpcionesOscuro.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent e) {
               if(radioOpcionesOscuro.isSelected()){
                   getContentPane().setBackground(grisOscuro);
                   txt.setBackground(negroOscuro);
                   txt.setForeground(blanco);
                   txt.setCaretColor(blanco);
                   txt1.setBackground(negroOscuro);
                   txt1.setForeground(blanco);
                   txt1.setCaretColor(blanco);
                   txtArchivo.setBackground(negroOscuro);
                   txtArchivo.setForeground(blanco);
                   txtArchivo.setCaretColor(blanco);
                   
               }else{
                   getContentPane().setBackground(gris);
                   txt.setBackground(blanco);
                   txt.setForeground(fullNegro);
                   txt.setCaretColor(fullNegro);
                   txt1.setBackground(blanco);
                   txt1.setForeground(fullNegro);
                   txt1.setCaretColor(fullNegro);
                   txtArchivo.setBackground(blanco);
                   txtArchivo.setForeground(fullNegro);
                   txtArchivo.setCaretColor(fullNegro);
                   
               }
           }
            
        });
        menOpciones.add(radioOpcionesOscuro);
        barraMenu.add(menArchivo);
        barraMenu.add(menOpciones);
        barraMenu.setBorder(null);
        barraMenu.setBounds(0,0,2000,30);
        add(barraMenu);
        barraMenu.setBackground(negroOscuro);
        menOpciones.setForeground(blanco);
        menArchivo.setForeground(blanco);
    }
    
    public static void main(String[] args) {
        InterfzaPrincipal ui=new InterfzaPrincipal();
        ui.iniciarComponentes();
        ui.setVisible(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object o=e.getSource();
        if (o==itemArchivoAbrir) {
            selector.setFileFilter(filtro);
            int resultado = selector.showOpenDialog(InterfzaPrincipal.this);
            if(resultado==JFileChooser.APPROVE_OPTION){
                f=selector.getSelectedFile();
                txt.setText(Archivo.leer(f));
            }
            if (f==null) {
                return;
            }
            txtArchivo.setText("ARCHIVO: "+f.getAbsolutePath());
        }
        if(o==itemOpcionesSalir){
            System.exit(0);
        }
        
        if (o==btnCompilar) {
            if (f==null) {
                String nombre="";
                do {
                    nombre=JOptionPane.showInputDialog(rootPane, "INGRESA EL NOMBRE DEL ARCHIVO");
                    if (nombre==null) {
                        return;
                    }
                } while (nombre.equals(""));
                Archivo.crear(nombre,txt.getText());
                
            }else{
                if (!Archivo.leer(f).equals(txt.getText())) {
                    Archivo.guardar(f.getName(), txt.getText());
                }
            }
            
            if (lex.seccionarCadena(txt.getText())) {
                txt1.setText("ANALIZADOR LEXICO PASADO \n"+lex.mostrarTokens());
            }else{
                txt1.setText("ANALIZADOR LEXICO NO PASADO: "+lex.mostrarTokens());
            }
        }
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}

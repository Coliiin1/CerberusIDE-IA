/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txtCodigo to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UI;

import Archivos.Archivo;
import AnalizadorLexico.AnalizadorLexico;
import AnalizadorSintactico.AnalizadorSintactico;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Image;
import java.io.File;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
/**
 *
 * @author fabri
 */
public class InterfzaPrincipal extends JFrame implements ActionListener{

    private final String urlCerberus="/Imagenes/LOGO1.png";
    JLabel imagenLogo;
    
    AnalizadorLexico lex=new AnalizadorLexico();
    AnalizadorSintactico sin;
    File archivo;
    //a
    Color gris=new Color(203,203,203);
    Color negro=new Color(143,143,143);
    Color grisOscuro=new Color(70,70,70);
    Color negroOscuro=new Color(50,50,50);
    Color blanco=new Color(255,255,255);
    Color fullNegro=new Color(0,0,0);
    
    JButton btnCompilar;
    
    
    //editor
    JScrollPane scroll;
    JTextArea txtLineas;
    JTextArea txtCodigo;
    
    JTextArea txtArchivo;
    
    JTextArea txtSintactico;
    JScrollPane scroll3;
    
    JTextArea txtLexico;
    JScrollPane scroll2;
    //Menu
    JMenuBar barraMenu;
    JMenu menArchivo;
    JMenu menOpciones;
    JMenuItem itemArchivoAbrir;
    JMenuItem itemArchivoNuevo;
    JMenuItem itemOpcionesSalir;
    JRadioButtonMenuItem radioOpcionesOscuro;
    
    JFileChooser selector=new JFileChooser();
    FileNameExtensionFilter filtro = new FileNameExtensionFilter("Texto (.txt)", "txt");
    
    public InterfzaPrincipal() {
        setTitle("CERBERUS IDE");
        setLayout(null);
        setSize(1800,1000);

        //setResizable(false);
        getContentPane().setBackground(gris);
        setDefaultCloseOperation(EXIT_ON_CLOSE);  
    }

    public void iniciarComponentes(){
       //imagen
//       ImageIcon iconCerberus = new ImageIcon(getClass().getResource(urlCerberus));
//       
//       Image imagenOriginal = iconCerberus.getImage();
//       Image imagenEscalada = imagenOriginal.getScaledInstance(200, 300, Image.SCALE_SMOOTH);
//       
//       ImageIcon iconFinal = new ImageIcon(imagenEscalada);
//       imagenLogo=new JLabel(iconFinal);
//       imagenLogo.setBounds(1050, 100, 200, 300);
//       add(imagenLogo);
       //botones
       btnCompilar = new JButton("COMPILAR");
       btnCompilar.setBounds(1050, 50, 200, 30);
       btnCompilar.addActionListener(this);
       add(btnCompilar);
        //inicio de editor
        txtCodigo=new JTextArea("");
        txtCodigo.setFont(new java.awt.Font("Consolas", 0, 18));
        
        
        txtCodigo.getDocument().addDocumentListener(new DocumentListener() {

            public String getTextoLineas(){
                int lineas = txtCodigo.getLineCount();
                StringBuilder numeros = new StringBuilder();

                for(int i = 1; i <= lineas; i++){
                    numeros.append(i).append("\n");
                }

                return numeros.toString();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                txtLineas.setText(getTextoLineas());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                txtLineas.setText(getTextoLineas());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                txtLineas.setText(getTextoLineas());
            }
        });
        
        
        txtLineas = new JTextArea("1");
        txtLineas.setBackground(new Color(230,230,230));
        txtLineas.setEditable(false);
        txtLineas.setFont(new Font("Consolas", Font.PLAIN, 18));
        txtLineas.setColumns(3);

        scroll=new JScrollPane(txtCodigo);
        scroll.setRowHeaderView(txtLineas);
        scroll.setBorder(null);
        scroll.setBounds(10,80,1000,800);
        add(scroll);
        
        txtArchivo=new JTextArea("ARCHIVO: ");
        txtArchivo.setBounds(10, 50, 1000, 20);
        txtArchivo.setEditable(false);
        txtArchivo.setFont(new Font("Arial",Font.BOLD,20));
        add(txtArchivo);
        
        txtSintactico = new JTextArea("");
        txtSintactico.setFont(new java.awt.Font("Arial", 0, 18));
        scroll3=new JScrollPane(txtSintactico);
        scroll3.setBorder(null);
        scroll3.setBounds(1050, 90, 500, 200);
        add(scroll3);
        
        txtLexico=new JTextArea("");
        txtLexico.setFont(new java.awt.Font("Arial", 0, 18));
        scroll2=new JScrollPane(txtLexico);
        scroll2.setBorder(null);
        scroll2.setBounds(1050, 300, 500, 200);
        add(scroll2);
        //inicio de meus 
        barraMenu=new JMenuBar();
        menArchivo=new JMenu("Archivo");
        menOpciones=new JMenu("Opciones");
        
        itemArchivoAbrir=new JMenuItem("Abrir");
        itemArchivoAbrir.addActionListener(this);
        itemArchivoNuevo=new JMenuItem("Nuevo");
        itemArchivoNuevo.addActionListener(this);
        menArchivo.add(itemArchivoAbrir);
        menArchivo.add(itemArchivoNuevo);
        
        itemOpcionesSalir=new JMenuItem("SALIR");
        itemOpcionesSalir.addActionListener(this);
        menOpciones.add(itemOpcionesSalir);
        
        radioOpcionesOscuro=new JRadioButtonMenuItem("Modo Oscuro");
        radioOpcionesOscuro.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent e) {
               if(radioOpcionesOscuro.isSelected()){
                   getContentPane().setBackground(grisOscuro);
                   txtCodigo.setBackground(negroOscuro);
                   txtCodigo.setForeground(blanco);
                   txtCodigo.setCaretColor(blanco);
                   txtLexico.setBackground(negroOscuro);
                   txtLexico.setForeground(blanco);
                   txtLexico.setCaretColor(blanco);
                   txtArchivo.setBackground(negroOscuro);
                   txtArchivo.setForeground(blanco);
                   txtArchivo.setCaretColor(blanco);
                   txtSintactico.setBackground(negroOscuro);
                   txtSintactico.setForeground(blanco);
                   txtSintactico.setCaretColor(blanco);
                   
               }else{
                   getContentPane().setBackground(gris);
                   txtCodigo.setBackground(blanco);
                   txtCodigo.setForeground(fullNegro);
                   txtCodigo.setCaretColor(fullNegro);
                   txtLexico.setBackground(blanco);
                   txtLexico.setForeground(fullNegro);
                   txtLexico.setCaretColor(fullNegro);
                   txtArchivo.setBackground(blanco);
                   txtArchivo.setForeground(fullNegro);
                   txtArchivo.setCaretColor(fullNegro);
                   txtSintactico.setBackground(blanco);
                   txtSintactico.setForeground(fullNegro);
                   txtSintactico.setCaretColor(fullNegro);
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
                archivo=selector.getSelectedFile();
                txtCodigo.setText(Archivo.leer(archivo));
            }
            if (archivo==null) {
                return;
            }
            txtArchivo.setText("ARCHIVO: "+archivo.getAbsolutePath());
        }
        if (o==itemArchivoNuevo) {
            txtCodigo.setText("");
            archivo=null;
            txtArchivo.setText("ARCHIVO: ");
        }
        
        if(o==itemOpcionesSalir){
            System.exit(0);
        }
        
        if (o==btnCompilar) {
            lex.limpiarTabla();
            if (archivo==null) {
                String nombre="";
                do {
                    nombre=JOptionPane.showInputDialog(rootPane, "INGRESA EL NOMBRE DEL ARCHIVO");
                    if (nombre==null) {
                        return;
                    }
                } while (nombre.equals(""));
                Archivo.crear(nombre,txtCodigo.getText());
                
            }else{
                if (!Archivo.leer(archivo).equals(txtCodigo.getText())) {
                    Archivo.guardar(archivo.getName(), txtCodigo.getText());
                }
            }
            
            if (lex.seccionarCadena(txtCodigo.getText())) {
                txtLexico.setText("ANALIZADOR LEXICO PASADO \n"+lex.mostrarTokens());
                lex.generarTabla();
                sin=new AnalizadorSintactico(lex.getTokensDetectados());
            }else{
                txtLexico.setText("ANALIZADOR LEXICO NO PASADO: "+lex.mostrarTokens());
                return;
            }
            if(sin.comprobarParentesis()){
                sin.mostrarParentetis();
            }else{
                txtSintactico.setText("HAY UN PROBLEMA CON LLAVES PARENTESIS Y CORCHETES");
            }
            sin.analizar();
            if (sin.getResultado()) {
                txtSintactico.setText("ANALISIS SINATCTICO PASADO\n"+sin.imprimirTabla());
            }else{
                txtSintactico.setText("NO SE PASO EN ANALISIS SINTACTICO");
            }
        }
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}

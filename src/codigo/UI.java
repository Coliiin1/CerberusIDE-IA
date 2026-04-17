/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codigo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.*;
/**
 *
 * @author fabri
 */
public class UI extends JFrame implements ActionListener{
    
    JTextArea txtAnalizar;
    JTextArea txtResultado;
    JButton btnAnalizar;
    public UI(){
        setSize(800,500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("ANALIZADOR LEXICO");
        setLayout(null);
        setResizable(false);
        
    }
    public void contenedor(){
        txtAnalizar=new JTextArea("");
        JScrollPane barra1=new JScrollPane(txtAnalizar);
        barra1.setBounds(10, 10, 550, 80);
        add(barra1);
        
        txtResultado=new JTextArea();
        JScrollPane barra2=new JScrollPane(txtResultado);
        barra2.setBounds(10, 100, 750, 350);
        add(barra2);

        
        btnAnalizar=new JButton("ANALIZAR");
        btnAnalizar.setBounds(600, 10, 150, 80);
        btnAnalizar.addActionListener(this);
        add(btnAnalizar);
    }
    
    public static void main(String[] args) {
        UI ui=new UI();
        ui.contenedor();
        ui.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object evento=e.getSource();
        if(evento==btnAnalizar){
            File archivo=new File("archivo.txt");
            PrintWriter escribir;
            try {
                escribir =new PrintWriter(archivo);
                escribir.print(txtAnalizar.getText());
                escribir.close();
            } catch (FileNotFoundException ex) {
                System.getLogger(UI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
            
            try {
                Reader lector=new BufferedReader(new FileReader("archivo.txt"));
                Lexer lexer=new Lexer(lector);
                String resultado="";
                while (true) {
                    Tokens token=lexer.yylex();
                    if (token==null) {
                        resultado+="YA SE REVISO TODO";
                        txtResultado.setText(resultado);
                        return; 
                    }
                    switch (token) {
                        case FX:
                             System.out.println("CREADO POR EQUIPO 3");
                            break;
                        case ERROR:
                            resultado+="EL SIMBOLO NO ESTA DEFINIDO\n";
                            break;
                        case IDENTIFICADOR: case OPERADOR_SUMA: case OPERADOR_RESTA: case OPERADOR_MULTIPLICAR: case OPERADOR_DIVISION: case OPERADOR_MODULO: 
                        case NUMERO_ENTERO: case NUMERO_REAL: case PUNTO_COMA: case INCREMENTO: case DECREMENTO:case INC_VARIABLE: case DEC_VARIABLE: case MUL_VARIABLE: 
                        case DIV_VARIABLE: case IGUAL: case DIFERENTE: case MENOR,MENOR_IGUAL: case MAYOR: case MAYOR_IGUAL: case AND: case OR: case NEGAR: case TIPO_CARACTER:
                            resultado+=lexer.lexema+": ES UN(A) " + token+"\n";
                            break;
                        default: resultado+="TOKEN: "+lexer.lexema+" "+token+"\n";
                        break;
                    }
                }
            } catch (FileNotFoundException ex) {
                System.getLogger(UI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            } catch (IOException ex) {
                System.getLogger(UI.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }
}

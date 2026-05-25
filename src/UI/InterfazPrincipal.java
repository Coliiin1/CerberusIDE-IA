/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UI;

import Archivos.Archivo;
import AnalizadorLexico.AnalizadorLexico;
import AnalizadorSintactico.AnalizadorSintactico;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ColorUIResource;

/**
 *
 * @author fabri
 */
public class InterfazPrincipal extends JFrame implements ActionListener {

    AnalizadorLexico lex = new AnalizadorLexico();
    AnalizadorSintactico sin;
    File archivo;

    // ======== COLORES ========

    private Color fondoActual;

    private Color panelActual;

    private Color panelSecundarioActual;

    private Color textoActual;

    private Color editorActual;

    private Color lineaActual;

    // ======== COMPONENTES ========

    private JPanel panelSuperior;

    private JPanel panelCentral;

    private JPanel panelEditor;

    private JPanel panelResultados;

    private JLabel lblTitulo;
    JButton btnCompilar;
    JTextArea txtCodigo;
    JTextArea txtLineas;
    JTextArea txtArchivo;
    JTextArea txtLexico;
    JTextArea txtSintactico;
    JTextArea txtSemantico;
    JTextArea txtDocumentador;
    JScrollPane scrollEditor;
    JTabbedPane pestañas;
    JMenuBar barraMenu;
    JMenu menArchivo;
    JMenu menOpciones;
    JMenuItem itemArchivoAbrir;
    JMenuItem itemArchivoNuevo;
    JMenuItem itemOpcionesSalir;
    JRadioButtonMenuItem radioModoClaro;
    JFileChooser selector = new JFileChooser();
    FileNameExtensionFilter filtro =
            new FileNameExtensionFilter("Texto (.txt)", "txt");

    public InterfazPrincipal() {

        setTitle("CERBERUS IDE");
        setSize(1750, 950);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(40,40,40)));
        Image icono = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/Imagenes/LogoC.png"));
        setIconImage(icono);
    }

    // ======== APLICAR UIManager GLOBALES ANTES DE CREAR COMPONENTES ========

    private void aplicarUIManagerTema(boolean oscuro) {

        Color fondo        = oscuro ? new Color(30,30,34)   : new Color(255,255,255);
        Color fondoPrinc   = oscuro ? new Color(18,18,20)   : new Color(245,245,245);
        Color fondoSecun   = oscuro ? new Color(45,45,50)   : new Color(235,235,235);
        Color texto        = oscuro ? new Color(235,235,235) : new Color(25,25,25);
        Color linea        = oscuro ? new Color(65,65,70)   : new Color(210,210,210);
        Color seleccionada = oscuro ? new Color(55,55,62)   : new Color(220,220,220);

        // ---- MenuBar y Menus ----
        UIManager.put("MenuBar.background",         new ColorUIResource(fondo));
        UIManager.put("MenuBar.foreground",         new ColorUIResource(texto));
        UIManager.put("MenuBar.border",             BorderFactory.createEmptyBorder());
        UIManager.put("MenuBar.shadow",             new ColorUIResource(fondo));
        UIManager.put("MenuBar.highlight",          new ColorUIResource(fondo));
        UIManager.put("MenuBar.gradient",           null);
        UIManager.put("Menu.background",            new ColorUIResource(fondo));
        UIManager.put("Menu.foreground",            new ColorUIResource(texto));
        UIManager.put("Menu.selectionBackground",   new ColorUIResource(fondoSecun));
        UIManager.put("Menu.selectionForeground",   new ColorUIResource(texto));
        UIManager.put("Menu.border",                BorderFactory.createEmptyBorder(4,8,4,8));
        UIManager.put("Menu.borderPainted",         false);
        UIManager.put("Menu.opaque",                true);
        UIManager.put("MenuItem.background",        new ColorUIResource(fondo));
        UIManager.put("MenuItem.foreground",        new ColorUIResource(texto));
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(fondoSecun));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(texto));
        UIManager.put("MenuItem.border",            BorderFactory.createEmptyBorder(5,10,5,10));
        UIManager.put("MenuItem.borderPainted",     false);
        UIManager.put("RadioButtonMenuItem.background",          new ColorUIResource(fondo));
        UIManager.put("RadioButtonMenuItem.foreground",          new ColorUIResource(texto));
        UIManager.put("RadioButtonMenuItem.selectionBackground", new ColorUIResource(fondoSecun));
        UIManager.put("RadioButtonMenuItem.selectionForeground", new ColorUIResource(texto));
        UIManager.put("RadioButtonMenuItem.border",              BorderFactory.createEmptyBorder(5,10,5,10));
        UIManager.put("PopupMenu.background",       new ColorUIResource(fondo));
        UIManager.put("PopupMenu.foreground",       new ColorUIResource(texto));
        UIManager.put("PopupMenu.border",           BorderFactory.createLineBorder(linea));
        UIManager.put("Separator.foreground",       new ColorUIResource(linea));
        UIManager.put("Separator.background",       new ColorUIResource(fondo));

        // ---- TabbedPane ----
        UIManager.put("TabbedPane.background",             new ColorUIResource(fondo));
        UIManager.put("TabbedPane.foreground",             new ColorUIResource(texto));
        UIManager.put("TabbedPane.selected",               new ColorUIResource(seleccionada));
        UIManager.put("TabbedPane.selectedForeground",     new ColorUIResource(texto));
        UIManager.put("TabbedPane.unselectedBackground",   new ColorUIResource(fondo));
        UIManager.put("TabbedPane.contentAreaColor",       new ColorUIResource(fondoPrinc));
        UIManager.put("TabbedPane.borderHightlightColor",  new ColorUIResource(linea));
        UIManager.put("TabbedPane.darkShadow",             new ColorUIResource(linea));
        UIManager.put("TabbedPane.shadow",                 new ColorUIResource(fondo));
        UIManager.put("TabbedPane.light",                  new ColorUIResource(fondo));
        UIManager.put("TabbedPane.focus",                  new ColorUIResource(fondo));
        UIManager.put("TabbedPane.tabAreaBackground",      new ColorUIResource(fondo));
        UIManager.put("TabbedPane.selectHighlight",        new ColorUIResource(seleccionada));
        UIManager.put("TabbedPane.contentBorderInsets",    new Insets(2,2,2,2));
        UIManager.put("TabbedPane.tabInsets",              new Insets(6,14,6,14));

        // ---- ScrollBar ----
        UIManager.put("ScrollBar.background",       new ColorUIResource(fondo));
        UIManager.put("ScrollBar.thumb",            new ColorUIResource(fondoSecun));
        UIManager.put("ScrollBar.thumbHighlight",   new ColorUIResource(fondoSecun));
        UIManager.put("ScrollBar.thumbDarkShadow",  new ColorUIResource(fondo));
        UIManager.put("ScrollBar.thumbShadow",      new ColorUIResource(fondo));
        UIManager.put("ScrollBar.track",            new ColorUIResource(fondo));
        UIManager.put("ScrollBar.trackHighlight",   new ColorUIResource(fondo));

        // ---- Panel / general ----
        UIManager.put("Panel.background", new ColorUIResource(fondoPrinc));
    }

    public void iniciarComponentes() {

        // ======== PANEL SUPERIOR ========

        panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setPreferredSize(new Dimension(0, 65));
        panelSuperior.setBorder(new EmptyBorder(10, 20, 10, 20));
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Imagenes/logoCT.png"));
        Image logoEscalado = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);

        lblTitulo = new JLabel("CERBERUS IDE", new ImageIcon(logoEscalado), JLabel.LEFT);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setIconTextGap(10);
        btnCompilar = new JButton("COMPILAR");
        btnCompilar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCompilar.setPreferredSize(new Dimension(160, 40));
        btnCompilar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCompilar.setFocusPainted(false);
        btnCompilar.setBorderPainted(false);
        btnCompilar.setContentAreaFilled(true);
        btnCompilar.setOpaque(true);
        btnCompilar.setBackground(new Color(0, 54, 166));
        btnCompilar.setForeground(Color.WHITE);
        btnCompilar.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        btnCompilar.addActionListener(this);
        panelSuperior.add(lblTitulo, BorderLayout.WEST);
        panelSuperior.add(btnCompilar, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // ======== PANEL CENTRAL ========

        panelCentral = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        // ======== PANEL EDITOR ========

        panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(new EmptyBorder(10, 10, 10, 5));
        txtArchivo = new JTextArea("ARCHIVO: ");
        txtArchivo.setEditable(false);
        txtArchivo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtArchivo.setBorder(new EmptyBorder(12, 12, 12, 12));
        txtCodigo = new JTextArea();
        txtCodigo.setFont(new Font("Consolas", Font.PLAIN, 18));
        txtCodigo.setMargin(new Insets(15, 15, 15, 15));
        txtCodigo.setLineWrap(false);
        txtCodigo.setTabSize(4);
        txtCodigo.getDocument().addDocumentListener(new DocumentListener() {

            public String getTextoLineas() {

                int lineas = txtCodigo.getLineCount();
                StringBuilder numeros = new StringBuilder();

                for (int i = 1; i <= lineas; i++) {

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
        txtLineas.setEditable(false);
        txtLineas.setFont(new Font("Consolas", Font.PLAIN, 18));
        txtLineas.setBorder(new EmptyBorder(12, 10, 12, 10));
        scrollEditor = new JScrollPane(txtCodigo);
        scrollEditor.setRowHeaderView(txtLineas);
        panelEditor.add(txtArchivo, BorderLayout.NORTH);
        panelEditor.add(scrollEditor, BorderLayout.CENTER);

        // ======== PANEL RESULTADOS ========

        panelResultados = new JPanel(new BorderLayout());
        panelResultados.setBorder(new EmptyBorder(10, 5, 10, 10));
        pestañas = new JTabbedPane();
        pestañas.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        pestañas.setFont(new Font("Segoe UI", Font.BOLD, 15));

        // Forzar que el TabbedPane use los colores del UIManager
        pestañas.setOpaque(true);
        txtLexico = crearAreaResultado();
        txtSintactico = crearAreaResultado();
        txtSemantico = crearAreaResultado();
        txtDocumentador = crearAreaResultado();
        pestañas.addTab("Analizador Léxico",     crearPanelConScroll(txtLexico));
        pestañas.addTab("Analizador Sintáctico", crearPanelConScroll(txtSintactico));
        pestañas.addTab("Analizador Semántico",  crearPanelConScroll(txtSemantico));
        pestañas.addTab("Documentador",          crearPanelConScroll(txtDocumentador));
        panelResultados.add(pestañas, BorderLayout.CENTER);

        // ======== GRID ========

        gbc.gridx = 0;
        gbc.weightx = 0.72;
        gbc.weighty = 1.0;
        panelCentral.add(panelEditor, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.28;
        panelCentral.add(panelResultados, gbc);
        add(panelCentral, BorderLayout.CENTER);

        // ======== MENU ========

        barraMenu = new JMenuBar() {
            // Sobrescribir para eliminar el espacio/relleno blanco al final
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        menArchivo = new JMenu("Archivo");
        menOpciones = new JMenu("Opciones");
        itemArchivoAbrir = new JMenuItem("Abrir");
        itemArchivoAbrir.addActionListener(this);
        itemArchivoNuevo = new JMenuItem("Nuevo");
        itemArchivoNuevo.addActionListener(this);
        itemOpcionesSalir = new JMenuItem("Salir");
        itemOpcionesSalir.addActionListener(this);
        radioModoClaro = new JRadioButtonMenuItem("Modo Claro");
        radioModoClaro.addActionListener(e -> {

            if (radioModoClaro.isSelected()) {

                aplicarModoClaro();
            } else {

                aplicarModoOscuro();
            }

        });

        menArchivo.add(itemArchivoAbrir);
        menArchivo.add(itemArchivoNuevo);
        menOpciones.add(radioModoClaro);
        menOpciones.add(itemOpcionesSalir);
        barraMenu.add(menArchivo);
        barraMenu.add(menOpciones);
        setJMenuBar(barraMenu);
        aplicarModoOscuro();

    }

    // ======== CREAR AREA ========

    private JTextArea crearAreaResultado() {

        JTextArea area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 16));
        area.setEditable(false);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        return area;

    }

    // ======== CREAR SCROLL ========

    private JScrollPane crearPanelConScroll(JTextArea area) {

        return new JScrollPane(area);

    }

    // ======== ESTILOS ========

    private void estilizarArea(JTextArea area) {

        area.setBackground(editorActual);
        area.setForeground(textoActual);
        area.setCaretColor(textoActual);
        area.setSelectedTextColor(Color.WHITE);
        area.setSelectionColor(new Color(0,122,255));

    }
    // ======== MODOS ========

    private void aplicarModoOscuro() {
        
        fondoActual           = new Color(18,18,20);
        panelActual           = new Color(30,30,34);
        panelSecundarioActual = new Color(45,45,50);
        textoActual           = new Color(235,235,235);
        editorActual          = new Color(22,22,24);
        lineaActual           = new Color(65,65,70);

        // Aplicar UIManager ANTES de actualizar componentes
        aplicarUIManagerTema(true);
        actualizarTema();

    }

    private void aplicarModoClaro() {

        fondoActual           = new Color(245,245,245);
        panelActual           = new Color(255,255,255);
        panelSecundarioActual = new Color(235,235,235);
        textoActual           = new Color(25,25,25);
        editorActual          = new Color(252,252,252);
        lineaActual           = new Color(210,210,210);

        // Aplicar UIManager ANTES de actualizar componentes
        aplicarUIManagerTema(false);
        actualizarTema();

    }

    // ======== TEMA ========

    private void actualizarTema() {

        getContentPane().setBackground(fondoActual);
        panelSuperior.setBackground(panelActual);
        panelCentral.setBackground(fondoActual);
        panelEditor.setBackground(fondoActual);
        panelResultados.setBackground(fondoActual);
        lblTitulo.setForeground(textoActual);
        estilizarArea(txtCodigo);
        estilizarArea(txtLexico);
        estilizarArea(txtSintactico);
        estilizarArea(txtSemantico);
        estilizarArea(txtDocumentador);
        txtLineas.setBackground(panelSecundarioActual);
        txtLineas.setForeground(textoActual);
        txtArchivo.setBackground(panelActual);
        txtArchivo.setForeground(textoActual);
        scrollEditor.setBorder(BorderFactory.createLineBorder(lineaActual));
        scrollEditor.setBackground(editorActual);
        scrollEditor.getViewport().setBackground(editorActual);
        scrollEditor.getVerticalScrollBar().setBackground(panelActual);
        scrollEditor.getHorizontalScrollBar().setBackground(panelActual);

        // ---- TabbedPane: actualizar colores directamente además del UIManager ----

        pestañas.setBackground(panelActual);
        pestañas.setForeground(textoActual);
        pestañas.setOpaque(true);
        pestañas.setBorder(BorderFactory.createLineBorder(lineaActual));

        // Actualizar cada ScrollPane interno de las pestañas
        for (int i = 0; i < pestañas.getTabCount(); i++) {

            JScrollPane sp = (JScrollPane) pestañas.getComponentAt(i);
            sp.setBorder(BorderFactory.createLineBorder(lineaActual));
            sp.setBackground(editorActual);
            sp.getViewport().setBackground(editorActual);
            sp.getVerticalScrollBar().setBackground(panelActual);
            sp.getHorizontalScrollBar().setBackground(panelActual);
        }

        // ---- Barra de menú ----

        barraMenu.setBackground(panelActual);
        barraMenu.setOpaque(true);
        barraMenu.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, lineaActual));

        // Forzar que el área vacía de la barra de menú también use el color correcto
        barraMenu.putClientProperty("JMenuBar.background", panelActual);

        // ---- Menús ----

        JMenu[] menus = {menArchivo, menOpciones};

        for (JMenu menu : menus) {

            menu.setForeground(textoActual);
            menu.setBackground(panelActual);
            menu.setOpaque(true);
            menu.setBorderPainted(false);
        }

        // ---- Items de menú ----

        JMenuItem[] items = {
                itemArchivoAbrir,
                itemArchivoNuevo,
                itemOpcionesSalir,
                radioModoClaro
        };

        for (JMenuItem item : items) {

            item.setBackground(panelActual);
            item.setForeground(textoActual);
            item.setOpaque(true);
            item.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        }

        // Forzar actualización de UI en pestañas para que tome los nuevos colores del UIManager
        SwingUtilities.updateComponentTreeUI(pestañas);

        // Re-aplicar colores de areas de resultado porque updateComponentTreeUI puede resetearlos
        estilizarArea(txtLexico);
        estilizarArea(txtSintactico);
        estilizarArea(txtSemantico);
        estilizarArea(txtDocumentador);

        for (int i = 0; i < pestañas.getTabCount(); i++) {

            JScrollPane sp = (JScrollPane) pestañas.getComponentAt(i);
            sp.setBorder(BorderFactory.createLineBorder(lineaActual));
            sp.setBackground(editorActual);
            sp.getViewport().setBackground(editorActual);
        }

        repaint();
        revalidate();

    }
    @Override
    public void actionPerformed(ActionEvent e) {

        Object o = e.getSource();

        if (o == itemArchivoAbrir) {

            selector.setFileFilter(filtro);
            int resultado = selector.showOpenDialog(InterfazPrincipal.this);

            if (resultado == JFileChooser.APPROVE_OPTION) {

                archivo = selector.getSelectedFile();
                txtCodigo.setText(Archivo.leer(archivo));
            }

            if (archivo == null) {

                return;
            }

            txtArchivo.setText("ARCHIVO: " + archivo.getAbsolutePath());
        }

        if (o == itemArchivoNuevo) {

            txtCodigo.setText("");
            archivo = null;
            txtArchivo.setText("ARCHIVO: ");
        }

        if (o == itemOpcionesSalir) {

            System.exit(0);
        }

        if (o == btnCompilar) {

            lex.limpiarTabla();

            if (archivo == null) {

                String nombre = "";

                do {

                    nombre = JOptionPane.showInputDialog(rootPane, "INGRESA EL NOMBRE DEL ARCHIVO");

                    if (nombre == null) {

                        return;
                    }

                } while (nombre.equals(""));

                Archivo.crear(nombre, txtCodigo.getText());
            } else {

                if (!Archivo.leer(archivo).equals(txtCodigo.getText())) {

                    Archivo.guardar(archivo.getName(), txtCodigo.getText());
                }

            }

            // ===== LEXICO =====

            if (lex.seccionarCadena(txtCodigo.getText())) {

                txtLexico.setText("ANALIZADOR LÉXICO PASADO\n\n" + lex.mostrarTokens());
                lex.generarTabla();
                sin = new AnalizadorSintactico(lex.getTokensDetectados());
            } else {

                txtLexico.setText("ANALIZADOR LÉXICO NO PASADO\n\n" + lex.mostrarTokens());
                return;
            }
            // ===== SINTACTICO =====
            if (sin.comprobarParentesis()) {
                sin.mostrarParentesis();
            } else {
                txtSintactico.setText("HAY UN PROBLEMA CON LLAVES, PARÉNTESIS Y CORCHETES");
            }
            sin.analizar();
            if (sin.getResultado()) {

                txtSintactico.setText("ANÁLISIS SINTÁCTICO PASADO\n\n" + sin.imprimirTabla());
            } else {

                txtSintactico.setText("NO SE PASÓ EL ANÁLISIS SINTÁCTICO");
            }
            // ===== SEMANTICO =====
            txtSemantico.setText("ANALIZADOR SEMÁNTICO DISPONIBLE\n\nAÚN NO IMPLEMENTADO.");
            // ===== DOCUMENTADOR =====
            if (!sin.getResultado()) {
                txtDocumentador.setText("NO SE PASO NI EL SEMANTICO NI EL SINTACTICO NO SE PUEDE CREAR DOCUMENTADOR");
            }else{
                txtSemantico.setText("SE PASO EL ANALIZADOR SEMANTICO");
                txtDocumentador.setText(sin.getDocumentador().imprimir());
            }
            
            
            
            
        }
    }

}
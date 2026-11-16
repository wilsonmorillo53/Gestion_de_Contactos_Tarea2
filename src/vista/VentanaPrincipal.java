package vista;

import controlador.BusquedaContactoTask;      // Unidad 3: búsqueda con Thread
import controlador.BusquedaExecutorTask;       // Unidad 3: búsqueda con ExecutorService
import controlador.NotificacionTask;           // Unidad 3: avisos con Thread
import controlador.ValidacionContactoTask;     // Unidad 3: validación con Thread
import modelo.Contacto;
import modelo.ContactoDAO;
import util.ExportadorCSV;
import util.FuenteUtil;
import util.GestorIdioma;
import vista.componentes.BotonPersonalizado;
import vista.componentes.CampoTextoRedondeado;
import vista.componentes.IconoUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class VentanaPrincipal extends JFrame implements GestorIdioma.IdiomaListener {

    // Paleta de colores exacta de la imagen
    private static final Color COLOR_HEADER = new Color(11, 58, 98);
    private static final Color COLOR_TAB_BAR = new Color(216, 231, 245);
    private static final Color COLOR_FONDO = new Color(234, 242, 249);
    private static final Color COLOR_BORDE = new Color(11, 58, 98);

    // Colores de los 6 botones
    private static final Color COLOR_BTN_AGREGAR = new Color(0, 102, 204);
    private static final Color COLOR_BTN_MODIFICAR = new Color(0, 120, 215);
    private static final Color COLOR_BTN_ELIMINAR = new Color(217, 67, 67);
    private static final Color COLOR_BTN_BUSCAR = new Color(0, 90, 168);
    private static final Color COLOR_BTN_EXPORTAR = new Color(0, 132, 119);
    private static final Color COLOR_BTN_IDIOMAS = new Color(92, 77, 183);

    private final ContactoDAO dao = new ContactoDAO();
    private DefaultTableModel modeloTabla;
    private final JTable tabla;
    private TableRowSorter<DefaultTableModel> sorter;

    // Barra superior
    private final JLabel lblTituloVentana = new JLabel();
    private final JLabel lblIdiomaHeader = new JLabel();
    private final BotonPillIdioma btnPillIdioma;

    // Tabs
    private final BotonTab btnTabContactos;
    private final BotonTab btnTabEstadisticas;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenedorCards = new JPanel(cardLayout);

    // Formulario
    private final JLabel lblAvatar = new JLabel();
    private final JLabel lblNombre = new JLabel();
    private final JLabel lblTelefono = new JLabel();
    private final JLabel lblEmail = new JLabel();

    private final CampoTextoRedondeado txtNombre = new CampoTextoRedondeado(14);
    private final CampoTextoRedondeado txtTelefono = new CampoTextoRedondeado(14);
    private final CampoTextoRedondeado txtEmail = new CampoTextoRedondeado(14);
    private final CampoTextoRedondeado txtBuscar = new CampoTextoRedondeado(16);

    // Botones de acción
    private BotonPersonalizado btnAgregar;
    private BotonPersonalizado btnModificar;
    private BotonPersonalizado btnEliminar;
    private BotonPersonalizado btnBuscar;
    private BotonPersonalizado btnExportar;
    private BotonPersonalizado btnIdiomas;

    // Barra inferior
    private final JLabel lblBuscarTexto = new JLabel();
    private final JLabel lblIconoEstado = new JLabel();
    private final JLabel lblEstado = new JLabel();
    private final JLabel lblAviso = new JLabel();
    private final JPanel panelAviso = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final Timer ocultarAviso = new Timer(3200, e -> {
        panelAviso.setVisible(false);
        if (panelAviso.getParent() != null) panelAviso.getParent().repaint();
    });

    private final PanelEstadisticas panelEstadisticas = new PanelEstadisticas();

    private int filaModeloEditando = -1;
    private ContactoDAO.BloqueoEdicion bloqueoEdicion;
    private int mouseX, mouseY;

    public VentanaPrincipal() {
        // Inicializar tabla con dibujo de cuadrícula extendida
        this.tabla = new JTable() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Dibuja las líneas de cuadrícula para las filas vacías que quedan hasta el fondo
                int rowHeight = getRowHeight();
                int rowCount = getRowCount();
                int y = rowCount * rowHeight;
                int tableWidth = getWidth();
                int height = getHeight();
                g.setColor(getGridColor());
                while (y < height) {
                    g.drawLine(0, y, tableWidth, y);
                    y += rowHeight;
                }
                // Líneas verticales de columnas para el área vacía
                TableColumnModel cm = getColumnModel();
                int x = 0;
                for (int i = 0; i < cm.getColumnCount() - 1; i++) {
                    x += cm.getColumn(i).getWidth();
                    g.drawLine(x - 1, rowCount * rowHeight, x - 1, height);
                }
            }
        };

        // Inicializar tabs personalizados
        this.btnTabContactos = new BotonTab("Contactos", "contactos_tab.png", "contactos_tab_dark.png", true);
        this.btnTabEstadisticas = new BotonTab("Estadísticas", "estadisticas_tab_white.png", "estadisticas_tab.png", false);
        this.btnPillIdioma = new BotonPillIdioma();

        configurarVentana();
        construirInterfaz();
        iniciarNotificaciones();
        GestorIdioma.agregarListener(this);
        idiomaCambiado(GestorIdioma.getIdiomaActual());
        actualizarEstadisticas();
    }

    private void configurarVentana() {
        setUndecorated(true);
        setSize(880, 560);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        // Cierra el ExecutorService al salir de la aplicación
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                dao.desbloquearEdicion(bloqueoEdicion);
                BusquedaExecutorTask.cerrar();
            }
        });
    }

    private void construirInterfaz() {
        JPanel panelRaiz = new JPanel(new BorderLayout());
        panelRaiz.setBackground(COLOR_FONDO);
        panelRaiz.setBorder(new LineBorder(COLOR_BORDE, 1));

        // 1. Barra de Título Superior
        panelRaiz.add(construirBarraTitulo(), BorderLayout.NORTH);

        // 2. Contenedor Central (Barra de Tabs + Paneles)
        JPanel panelCuerpo = new JPanel(new BorderLayout());
        panelCuerpo.setBackground(COLOR_FONDO);
        panelCuerpo.add(construirBarraTabs(), BorderLayout.NORTH);

        // Tarjetas
        panelContenedorCards.setOpaque(false);
        panelContenedorCards.add(construirPanelContactos(), "contactos");
        panelContenedorCards.add(panelEstadisticas, "estadisticas");
        panelCuerpo.add(panelContenedorCards, BorderLayout.CENTER);

        panelRaiz.add(panelCuerpo, BorderLayout.CENTER);

        // 3. Barra Inferior (Buscar + Estado)
        panelRaiz.add(construirBarraInferior(), BorderLayout.SOUTH);

        setContentPane(panelRaiz);
    }

    // =========================================================================
    // 1. BARRA DE TÍTULO MODERNA CON CONTROLES Y CAMBIO DE IDIOMA
    // =========================================================================
    private JPanel construirBarraTitulo() {
        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setBackground(COLOR_HEADER);
        barra.setPreferredSize(new Dimension(getWidth(), 44));
        barra.setBorder(new EmptyBorder(4, 12, 4, 8));

        // Arrastrar ventana
        barra.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleMaximizar();
                }
            }
        });
        barra.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - mouseX, getLocation().y + e.getY() - mouseY);
            }
        });

        // Izquierda: Icono App + Título
        JPanel panelIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        panelIzq.setOpaque(false);

        JLabel lblIconoApp = new JLabel(IconoUtil.getIcono("app_icon.png", 28, 28));
        lblTituloVentana.setFont(FuenteUtil.cargarFuenteBold(15f));
        lblTituloVentana.setForeground(Color.WHITE);

        panelIzq.add(lblIconoApp);
        panelIzq.add(lblTituloVentana);
        barra.add(panelIzq, BorderLayout.WEST);

        // Derecha: Selector de Idioma (Pill) + Botones Ventana
        JPanel panelDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        panelDer.setOpaque(false);

        // Botón Pill de Idioma
        configurarBotonPillIdioma();
        panelDer.add(btnPillIdioma);

        // Botones de ventana con trazado vectorial exacto
        JButton btnMinimizar = crearBotonVentana(0, e -> setState(JFrame.ICONIFIED));
        JButton btnMaximizar = crearBotonVentana(1, e -> toggleMaximizar());
        JButton btnCerrar = crearBotonVentana(2, e -> {
            BusquedaExecutorTask.cerrar();
            System.exit(0);
        });

        panelDer.add(btnMinimizar);
        panelDer.add(btnMaximizar);
        panelDer.add(btnCerrar);

        barra.add(panelDer, BorderLayout.EAST);

        return barra;
    }

    private void toggleMaximizar() {
        if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
            setExtendedState(JFrame.NORMAL);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private void configurarBotonPillIdioma() {
        btnPillIdioma.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 3));

        lblIdiomaHeader.setFont(FuenteUtil.cargarFuente(12f));
        lblIdiomaHeader.setForeground(Color.WHITE);

        JLabel iconoGlobo = new JLabel(IconoUtil.getIcono("idiomas.png", 16, 16));

        btnPillIdioma.add(iconoGlobo);
        btnPillIdioma.add(lblIdiomaHeader);

        btnPillIdioma.addActionListener(e -> mostrarMenuIdiomas(btnPillIdioma));
    }

    // Tipo: 0 = Minimizar, 1 = Maximizar, 2 = Cerrar
    private JButton crearBotonVentana(int tipo, ActionListener accion) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (getModel().isRollover()) {
                    g2.setColor(tipo == 2 ? new Color(232, 17, 35) : new Color(30, 85, 135));
                    g2.fillRect(0, 0, w, h);
                }

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = w / 2;
                int cy = h / 2;

                if (tipo == 0) { // Minimizar: raya horizontal
                    g2.drawLine(cx - 5, cy + 3, cx + 5, cy + 3);
                } else if (tipo == 1) { // Maximizar: cuadrado
                    g2.drawRect(cx - 5, cy - 5, 10, 10);
                } else if (tipo == 2) { // Cerrar: cruz X
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                }

                g2.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(36, 28));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(accion);
        return btn;
    }

    // =========================================================================
    // 2. BARRA DE TABS ("Contactos" / "Estadísticas")
    // =========================================================================
    private JPanel construirBarraTabs() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barra.setBackground(COLOR_TAB_BAR);
        barra.setBorder(new EmptyBorder(4, 16, 0, 16));

        btnTabContactos.addActionListener(e -> activarTab("contactos"));
        btnTabEstadisticas.addActionListener(e -> activarTab("estadisticas"));

        barra.add(btnTabContactos);
        barra.add(btnTabEstadisticas);

        return barra;
    }

    private void activarTab(String nombre) {
        cardLayout.show(panelContenedorCards, nombre);
        boolean esContactos = "contactos".equals(nombre);
        btnTabContactos.setActivo(esContactos);
        btnTabEstadisticas.setActivo(!esContactos);

        if (!esContactos) {
            actualizarEstadisticas();
        }
    }

    // =========================================================================
    // 3. PANEL DE CONTACTOS (Formulario + 6 Botones + Tabla)
    // =========================================================================
    private JPanel construirPanelContactos() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(new EmptyBorder(12, 16, 8, 16));

        // Sección Superior: Formulario a la izquierda | 6 Botones a la derecha
        JPanel superior = new JPanel(new BorderLayout(15, 0));
        superior.setOpaque(false);

        // Izquierda: Avatar grande + 3 campos
        superior.add(construirFormulario(), BorderLayout.CENTER);

        // Separador vertical fino
        JSeparator separador = new JSeparator(SwingConstants.VERTICAL);
        separador.setForeground(new Color(196, 216, 234));
        separador.setBackground(new Color(196, 216, 234));
        separador.setPreferredSize(new Dimension(1, 100));
        superior.add(separador, BorderLayout.EAST);

        // Derecha: Los 6 botones en cuadrícula 2x3
        JPanel contenedorBotones = new JPanel(new BorderLayout());
        contenedorBotones.setOpaque(false);
        contenedorBotones.setBorder(new EmptyBorder(0, 15, 0, 0));
        contenedorBotones.add(construirGridBotones(), BorderLayout.CENTER);

        JPanel panelSuperiorCompleto = new JPanel(new BorderLayout());
        panelSuperiorCompleto.setOpaque(false);
        panelSuperiorCompleto.add(superior, BorderLayout.CENTER);
        panelSuperiorCompleto.add(contenedorBotones, BorderLayout.EAST);

        panel.add(panelSuperiorCompleto, BorderLayout.NORTH);

        // Centro: Tabla estilizada
        panel.add(construirSeccionTabla(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);

        // 1. Avatar circular a la izquierda
        lblAvatar.setIcon(IconoUtil.getIcono("avatar.png", 64, 64));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(lblAvatar, gbc);

        // 2. Campos de entrada
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // Fila 0: Nombre
        gbc.gridx = 1;
        gbc.gridy = 0;
        lblNombre.setFont(FuenteUtil.cargarFuente(13f));
        panel.add(lblNombre, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(txtNombre, gbc);

        // Fila 1: Teléfono
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        lblTelefono.setFont(FuenteUtil.cargarFuente(13f));
        panel.add(lblTelefono, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(txtTelefono, gbc);

        // Fila 2: Email
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        lblEmail.setFont(FuenteUtil.cargarFuente(13f));
        panel.add(lblEmail, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(txtEmail, gbc);

        return panel;
    }

    private JPanel construirGridBotones() {
        JPanel grid = new JPanel(new GridLayout(2, 3, 10, 10));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(380, 110));

        // Fila 1: Icono arriba, texto abajo
        btnAgregar = new BotonPersonalizado(
                GestorIdioma.get("btn.add"),
                IconoUtil.getIcono("agregar.png", 24, 24),
                COLOR_BTN_AGREGAR,
                true
        );

        btnModificar = new BotonPersonalizado(
                GestorIdioma.get("btn.modify"),
                IconoUtil.getIcono("modificar.png", 24, 24),
                COLOR_BTN_MODIFICAR,
                true
        );

        btnEliminar = new BotonPersonalizado(
                GestorIdioma.get("btn.delete"),
                IconoUtil.getIcono("eliminar.png", 24, 24),
                COLOR_BTN_ELIMINAR,
                true
        );

        // Fila 2: Icono a la izquierda, texto a la derecha
        btnBuscar = new BotonPersonalizado(
                GestorIdioma.get("btn.search"),
                IconoUtil.getIcono("buscar.png", 18, 18),
                COLOR_BTN_BUSCAR,
                false
        );

        btnExportar = new BotonPersonalizado(
                GestorIdioma.get("btn.export"),
                IconoUtil.getIcono("exportar.png", 18, 18),
                COLOR_BTN_EXPORTAR,
                false
        );

        btnIdiomas = new BotonPersonalizado(
                GestorIdioma.get("btn.languages"),
                IconoUtil.getIcono("idiomas.png", 18, 18),
                COLOR_BTN_IDIOMAS,
                false
        );

        // Eventos
        btnAgregar.addActionListener(e -> {
            if (filaModeloEditando >= 0) {
                guardarCambiosContacto();
            } else {
                agregarContacto();
            }
        });
        btnModificar.addActionListener(e -> iniciarModificacion());
        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        btnBuscar.addActionListener(e -> txtBuscar.requestFocusInWindow());
        btnExportar.addActionListener(e -> exportarCSV());
        btnIdiomas.addActionListener(e -> mostrarMenuIdiomas(btnIdiomas));

        grid.add(btnAgregar);
        grid.add(btnModificar);
        grid.add(btnEliminar);
        grid.add(btnBuscar);
        grid.add(btnExportar);
        grid.add(btnIdiomas);

        return grid;
    }

    private JPanel construirSeccionTabla() {
        modeloTabla = new DefaultTableModel(
                new Object[]{
                        GestorIdioma.get("table.name"),
                        GestorIdioma.get("table.phone"),
                        GestorIdioma.get("table.email")
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla.setModel(modeloTabla);
        sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(28);
        tabla.setGridColor(new Color(224, 235, 245));
        tabla.setShowGrid(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        // Encabezado estilizado azul marino (#0b3a62) con texto blanco
        tabla.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                l.setBackground(COLOR_HEADER);
                l.setForeground(Color.WHITE);
                l.setFont(FuenteUtil.cargarFuenteBold(13f));
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(24, 75, 119)),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        tabla.getTableHeader().setPreferredSize(new Dimension(tabla.getWidth(), 34));

        // Celdas estilizadas
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                l.setFont(FuenteUtil.cargarFuente(13f));
                l.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
                if (isSelected) {
                    l.setBackground(new Color(203, 227, 247));
                    l.setForeground(new Color(15, 30, 45));
                } else {
                    l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 251, 254));
                    l.setForeground(new Color(30, 30, 30));
                }
                return l;
            }
        });

        // Menú contextual al hacer clic derecho
        JPopupMenu menuContextual = new JPopupMenu();
        JMenuItem itemModificar = new JMenuItem(GestorIdioma.get("btn.modify"));
        JMenuItem itemEliminar = new JMenuItem(GestorIdioma.get("btn.delete"));
        itemModificar.setFont(FuenteUtil.cargarFuente(12f));
        itemEliminar.setFont(FuenteUtil.cargarFuente(12f));
        menuContextual.add(itemModificar);
        menuContextual.add(itemEliminar);

        itemModificar.addActionListener(e -> iniciarModificacion());
        itemEliminar.addActionListener(e -> eliminarSeleccionado());

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int fila = tabla.rowAtPoint(e.getPoint());
                    if (fila >= 0) {
                        tabla.setRowSelectionInterval(fila, fila);
                        menuContextual.show(tabla, e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new LineBorder(new Color(196, 216, 234), 1));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setOpaque(false);
        panelTabla.add(scroll, BorderLayout.CENTER);

        return panelTabla;
    }

    // =========================================================================
    // 4. BARRA INFERIOR (Buscar + Estado con Check Verde)
    // =========================================================================
    private JPanel construirBarraInferior() {
        JPanel pie = new JPanel(new BorderLayout(15, 0));
        pie.setBackground(COLOR_FONDO);
        pie.setBorder(new EmptyBorder(8, 16, 12, 16));

        // Izquierda: "Buscar:" + Caja de texto
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelBuscar.setOpaque(false);

        lblBuscarTexto.setFont(FuenteUtil.cargarFuente(13f));
        lblBuscarTexto.setForeground(new Color(20, 35, 50));
        panelBuscar.add(lblBuscarTexto);
        panelBuscar.add(txtBuscar);

        // Unidad 3 – Búsqueda en segundo plano con Thread + ExecutorService
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            private void buscarEnSegundoPlano() {
                String texto = txtBuscar.getText().trim();

                if (texto.isEmpty()) {
                    // Sin texto: restaurar tabla completa en el EDT directamente
                    sorter.setRowFilter(null);
                    lblEstado.setText(GestorIdioma.get("status.ready"));
                    return;
                }

                lblEstado.setText("Buscando…");

                // Opción A – Thread independiente (mismo enfoque que ValidacionContactoTask)
                new BusquedaContactoTask(dao, texto, resultados -> {
                    modeloTabla.setRowCount(0);
                    resultados.forEach(c -> modeloTabla.addRow(
                            new Object[]{c.getNombre(), c.getTelefono(), c.getEmail()}));
                    lblEstado.setText("Resultados: " + resultados.size());
                }).ejecutar();

                // Opción B – ExecutorService (cancela búsquedas anteriores si el usuario escribe rápido)
                BusquedaExecutorTask.buscar(dao, texto, resultados -> {
                    modeloTabla.setRowCount(0);
                    resultados.forEach(c -> modeloTabla.addRow(
                            new Object[]{c.getNombre(), c.getTelefono(), c.getEmail()}));
                    lblEstado.setText("Resultados: " + resultados.size());
                });
            }
            @Override public void insertUpdate(DocumentEvent e)  { buscarEnSegundoPlano(); }
            @Override public void removeUpdate(DocumentEvent e)   { buscarEnSegundoPlano(); }
            @Override public void changedUpdate(DocumentEvent e)  { buscarEnSegundoPlano(); }
        });

        pie.add(panelBuscar, BorderLayout.WEST);

        // Derecha: Checkmark verde + "Listo."
        JPanel panelEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panelEstado.setOpaque(false);

        lblIconoEstado.setIcon(IconoUtil.getIcono("listo.png", 18, 18));
        lblEstado.setFont(FuenteUtil.cargarFuente(13f));
        lblEstado.setForeground(new Color(30, 45, 60));

        panelEstado.add(lblIconoEstado);
        panelEstado.add(lblEstado);

        pie.add(panelEstado, BorderLayout.EAST);

        return pie;
    }

    /** El hilo de notificaciones pinta un aviso y lo retira solo. */
    private void iniciarNotificaciones() {
        panelAviso.setBackground(COLOR_HEADER);
        panelAviso.setBorder(new EmptyBorder(8, 16, 8, 16));
        panelAviso.setVisible(false);
        lblAviso.setFont(FuenteUtil.cargarFuente(13f));
        lblAviso.setForeground(Color.WHITE);
        panelAviso.add(lblAviso);

        JComponent cristal = new JComponent() {
            @Override public boolean contains(int x, int y) { return false; }
        };
        cristal.setLayout(new BorderLayout());
        cristal.add(panelAviso, BorderLayout.SOUTH);
        setGlassPane(cristal);
        cristal.setVisible(true);
        ocultarAviso.setRepeats(false);

        NotificacionTask.iniciar(mensaje -> {
            lblAviso.setText(mensaje);
            panelAviso.setVisible(true);
            cristal.revalidate();
            cristal.repaint();
            ocultarAviso.restart();
        });
    }

    private void avisar(String mensaje) {
        lblEstado.setText(mensaje);
        NotificacionTask.publicar(mensaje);
    }

    // =========================================================================
    // 5. GESTIÓN MULTI-IDIOMA
    // =========================================================================
    private void mostrarMenuIdiomas(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(new LineBorder(new Color(180, 205, 230), 1));

        String idiomaActual = GestorIdioma.getIdiomaActual();
        for (String idioma : GestorIdioma.getIdiomasDisponibles()) {
            boolean esActivo = idioma.equals(idiomaActual);
            String texto = (esActivo ? "✓ " : "   ") + idioma;
            JMenuItem item = new JMenuItem(texto);
            item.setFont(FuenteUtil.cargarFuente(13f));
            item.setBackground(Color.WHITE);
            item.setForeground(esActivo ? COLOR_BTN_MODIFICAR : new Color(30, 30, 30));
            item.addActionListener(e -> GestorIdioma.setIdioma(idioma));
            menu.add(item);
        }

        menu.show(invoker, 0, invoker.getHeight() + 2);
    }

    @Override
    public void idiomaCambiado(String nuevoIdioma) {
        // Título de ventana y selector superior
        setTitle(GestorIdioma.get("app.title"));
        lblTituloVentana.setText(GestorIdioma.get("app.title"));
        lblIdiomaHeader.setText(nuevoIdioma);

        // Tabs
        btnTabContactos.setText(GestorIdioma.get("tab.contacts"));
        btnTabEstadisticas.setText(GestorIdioma.get("tab.stats"));

        // Formulario
        lblNombre.setText(GestorIdioma.get("form.name"));
        lblTelefono.setText(GestorIdioma.get("form.phone"));
        lblEmail.setText(GestorIdioma.get("form.email"));

        // Botones
        if (filaModeloEditando >= 0) {
            btnAgregar.setText(GestorIdioma.get("btn.save"));
        } else {
            btnAgregar.setText(GestorIdioma.get("btn.add"));
        }
        btnModificar.setText(GestorIdioma.get("btn.modify"));
        btnEliminar.setText(GestorIdioma.get("btn.delete"));
        btnBuscar.setText(GestorIdioma.get("btn.search"));
        btnExportar.setText(GestorIdioma.get("btn.export"));
        btnIdiomas.setText(GestorIdioma.get("btn.languages"));

        // Columnas de la tabla
        String[] columnas = new String[]{
                GestorIdioma.get("table.name"),
                GestorIdioma.get("table.phone"),
                GestorIdioma.get("table.email")
        };
        for (int i = 0; i < columnas.length; i++) {
            tabla.getColumnModel().getColumn(i).setHeaderValue(columnas[i]);
        }
        tabla.getTableHeader().repaint();

        // Barra inferior
        lblBuscarTexto.setText(GestorIdioma.get("footer.search"));
        if (filaModeloEditando < 0) {
            lblEstado.setText(GestorIdioma.get("status.ready"));
        }

        // Estadísticas
        panelEstadisticas.refrescarTextos();

        repaint();
    }

    // =========================================================================
    // 6. ACCIONES DEL CONTROLADOR
    // =========================================================================
    // Unidad 3 – Valida duplicados en segundo plano antes de guardar
    private void agregarContacto() {
        String nombre   = txtNombre.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email    = txtEmail.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.name_required"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            txtNombre.requestFocusInWindow();
            return;
        }
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.email_invalid"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocusInWindow();
            return;
        }

        Contacto candidato = new Contacto(nombre, telefono, email);
        btnAgregar.setEnabled(false);
        lblEstado.setText("Validando…");

        new ValidacionContactoTask(dao, candidato,
            msg -> {                                       // onDuplicado
                btnAgregar.setEnabled(true);
                lblEstado.setText(GestorIdioma.get("status.ready"));
                JOptionPane.showMessageDialog(this, msg,
                        GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            },
            () -> {                                        // onValido
                modeloTabla.addRow(new Object[]{candidato.getNombre(), candidato.getTelefono(), candidato.getEmail()});
                limpiarFormulario();
                actualizarEstadisticas();
                btnAgregar.setEnabled(true);
                avisar(GestorIdioma.get("status.added"));
            }
        ).ejecutar();
    }

    private Contacto contactoEnFila(int filaModelo) {
        return new Contacto(
                String.valueOf(modeloTabla.getValueAt(filaModelo, 0)),
                String.valueOf(modeloTabla.getValueAt(filaModelo, 1)),
                String.valueOf(modeloTabla.getValueAt(filaModelo, 2)));
    }

    private void iniciarModificacion() {
        if (bloqueoEdicion != null) return;
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this,
                    GestorIdioma.get("val.select_modify"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        ContactoDAO.BloqueoEdicion bloqueo = dao.bloquearEdicion(contactoEnFila(filaModelo));
        if (bloqueo == null) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.contact_locked"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        bloqueoEdicion = bloqueo;
        filaModeloEditando = filaModelo;
        Contacto contacto = bloqueo.getContacto();
        txtNombre.setText(contacto.getNombre());
        txtTelefono.setText(contacto.getTelefono());
        txtEmail.setText(contacto.getEmail());

        btnAgregar.setText(GestorIdioma.get("btn.save"));
        btnModificar.setEnabled(false);
        lblEstado.setText(GestorIdioma.get("status.editing"));
        txtNombre.requestFocusInWindow();
    }

    private void guardarCambiosContacto() {
        if (filaModeloEditando < 0 || filaModeloEditando >= modeloTabla.getRowCount()
                || bloqueoEdicion == null) {
            cancelarModificacion();
            return;
        }

        String nombre = txtNombre.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.name_required"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            txtNombre.requestFocusInWindow();
            return;
        }
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.email_invalid"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocusInWindow();
            return;
        }

        Contacto actualizado = new Contacto(nombre, telefono, email);
        if (!dao.modificar(bloqueoEdicion, actualizado)) {
            JOptionPane.showMessageDialog(this, "Error al actualizar contacto.",
                    GestorIdioma.get("val.error_title"), JOptionPane.ERROR_MESSAGE);
            cancelarModificacion();
            return;
        }

        modeloTabla.setValueAt(actualizado.getNombre(), filaModeloEditando, 0);
        modeloTabla.setValueAt(actualizado.getTelefono(), filaModeloEditando, 1);
        modeloTabla.setValueAt(actualizado.getEmail(), filaModeloEditando, 2);

        cancelarModificacion();
        actualizarEstadisticas();
        avisar(GestorIdioma.get("status.modified"));
    }

    private void cancelarModificacion() {
        dao.desbloquearEdicion(bloqueoEdicion);
        bloqueoEdicion = null;
        filaModeloEditando = -1;
        btnAgregar.setText(GestorIdioma.get("btn.add"));
        btnModificar.setEnabled(true);
        limpiarFormulario();
        lblEstado.setText(GestorIdioma.get("status.ready"));
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtNombre.requestFocusInWindow();
    }

    private void eliminarSeleccionado() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.select_delete"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        String nombre = String.valueOf(modeloTabla.getValueAt(filaModelo, 0));

        int confirmar = JOptionPane.showConfirmDialog(this,
                GestorIdioma.get("val.confirm_delete", nombre),
                GestorIdioma.get("val.confirm_title"),
                JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        if (filaModeloEditando == filaModelo) cancelarModificacion();
        if (!dao.eliminar(contactoEnFila(filaModelo))) {
            JOptionPane.showMessageDialog(this, GestorIdioma.get("val.contact_locked"),
                    GestorIdioma.get("val.validation_title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        modeloTabla.removeRow(filaModelo);
        if (filaModeloEditando >= 0) {
            cancelarModificacion();
        }
        actualizarEstadisticas();
        avisar(GestorIdioma.get("status.deleted"));
    }

    private void exportarCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("contactos.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File archivo = chooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
        }
        Path destino = archivo.toPath();
        avisar(GestorIdioma.get("status.exporting"));

        ExportadorCSV.exportarEnSegundoPlano(dao.listar(), destino,
                () -> {
                    avisar(GestorIdioma.get("status.exported"));
                    JOptionPane.showMessageDialog(this,
                            GestorIdioma.get("val.export_success", destino.toAbsolutePath().toString()),
                            GestorIdioma.get("val.success_title"),
                            JOptionPane.INFORMATION_MESSAGE);
                },
                error -> {
                    avisar(GestorIdioma.get("status.export_error"));
                    JOptionPane.showMessageDialog(this, error.getMessage(),
                            GestorIdioma.get("val.error_title"),
                            JOptionPane.ERROR_MESSAGE);
                });
    }

    private void actualizarEstadisticas() {
        List<Contacto> contactos = dao.listar();
        int conEmail = 0;
        String ultimo = null;
        for (Contacto c : contactos) {
            if (c.getEmail() != null && !c.getEmail().trim().isEmpty()) {
                conEmail++;
            }
            ultimo = c.getNombre();
        }
        panelEstadisticas.actualizar(contactos.size(), conEmail, contactos.size() - conEmail, ultimo);
    }

    // =========================================================================
    // SUBCOMPONENTES INTERNOS ESTILIZADOS
    // =========================================================================

    /**
     * Botón de Pestaña (Tab) redondeado idéntico al diseño de la imagen.
     */
    private static class BotonTab extends JButton {
        private final String iconoActivo;
        private final String iconoInactivo;
        private boolean activo;

        public BotonTab(String texto, String iconoActivo, String iconoInactivo, boolean activo) {
            super(texto);
            this.iconoActivo = iconoActivo;
            this.iconoInactivo = iconoInactivo;
            this.activo = activo;

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(FuenteUtil.cargarFuenteBold(13f));
            setIconTextGap(8);
            setPreferredSize(new Dimension(135, 34));

            actualizarEstadoVisual();
        }

        public void setActivo(boolean a) {
            this.activo = a;
            actualizarEstadoVisual();
            repaint();
        }

        private void actualizarEstadoVisual() {
            setForeground(activo ? Color.WHITE : new Color(30, 45, 60));
            setIcon(IconoUtil.getIcono(activo ? iconoActivo : iconoInactivo, 18, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (activo) {
                g2.setColor(new Color(0, 120, 215)); // Azul brillante
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(250, 252, 255));
            } else {
                g2.setColor(new Color(242, 247, 252));
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Botón en forma de pastilla (Pill) para el selector de idioma en la cabecera.
     */
    private static class BotonPillIdioma extends JButton {
        public BotonPillIdioma() {
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(118, 28));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color colorFondo;
            if (getModel().isPressed()) {
                colorFondo = new Color(15, 60, 100);
            } else if (getModel().isRollover()) {
                colorFondo = new Color(30, 95, 150);
            } else {
                colorFondo = new Color(22, 77, 124);
            }

            g2.setColor(colorFondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            // Flecha blanca hacia abajo ▼ a la derecha
            g2.setColor(Color.WHITE);
            int ax = getWidth() - 14;
            int ay = getHeight() / 2 - 2;
            Path2D arrow = new Path2D.Float();
            arrow.moveTo(ax, ay);
            arrow.lineTo(ax + 7, ay);
            arrow.lineTo(ax + 3.5f, ay + 4);
            arrow.closePath();
            g2.fill(arrow);

            g2.dispose();

            super.paintComponent(g);
        }
    }
}

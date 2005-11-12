package org.electrocodeogram.ui.modules;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Observable;

import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import org.electrocodeogram.modulepackage.ModuleType;
import org.electrocodeogram.misc.constants.UIConstants;
import org.electrocodeogram.module.Module;
import org.electrocodeogram.system.ISystem;
import org.electrocodeogram.ui.Gui;
import org.electrocodeogram.ui.IGui;
import org.electrocodeogram.ui.MenuManager;
import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.DefaultGraphModel;

public class ModuleGraph extends JGraph {

    private static final int DEFAULT_X_DISTANCE = 100;

    private static final int DEFAULT_MARGIN = 10;

    Gui _gui;

    private HashMap < Integer, ModuleCell > _cellMap;

    private static int _selectedBefore = -1;

    private static int _selected = -1;

    ModuleGraphObserverDummy _observerDummy = null;

    private int _margin;

    private int _dinstanceX;

    public ModuleGraph(Gui configurator) {
        super(new DefaultGraphModel());

        this._margin = DEFAULT_MARGIN;

        this._dinstanceX = DEFAULT_X_DISTANCE;

        this._gui = configurator;

        this._observerDummy = new ModuleGraphObserverDummy(configurator, this);

        this._cellMap = new HashMap < Integer, ModuleCell >();

        this.setSizeable(true);

        this.setAutoscrolls(true);

        this.setBackground(UIConstants.MGR_BACKGROUND_COLOR);

        this.setBorder(new LineBorder(UIConstants.MGR_BORDER_COLOR,
            UIConstants.MGR_BORDER_WIDTH, true));

        addGraphSelectionListener(new GraphSelectionListener() {

            public void valueChanged(GraphSelectionEvent arg0) {
                if (arg0.isAddedCell()
                    && (arg0.getCell() instanceof ModuleCell)) {

                    ModuleGraph._selected = ((ModuleCell) (arg0.getCell()))
                        .getId();

                    // ModuleGraph.this._cellMap.get(new
                    // Integer(ModuleGraph.this._selected)).select();

                    ModuleGraph.this._gui.enableModuleMenu(true);
                } else {
                    ModuleGraph._selectedBefore = ModuleGraph.this._selected;

                    // ModuleGraph.this._cellMap.get(new
                    // Integer(ModuleGraph.this._selectedBefore)).deselect();

                    ModuleGraph._selected = -1;

                    ModuleGraph.this._gui.enableModuleMenu(false);
                }

            }
        });

        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                ISystem systemRoot = org.electrocodeogram.system.System
                    .getInstance();

                IGui gui = systemRoot.getGui();

                boolean mode = gui.getModuleConnectionMode();

                if (mode) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        Object o = getFirstCellForLocation(e.getPoint().x, e
                            .getPoint().y);
                        if (o != null) {
                            if (o instanceof ModuleCell) {
                                ModuleCell mc = (ModuleCell) o;

                                _selected = mc.getId();

                                if (_selected == org.electrocodeogram.system.System
                                    .getInstance().getGui().getSourceModule()) {
                                    JOptionPane
                                        .showMessageDialog(
                                            getGui(),
                                            "You can not connect a module to itself.",
                                            "Connect Module",
                                            JOptionPane.ERROR_MESSAGE);
                                } else {
                                    try {

                                        org.electrocodeogram.system.System
                                            .getInstance()
                                            .getModuleRegistry()
                                            .getModule(
                                                org.electrocodeogram.system.System
                                                    .getInstance().getGui()
                                                    .getSourceModule())
                                            .connectModule(
                                                org.electrocodeogram.system.System
                                                    .getInstance()
                                                    .getModuleRegistry()
                                                    .getModule(_selected));

                                        org.electrocodeogram.system.System
                                            .getInstance().getGui()
                                            .exitModuleConnectionMode();
                                    } catch (Exception e1) {

                                        JOptionPane.showMessageDialog(getGui(),
                                            e1.getMessage(), "Connect Module",
                                            JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            }

                        }
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        org.electrocodeogram.system.System.getInstance()
                            .getGui().exitModuleConnectionMode();
                    }

                } else {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        Object o = getFirstCellForLocation(e.getPoint().x, e
                            .getPoint().y);
                        if (o != null) {
                            if (o instanceof ModuleCell) {
                                ModuleCell mc = (ModuleCell) o;

                                _selected = mc.getId();

                                MenuManager.showModuleMenu(_selected,
                                    ModuleGraph.this, e.getPoint().x, e
                                        .getPoint().y);

                            } else if (o instanceof ModuleEdge) {
                                ModuleEdge edge = (ModuleEdge) o;

                                MenuManager.showEdgeMenu(edge.getParentId(),
                                    edge.getChildId(), ModuleGraph.this, e
                                        .getPoint().x, e.getPoint().y);
                            }

                        }
                    }
                }
            }

        });
    }

    private Gui getGui() {
        return this._gui;
    }

    private void addModuleCell(ModuleCell cell) {
        this._cellMap.put(new Integer(cell.getId()), cell);

        this.getGraphLayoutCache().insert(cell);
    }

    public void updateModuleCell(int id, Module module) {
        if (containsModuleCell(id)) {

            ModuleCell moduleCell = this._cellMap.get(new Integer(id));

            if (module.getState()) {
                moduleCell.activate();
            } else {
                moduleCell.deactivate();
            }

            this.getGraphLayoutCache().insert(moduleCell);

            Module[] modules = module.getReceivingModules();

            Object[] edges = moduleCell.getEdges();

            if (edges.length > 0) {
                getGraphLayoutCache().remove(edges);
            }

            if (modules != null) {
                for (Module receivingModule : modules) {
                    ModuleCell childModuleCell = (ModuleCell) this._cellMap
                        .get(new Integer(receivingModule.getId()));

                    ModuleEdge edge = new ModuleEdge(moduleCell.getId(),
                        childModuleCell.getId());

                    edge.setSource(moduleCell.getChildAt(0));

                    edge.setTarget(childModuleCell.getChildAt(0));

                    addChildEdge(moduleCell, edge);

                }
            }
        }
    }

    public boolean containsModuleCell(int id) {
        boolean flag = _cellMap.containsKey(new Integer(id));

        return flag;
    }

    public void removeModuleCell(int id) {
        if (containsModuleCell(id)) {
            ModuleCell cell = (ModuleCell) _cellMap.get(new Integer(id));

            Object[] o = new Object[] {cell};

            Object[] edges = cell.getEdges();

            if (edges.length > 0) {
                getGraphLayoutCache().remove(edges);
            }

            getGraphLayoutCache().remove(o);

            _cellMap.remove(new Integer(id));
        }
    }

    /**
     * @param moduleCell
     * @param edge
     * @param childModuleCell
     * @param parentModuleCell
     */
    public void addChildEdge(ModuleCell moduleCell, ModuleEdge edge) {

        moduleCell.addEdge(edge);

        this.getGraphLayoutCache().insert(edge);

    }

    private static class ModuleGraphObserverDummy extends Observable {

        private ModuleGraph parent = null;

        public ModuleGraphObserverDummy(Gui configurator, ModuleGraph parent) {
            super();

            this.parent = parent;

            this.addObserver(configurator);

        }

        public void notifyUI(int moduleId) {
            setChanged();

            notifyObservers(moduleId);

            clearChanged();
        }
    }

    /**
     * @param moduleType
     * @param id
     * @param name
     * @param b
     */
    public void createModuleCell(ModuleType moduleType, int id, String name,
        boolean b) {
        ModuleCell cell = new ModuleCell(moduleType, id, name, b);

        int x = this._margin;

        int y = this._margin;

        switch (moduleType) {
            case SOURCE_MODULE:

                x = this.getX() + this._margin;

                break;

            case INTERMEDIATE_MODULE:

                x = (this.getWidth() / 2)
                    - (int) (cell.getBounds().getWidth() / 2);

                break;

            case TARGET_MODULE:

                x = this.getWidth() - (int) (cell.getBounds().getWidth())
                    - this._margin;

                break;

            default:

                break;

        }

        Object o = getFirstCellForLocation(x, y);

        while (o != null) {
            if (o instanceof ModuleCell) {
                y += this._dinstanceX;
            } else {
                break;
            }

            o = getFirstCellForLocation(x, y);
        }

        cell.setLocation(new Point(x, y));

        addModuleCell(cell);

    }

    public void setMargin(int margin) {
        this._margin = margin;
    }

    public static int getSelectedModule() {
        return _selected;
    }

    /**
     * @param id
     */
    public void highlight(int id) {
        ModuleCell cell = this._cellMap.get(new Integer(id));

        if (cell == null) {
            return;
        }

        cell.eventReceived();

        this.getGraphLayoutCache().insert(cell);
    }

}

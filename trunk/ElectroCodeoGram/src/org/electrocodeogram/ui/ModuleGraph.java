/*
 * Created on 10.03.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.electrocodeogram.ui;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.border.LineBorder;

import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;

/**
 * @author 7oas7er
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ModuleGraph extends JGraph
{

    private int selectedModuleCellId = -1;
    
    private ModuleCell rootCell = null;
    
    
    
    private ModuleGraph me = null;
    
    private Configurator root = null;
    
    public Configurator getRoot()
    {
        return root;
    }
    
    public ModuleGraph(Configurator root){
        
        super(new DefaultGraphModel());
      
        this.root = root;
        
        me = this;
   
        
        
        
        addGraphSelectionListener(new GraphSelectionListener() {

            public void valueChanged(GraphSelectionEvent arg0)
            {
                if(arg0.isAddedCell() && (arg0.getCell() instanceof ModuleCell))
                {   
                    
                    selectedModuleCellId  = ((ModuleCell)(arg0.getCell())).getId();
                    
                }
                else
                {
                    selectedModuleCellId = -1;
                }
                
            }});
           
        this.setBorder(new LineBorder(Color.GRAY));
        
        addMouseListener(new MouseAdapter(){

            public void mouseClicked(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON3)
                {
	                Object o = getFirstCellForLocation(e.getPoint().x,e.getPoint().y);
	                if(o != null)
	                {
	                    ModuleCell mc = (ModuleCell) o;
	                    
	                    selectedModuleCellId = mc.getId();
	                    
	                    MenuManager.getInstance().showModuleMenu(me,e.getPoint().x,e.getPoint().y);
	                    
	                }
                }
            }

            });
		
    }

    /**
     * 
     * @uml.property name="selectedModuleCellId"
     */
    public int getSelectedModuleCellId() {
        return selectedModuleCellId;
    }

          
    public void addModuleCell(ModuleCell cell)
    {
        if (rootCell == null)
        {
            rootCell = cell;
        }
        this.getGraphLayoutCache().insert(cell);
    }

    /**
     * @param edge
     */
    public void addEdge(DefaultEdge edge)
    {
        this.getGraphLayoutCache().insert(edge);
        
    }
    

    
}

/*
 * Created on 11.03.2005
 *
 */
package org.electrocodeogram.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.electrocodeogram.core.Core;
import org.electrocodeogram.module.ModuleConnectionException;
import org.electrocodeogram.module.registry.IllegalModuleIDException;
import org.electrocodeogram.module.registry.ModuleInstantiationException;
import org.electrocodeogram.module.registry.ModuleRegistry;
import org.electrocodeogram.module.registry.UnknownModuleIDException;
import org.electrocodeogram.msdt.EventValidator;

/**
 * @author 7oas7er
 */

public class ActionAdapter implements ActionListener
{

    
    private String moduleName = null;
    
    private int moduleClassId = -1;
    
    /**
     * @param configurator
     * @param selectedModuleCellId
     * @param moduleName
     */
    public ActionAdapter(Gui configurator, int moduleClassId, String moduleName)
    {
        
        this.moduleName = moduleName;
        
        this.moduleClassId = moduleClassId;
    }

    public void actionPerformed(ActionEvent e)
    {

//      int selectedModuleCellId = Configurator.getInstance().getSelectedModuleCellId();
//        
//      if(selectedModuleCellId != -1)
//      {
//          try
//          {
//              ModuleRegistry.getInstance().connectNewModuleInstance(selectedModuleCellId,moduleName);
//          }
//          catch(ModuleConnectionException er)
//          {
//              JOptionPane.showMessageDialog(Configurator.getInstance(),er.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);  
//          }
//       
//      }
        
        try {
        	Core.getInstance().getModuleRegistry().createModuleInstanceFromModuleClassId(moduleClassId,moduleName);
        }
        catch (ModuleInstantiationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IllegalModuleIDException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        catch (UnknownModuleIDException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        
    }

}
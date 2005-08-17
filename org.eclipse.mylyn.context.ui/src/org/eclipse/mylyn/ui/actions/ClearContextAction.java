/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.tasklist.ITask;
import org.eclipse.mylar.tasklist.Task;
import org.eclipse.mylar.tasklist.TaskListImages;
import org.eclipse.mylar.tasklist.ui.views.TaskListView;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.internal.Workbench;

/**
 * @author Mik Kersten and Ken Sueda
 */
public class ClearContextAction extends Action implements IViewActionDelegate{

	public static final String ID = "org.eclipse.mylar.tasklist.actions.context.clear";
	
	public ClearContextAction() {
		setText("Clear Task Context");
        setToolTipText("Clear Task Context");
        setId(ID);
        setImageDescriptor(TaskListImages.ERASE_TASKSCAPE);
	}
	
	@Override
	public void run() {
		if(TaskListView.getDefault() == null)
			return;
	    Object selectedObject = ((IStructuredSelection)TaskListView.getDefault().getViewer().getSelection()).getFirstElement();
	    if (selectedObject != null && selectedObject instanceof ITask) {
//	    	ITask task = ((ITask)selectedObject).getOrCreateCorrespondingTask();
//    		if (task.isActive()) {
//	    		MessageDialog.openError(Workbench.getInstance()
//						.getActiveWorkbenchWindow().getShell(), "Clear context failed",
//						"Task must be deactivated before clearing task context.");
//				return;
//	    	}
	    	boolean deleteConfirmed = MessageDialog.openQuestion(
		            Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
		            "Confirm clear context", 
		            "Clear context for the selected task?");
			if (!deleteConfirmed) 
				return;
			
	    	MylarPlugin.getContextManager().contextDeleted(((ITask)selectedObject).getHandle(), ((Task)selectedObject).getPath());
	    	TaskListView.getDefault().getViewer().refresh();
	    }
	    // TODO add this back in
//	    else if (selectedObject != null && selectedObject instanceof BugzillaHit && ((BugzillaHit)selectedObject).hasAssociatedActivatibleTask()) {
//	    	BugzillaTask task = ((BugzillaHit)selectedObject).getAssociatedTask();
//	    	if(task != null){
//	    		if (task.isActive()) {
//		    		MessageDialog.openError(Workbench.getInstance()
//							.getActiveWorkbenchWindow().getShell(), "Clear context failed",
//							"Task must be deactivated before clearing task context.");
//					return;
//		    	}
//		    	boolean deleteConfirmed = MessageDialog.openQuestion(
//			            Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
//			            "Confirm clear context", 
//			            "Clear context for the selected task?");
//				if (!deleteConfirmed) 
//					return;
//	    		MylarPlugin.getContextManager().taskDeleted(task.getHandle(), task.getPath());
//	    	}
//	    	TaskListView.getDefault().getViewer().refresh();
//	    }
	}

	public void init(IViewPart view) {
		
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		
	}
}
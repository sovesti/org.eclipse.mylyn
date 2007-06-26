/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.team.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
 * @author Mik Kersten
 */
public class TaskResourceMappingActionProvider extends CommonActionProvider {

	private OpenCorrespondingTaskAction openCorrespondingAction = new OpenCorrespondingTaskAction();

	private AddToTaskContextAction addToTaskContextAction = new AddToTaskContextAction();

	@Override
	public void fillContextMenu(IMenuManager menuManager) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		openCorrespondingAction.selectionChanged(openCorrespondingAction, selection);
		addToTaskContextAction.selectionChanged(addToTaskContextAction, selection);

		menuManager.insertAfter(ICommonMenuConstants.GROUP_ADDITIONS, openCorrespondingAction);
		menuManager.insertAfter(ICommonMenuConstants.GROUP_ADDITIONS, addToTaskContextAction);
	}

}

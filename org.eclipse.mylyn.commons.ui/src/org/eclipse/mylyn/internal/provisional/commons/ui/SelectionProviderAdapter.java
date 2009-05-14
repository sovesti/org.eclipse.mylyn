/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.provisional.commons.ui;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Provides an simple implementation of {@link ISelectionProvider} that propagates selection events to registered
 * listeners.
 * 
 * @author Steffen Pingel
 */
public class SelectionProviderAdapter extends EventManager implements ISelectionProvider {

	private ISelection selection;

	/**
	 * Constructs a <code>SelectionProviderAdapter</code> and initializes the selection to <code>selection</code>.
	 * 
	 * @param selection
	 *            the initial selection
	 * @see #setSelection(ISelection)
	 */
	public SelectionProviderAdapter(ISelection selection) {
		setSelection(selection);
	}

	/**
	 * Constructs a <code>SelectionProviderAdapter</code> with a <code>null</code> selection.
	 */
	public SelectionProviderAdapter() {
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		addListenerObject(listener);
	}

	public ISelection getSelection() {
		return selection;
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		removeListenerObject(listener);
	}

	protected void selectionChanged(final SelectionChangedEvent event) {
		Object[] listeners = getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
			SafeRunner.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(event);
				}
			});
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
		selectionChanged(new SelectionChangedEvent(this, selection));
	}

}

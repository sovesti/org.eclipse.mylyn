/*******************************************************************************
 * Copyright (c) 2012 Sebastian Schmidt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Schmidt - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.debug.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Sebastian Schmidt
 */
public class DebugUiPlugin extends AbstractUIPlugin {

	public static String ID_PLUGIN = "org.eclipse.mylyn.debug.ui"; //$NON-NLS-1$

	public static String CONTRIBUTOR_ID = "org.eclipse.mylyn.debug.breakpoints"; //$NON-NLS-1$

	public static final String WORKSPACE_STATE_FILE = "activeContext.xml"; //$NON-NLS-1$

	public static final String CONTENT_TYPE = "breakpoint"; //$NON-NLS-1$

	private static DebugUiPlugin plugin;

	/**
	 * The constructor
	 */
	public DebugUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static DebugUiPlugin getDefault() {
		return plugin;
	}
}

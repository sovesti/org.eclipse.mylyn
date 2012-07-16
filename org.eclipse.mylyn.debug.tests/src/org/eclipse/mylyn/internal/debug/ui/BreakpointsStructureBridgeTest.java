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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.mylyn.context.sdk.java.WorkspaceSetupHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sebastian Schmidt
 */
public class BreakpointsStructureBridgeTest {

	private final BreakpointsStructureBridge objectUnderTest = new BreakpointsStructureBridge();

	private IBreakpoint testBreakpoint;

	@Before
	public void setUp() throws Exception {
		BreakpointsTestUtil.createProject();
		testBreakpoint = BreakpointsTestUtil.createTestBreakpoint();
	}

	@After
	public void tearDown() throws Exception {
		WorkspaceSetupHelper.clearWorkspace();
	}

	@Test
	public void testGetHandleIdentifier() {
		assertNull(objectUnderTest.getHandleIdentifier(new Object()));
		assertEquals(BreakpointsStructureBridge.HANDLE_DEFAULT_BREAKPOINT_MANAGER,
				objectUnderTest.getHandleIdentifier(DebugPlugin.getDefault().getBreakpointManager()));
		assertNotNull(objectUnderTest.getHandleIdentifier(testBreakpoint));
	}

	@Test
	public void testGenerateBreakpointId() {
		assertNull(testBreakpoint.getMarker().getAttribute(BreakpointsStructureBridge.ATTRIBUTE_ID, null));
		objectUnderTest.getHandleIdentifier(testBreakpoint);
		assertNotNull(testBreakpoint.getMarker().getAttribute(BreakpointsStructureBridge.ATTRIBUTE_ID, null));
	}

	@Test
	public void testContentType() {
		assertEquals(DebugUiPlugin.CONTENT_TYPE,
				objectUnderTest.getContentType(BreakpointsStructureBridge.HANDLE_DEFAULT_BREAKPOINT_MANAGER));
		assertEquals(DebugUiPlugin.CONTENT_TYPE,
				objectUnderTest.getContentType(objectUnderTest.getHandleIdentifier(testBreakpoint)));
	}

	@Test
	public void testParentHandle() {
		assertEquals(BreakpointsStructureBridge.HANDLE_DEFAULT_BREAKPOINT_MANAGER,
				objectUnderTest.getParentHandle(objectUnderTest.getHandleIdentifier(testBreakpoint)));
		assertNull(objectUnderTest.getParentHandle("lalalalala")); //$NON-NLS-1$
	}

	@Test
	public void testGetObject() throws CoreException {
		IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		breakpointManager.addBreakpoint(testBreakpoint);
		assertNull(objectUnderTest.getObjectForHandle("lalalala")); //$NON-NLS-1$
		assertEquals(breakpointManager,
				objectUnderTest.getObjectForHandle(BreakpointsStructureBridge.HANDLE_DEFAULT_BREAKPOINT_MANAGER));
		assertEquals(testBreakpoint,
				objectUnderTest.getObjectForHandle(objectUnderTest.getHandleIdentifier(testBreakpoint)));
	}
}

/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Itema AS - Corrected lazy initialisation of fields
 *******************************************************************************/

package org.eclipse.mylyn.internal.builds.ui.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunHandler;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.mylyn.builds.core.IBuild;
import org.eclipse.mylyn.builds.core.IBuildPlan;
import org.eclipse.mylyn.builds.core.ITestCase;
import org.eclipse.mylyn.builds.core.ITestSuite;
import org.eclipse.mylyn.builds.internal.core.operations.OperationChangeEvent;
import org.eclipse.mylyn.builds.internal.core.operations.OperationChangeListener;
import org.eclipse.mylyn.builds.internal.core.operations.RefreshOperation;
import org.eclipse.mylyn.builds.internal.core.util.JUnitResultGenerator;
import org.eclipse.mylyn.internal.builds.ui.BuildsUiInternal;
import org.eclipse.mylyn.internal.builds.ui.BuildsUiPlugin;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonUiUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.ICoreRunnable;
import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.xml.sax.SAXException;

/**
 * @author Steffen Pingel
 * @author Torkild U. Resheim
 */
public class TestResultManager {

	/**
	 * Encapsulates JUnit dependencies to avoid ClassNotFoundException when JUnit is not available.
	 */
	private static class Runner {
		static void showInJUnitViewInternal(final IBuild build) {
			final TestRunSession testRunSession = new TestResultSession(build);
			try {
				CommonUiUtil.busyCursorWhile(new ICoreRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						JUnitResultGenerator generator = new JUnitResultGenerator(build.getTestResult());
						TestRunHandler handler = new TestRunHandler(testRunSession);
						try {
							generator.write(handler);
						} catch (SAXException e) {
							throw new CoreException(new Status(IStatus.ERROR, BuildsUiPlugin.ID_PLUGIN,
									"Unexpected parsing error while preparing test results", e));
						}
					}
				});
			} catch (OperationCanceledException e) {
				return;
			} catch (CoreException e) {
				StatusManager.getManager().handle(
						new Status(IStatus.ERROR, BuildsUiPlugin.ID_PLUGIN,
								"Unexpected error while processing test results", e),
						StatusManager.SHOW | StatusManager.LOG);
				return;
			}

			// show results in view
			WorkbenchUtil.showViewInActiveWindow(TestRunnerViewPart.NAME);
			getJUnitModel().addTestRunSession(testRunSession);
		}
	}

	private static volatile JUnitModel junitModel;

	/**
	 * @see {@link org.eclipse.jdt.internal.junit.ui.OpenTestAction}
	 */
	static IType findType(String className, IProgressMonitor monitor) throws CoreException {
		final IType[] result = { null };
		TypeNameMatchRequestor nameMatchRequestor = new TypeNameMatchRequestor() {
			@Override
			public void acceptTypeNameMatch(TypeNameMatch match) {
				result[0] = match.getType();
			}
		};
		int lastDot = className.lastIndexOf('.');
		char[] packageName = lastDot >= 0 ? className.substring(0, lastDot).toCharArray() : null;
		char[] typeName = (lastDot >= 0 ? className.substring(lastDot + 1) : className).toCharArray();
		SearchEngine engine = new SearchEngine();
		engine.searchAllTypeNames(packageName, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, typeName,
				SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, IJavaSearchConstants.TYPE,
				SearchEngine.createWorkspaceScope(), nameMatchRequestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
		return result[0];
	}

	static JUnitModel getJUnitModel() {
		if (junitModel == null) {
			try {
				// Eclipse 3.6 or later
				Class<?> clazz;
				try {
					clazz = Class.forName("org.eclipse.jdt.internal.junit.JUnitCorePlugin");
				} catch (ClassNotFoundException e) {
					// Eclipse 3.5 and earlier
					clazz = Class.forName("org.eclipse.jdt.internal.junit.ui.JUnitPlugin");
				}

				Method method = clazz.getDeclaredMethod("getModel");
				junitModel = (JUnitModel) method.invoke(null);
			} catch (Exception e) {
				NoClassDefFoundError error = new NoClassDefFoundError("Unable to locate container for JUnitModel");
				error.initCause(e);
				throw error;
			}
		}
		return junitModel;
	}

	public static boolean isJUnitAvailable() {
		return Platform.getBundle("org.eclipse.jdt.junit.core") != null; //$NON-NLS-1$
	}

	public static void openInEditor(ITestCase testCase) {
		openInEditor(testCase.getClassName(), testCase.getLabel());
	}

	public static void openInEditor(ITestSuite suite) {
		openInEditor(suite.getLabel(), null);
	}

	public static void openInEditor(final String className, final String testName) {
		final AtomicReference<IJavaElement> result = new AtomicReference<IJavaElement>();
		try {
			CommonUiUtil.busyCursorWhile(new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IType type = findType(className, monitor);
					if (type == null) {
						return;
					}
					result.set(type);
					if (testName != null) {
						IMethod method = type.getMethod(testName, new String[0]);
						if (method != null && method.exists()) {
							result.set(method);
						}
					}
				}
			});
			if (result.get() == null) {
				StatusManager.getManager().handle(
						new Status(IStatus.ERROR, BuildsUiPlugin.ID_PLUGIN, "Failed to locate test in workspace."),
						StatusManager.SHOW | StatusManager.BLOCK);
				return;
			}
			JavaUI.openInEditor(result.get(), true, true);
		} catch (OperationCanceledException e) {
			return;
		} catch (Exception e) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, BuildsUiPlugin.ID_PLUGIN, "Failed to locate test in workspace.", e),
					StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			return;
		}
	}

	public static void showInJUnitView(final IBuild build) {
		Assert.isNotNull(build);

		if (!isJUnitAvailable()) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, BuildsUiPlugin.ID_PLUGIN, "JUnit is not installed."),
					StatusManager.SHOW | StatusManager.BLOCK);
			return;
		}

		if (build.getTestResult() == null) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, BuildsUiPlugin.ID_PLUGIN, "The build did not produce test results."),
					StatusManager.SHOW | StatusManager.BLOCK);
			return;
		}

		// invoke separate method to avoid ClassNotFoundException when JUnit is not available
		Runner.showInJUnitViewInternal(build);
	}

	public static void showInJUnitView(final IBuildPlan plan) {
		if (plan.getLastBuild() != null) {
			showInJUnitView(plan.getLastBuild());
		} else {
			RefreshOperation operation = BuildsUiInternal.getFactory().getRefreshOperation(plan);
			operation.addOperationChangeListener(new OperationChangeListener() {
				@Override
				public void done(OperationChangeEvent event) {
					event.getOperation().getService().getRealm().asyncExec(new Runnable() {
						public void run() {
							if (plan.getLastBuild() != null) {
								showInJUnitView(plan.getLastBuild());
							}
						}
					});
				}
			});
			operation.execute();
		}
	}

}

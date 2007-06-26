/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.context.core;

import org.eclipse.mylyn.context.core.IDegreeOfSeparation;

/**
 * @author Shawn Minto
 */
public class DegreeOfSeparation implements IDegreeOfSeparation {

	private String label;

	private int degree;

	public DegreeOfSeparation(String label, int degree) {
		this.label = label;
		this.degree = degree;
	}

	public String getLabel() {
		return label;
	}

	public int getDegree() {
		return degree;
	}

}

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
/*
 * Created on Feb 18, 2005
  */
package org.eclipse.mylar.java.search;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.mylar.java.JavaStructureBridge;


/**
 * @author Mik Kersten
 */
public class JavaImplementorsProvider extends AbstractJavaRelationshipProvider {

    public static final String ID = "org.eclipse.mylar.java.search.implementors";
    public static final String NAME = "Java implementors";
    
    public JavaImplementorsProvider() {
        super(JavaStructureBridge.EXTENSION, ID);
    }

    @Override
    protected boolean acceptElement(IJavaElement javaElement) {
        return javaElement != null && javaElement instanceof IType;
    }
    
    @Override
    protected String getSourceId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }
}

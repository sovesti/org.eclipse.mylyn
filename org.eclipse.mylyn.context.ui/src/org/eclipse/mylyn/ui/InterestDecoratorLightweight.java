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
 * Created on Apr 12, 2005
  */
package org.eclipse.mylar.ui;

import java.util.ConcurrentModificationException;

import org.eclipse.jface.viewers.*;
import org.eclipse.mylar.core.IMylarElement;
import org.eclipse.mylar.core.IMylarStructureBridge;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.internal.MylarContextRelation;
import org.eclipse.mylar.ui.internal.UiUtil;


/**
 * @author Mik Kersten
 */
public class InterestDecoratorLightweight implements ILightweightLabelDecorator {

    public InterestDecoratorLightweight() {
        super();
    }

    public void decorate(Object element, IDecoration decoration) {
    	IMylarStructureBridge adapter = null;
    	try {
    		if(MylarPlugin.getDefault() == null)
    			return;
    		adapter = MylarPlugin.getDefault().getStructureBridge(element);
	    } catch (ConcurrentModificationException cme) {
	    	// ignored, because we can add structure bridges during decoration
	    }
    	try {
    		IMylarElement node = null;
            if (element instanceof MylarContextRelation) {
                decoration.setForegroundColor(MylarUiPlugin.getDefault().getColorMap().RELATIONSHIP);
            } else  if (element instanceof IMylarElement) {
                node = (IMylarElement)element;
            } else {
                if (adapter != null && adapter.getContentType() != null) {
                    node = MylarPlugin.getContextManager().getNode(adapter.getHandleIdentifier(element));
                }
            }
            if (node != null) {
                decoration.setBackgroundColor(UiUtil.getBackgroundForElement(node));
                decoration.setForegroundColor(UiUtil.getForegroundForElement(node));      
                if (adapter != null 
                	&& adapter.canBeLandmark(node.getHandleIdentifier()) 
                    && node.getDegreeOfInterest().isLandmark() 
                    && !node.getDegreeOfInterest().isPropagated()
                    && !node.getDegreeOfInterest().isPredicted()
                    ) {
                    decoration.setFont(MylarUiPlugin.BOLD);
                } 
            }
        } catch (Exception e) {
        	MylarPlugin.log(e, "decoration failed");
        }
    } 
  
    public void addListener(ILabelProviderListener listener) {
    	// don't care about listeners
    }

    public void dispose() {
    	// don't care when we are disposed
    }

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    	// don't care about listeners
    }

}

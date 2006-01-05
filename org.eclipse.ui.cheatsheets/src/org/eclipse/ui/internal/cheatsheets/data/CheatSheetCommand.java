/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.data;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.internal.cheatsheets.CommandRunner;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.w3c.dom.Node;

/**
 * A command which can be executed from the cheatsheet
 */

public class CheatSheetCommand extends AbstractExecutable {

	private String serialization;
	private boolean serializationFound;
	
	public void setSerialization(String serialization) {
		this.serialization = serialization;	
	}
	
	public String getSerialization() {
		return serialization;
	}

	public boolean isCheatSheetManagerUsed() {
		return false;
	}

	public IStatus execute(ICheatSheetManager csm) {
		return new CommandRunner().executeCommand(this);
	}

	public boolean hasParams() {
		return false;
	}

	public boolean handleAttribute(Node attribute) {

		if (IParserTags.SERIALIZATION.equals(attribute.getNodeName())) {
		    setSerialization(attribute.getNodeValue());
		    serializationFound = true;
		    return true;
		}
		return false;
	}

	public String checkAttributes(Node node) {
		if(!serializationFound) {
			return NLS.bind(Messages.ERROR_PARSING_NO_SERIALIZATION, (new Object[] {node.getNodeName()}));
		}
		return null;
	}

}

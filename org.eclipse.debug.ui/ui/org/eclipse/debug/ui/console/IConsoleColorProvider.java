/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.console;


import org.eclipse.debug.core.model.IProcess;
import org.eclipse.swt.graphics.Color;

/**
 * Provides coloring for a console document. When a process is added to a
 * registered launch the debug plug-in creates a console document for the
 * process. By default, a document is created which is connected to the standard
 * input, output, and error streams associated with the process. A client may
 * override the default coloring by specifying a custom content provider for a
 * process type. A proccess type is defined via the process attribute
 * <code>IProcess. ATTR_PROCESS_TYPE</code>.
 * <p>
 * A console color provider extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a console color
 * provider extension.
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.consoleColorProviders"&gt;
 *   &lt;consoleColorProvider 
 *      id="com.example.ExampleConsoleColorProvider"
 *      class="com.example.ExampleConsoleColorProviderClass"
 *      processType="ExampleProcessType"&gt;
 *   &lt;/consoleColorProvider&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this color provider.</li>
 * <li><code>class</code> specifies a fully qualified name of a Java class
 *  that implements <code>IConsoleColorProvider</code>.</li>
 * <li><code>processType</code> specifies the identifier of the process type
 * this content provider is associated with (which corresponds to the
 * <code>ATTR_PROCESS_TYPE</code> attribute on a process).</li>
 * </ul>
 * </p> 
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 2.1
 */

public interface IConsoleColorProvider {

	/**
	 * Returns whether the console associated with this color provider's
	 * process can currently accept keyboard input. This attribute is dynamic
	 * and may change over the lifetime of a process/document.
	 * 
	 * @return whether the console associated with this color provider's
	 * process can currently accept keyboard input
	 */
	public boolean isReadOnly();
	
	/**
	 * Returns the color to draw output associated with the given stream.
	 * 
	 * @param streamIdentifer
	 * @return Color
	 */
	public Color getColor(String streamIdentifer);
	
	/**
	 * Connects this color provider to the given process and console document.
	 * This color provider should connect its streams to the given console
	 * document.
	 * 
	 * @param process
	 * @param partitioner
	 */
	public void connect(IProcess process, IConsole console);
	
	/**
	 * Disconnects this color provider.
	 */
	public void disconnect();
}

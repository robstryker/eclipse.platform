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

package org.eclipse.ui.internal.cheatsheets.composite.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskGroup;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParserException;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CompositeCheatSheetParser implements IStatusContainer {
	
	private DocumentBuilder documentBuilder;
	
	private IStatus status;
	
	private int nextTaskId = 0;
	
	
	/**
	 *  Gets the status of the last call to parseGuide
	 */
	
	public IStatus getStatus() {
		return status;
	}
	
	/**
	 * Returns the DocumentBuilder to be used by composite cheat sheets.
	 */
	public DocumentBuilder getDocumentBuilder() {
		if(documentBuilder == null) {
			try {
				documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (Exception e) {
				addStatus(IStatus.ERROR, Messages.ERROR_CREATING_DOCUMENT_BUILDER, e);
			}
		}
		return documentBuilder;
	}
	
	private final int PARSER_ERROR = 1001; // TODO is there another number that would be more meaningful
	
	/**
	 * If the status is OK set it to reflect the new error condition, otherwise
	 * add to the existing status making it a MultiStatus if necessary.
	 */
	public void addStatus(int severity, String message, Throwable exception) {       
		Status newStatus = new Status(severity, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, PARSER_ERROR, message, exception);
		if (status.isOK()) {
			status = newStatus;
		} else if (status instanceof MultiStatus) {
			((MultiStatus)status).add(newStatus);
		} else {
			MultiStatus multiStatus = new MultiStatus(ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, 
					Messages.ERROR_MULTIPLE_ERRORS,  exception);
			multiStatus.add(status);
			multiStatus.add(newStatus);
			status = multiStatus;
		}
	}
	
	/**
	 * Parse a composite cheat sheet from a url. The parser status will be set as a result
	 * of this operation, if the status is IStatus.ERROR the parser returns null  
	 * @param url The url of the input
	 * @return A valid composite cheat sheet or null if there was an error
	 */
	public CompositeCheatSheetModel parseGuide(URL url) {
		status = Status.OK_STATUS;
		if(url == null) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {""})); //$NON-NLS-1$
			addStatus(IStatus.ERROR,  message,  null);
			return null;
		}

		InputStream is = null;	

		try {
			is = url.openStream();

			if (is == null) {
				String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {url.getFile()}));
				addStatus(IStatus.ERROR,  message,  null);
				return null;
			}
		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE, (new Object[] {url.getFile()}));
			addStatus(IStatus.ERROR,  message,  e);
			return null;
		}
		
		Document document;	
		String filename = url.getFile();
		try {
			InputSource inputSource = new InputSource(is);
			document = getDocumentBuilder().parse(inputSource);
		} catch (IOException e) {
			String message = NLS.bind(Messages.ERROR_OPENING_FILE_IN_PARSER, (new Object[] {filename}));
			addStatus(IStatus.ERROR,  message,  e);
			return null;
		} catch (SAXParseException spe) {
			String message = NLS.bind(Messages.ERROR_SAX_PARSING_WITH_LOCATION, (new Object[] {filename, new Integer(spe.getLineNumber()), new Integer(spe.getColumnNumber())}));
			addStatus(IStatus.ERROR,  message,  spe);
			return null;
		} catch (SAXException se) {
			String message = NLS.bind(Messages.ERROR_SAX_PARSING, (new Object[] {filename}));
			addStatus(IStatus.ERROR,  message, se);
			return null;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}
		
		CompositeCheatSheetModel result = parseCompositeCheatSheet(document, url);
		return result;		
	}

	/**
	 * Parse a composite cheatsheet. The parser status will be set as a result
	 * of this operation, if the status is IStatus.ERROR the parser returns null  
	 * @param url The url of the input. This is only used so the model can record
	 * its input location
	 * @param document the document to be parse
	 * @return A valid composite cheat sheet or null if there was an error
	 * @return
	 */
	public CompositeCheatSheetModel parseCompositeCheatSheet(Document document, URL url)   {
		status = Status.OK_STATUS;
		try {
			// If the document passed is null return a null tree and update the status
			if (document != null) {
				Node rootnode = document.getDocumentElement();
				
				// Is the root node correct?
				if( !rootnode.getNodeName().equals(ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET )) {
					String message = NLS.bind(Messages.ERROR_PARSING_ROOT_NODE_TYPE, (
							new Object[] {ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET}));
					throw new CheatSheetParserException(message);
				}
				
				String name = ""; //$NON-NLS-1$
				String explorerId = ICompositeCheatsheetTags.TREE;
				
				NamedNodeMap attributes = rootnode.getAttributes();
				if (attributes != null) {
					for (int x = 0; x < attributes.getLength(); x++) {
						Node attribute = attributes.item(x);
						String attributeName = attribute.getNodeName();
						if ( attributeName != null && attributeName.equals(ICompositeCheatsheetTags.NAME)) {
							name= attribute.getNodeValue();
						}
						if (attributeName.equals(ICompositeCheatsheetTags.EXPLORER)) {
							explorerId= attribute.getNodeValue();
						}
					}
				}
				CompositeCheatSheetModel compositeCS = new CompositeCheatSheetModel(name, name, explorerId);
				
				parseCompositeCheatSheetChildren(rootnode, compositeCS);
				
				compositeCS.getDependencies().resolveDependencies(this);
				
				if (compositeCS.getRootTask() == null) {
					addStatus(IStatus.ERROR, Messages.ERROR_PARSING_NO_ROOT, null);
				} else if (status.getSeverity() != IStatus.ERROR) {
					compositeCS.setContentUrl(url);
				    return compositeCS;
				}
			} 
			return null;
		} catch(CheatSheetParserException e) {
			addStatus(IStatus.ERROR, e.getMessage(), null);
			return null;
		}	
	}

	private void parseCompositeCheatSheetChildren(Node compositeCSNode, CompositeCheatSheetModel model) {
		nextTaskId = 0;
		NodeList childNodes = compositeCSNode.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index++) {
			Node nextNode = childNodes.item(index);
			if (isAbstractTask(nextNode.getNodeName()) ) {
				AbstractTask task = parseAbstractTask(nextNode, model);
			    if (model.getRootTask() == null ) { 
					model.setRootTask(task);
					parseTaskChildren(nextNode, task, model);
			    } else {
				    addStatus(IStatus.ERROR, Messages.ERROR_PARSING_MULTIPLE_ROOT, null);
			    }
			}
		}		
	}

	public static boolean isAbstractTask(String nodeName) {
		return nodeName == ICompositeCheatsheetTags.TASK ||
			nodeName == ICompositeCheatsheetTags.TASK_GROUP;
	}
	
	private void parseTaskChildren(Node parentNode, AbstractTask parentTask, CompositeCheatSheetModel model) {
		NodeList childNodes = parentNode.getChildNodes();
		ITaskParseStrategy strategy = parentTask.getParserStrategy();
		strategy.init();
		for (int index = 0; index < childNodes.getLength(); index++) {
			Node childNode = childNodes.item(index);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = childNode.getNodeName();
				if (nodeName == IParserTags.PARAM) {
					addParameter(parentTask, childNode.getAttributes());			
				} else if (nodeName == IParserTags.INTRO) {
					parentTask.setDescription(parseTextMarkup(childNode, parentTask, this));
				} else if (nodeName == ICompositeCheatsheetTags.ON_COMPLETION) {
					parentTask.setCompletionMessage(parseTextMarkup(childNode, parentTask, this));						
				} else if (nodeName == ICompositeCheatsheetTags.DEPENDS_ON) {
					parseDependency(childNode, parentTask, model);
				} else if (CompositeCheatSheetParser.isAbstractTask(nodeName)) {
					if (parentTask instanceof TaskGroup) {
					    AbstractTask task = parseAbstractTask(childNode, model);
					    ((TaskGroup)parentTask).addSubtask(task);
					    parseTaskChildren(childNode, task, model);
					}
			    } else {
					if (!strategy.parseElementNode(childNode, parentNode, parentTask, this)) {
						String message = NLS
						.bind(
								Messages.WARNING_PARSING_UNKNOWN_ELEMENT,
								(new Object[] { nodeName,
										parentNode.getNodeName() }));
				        addStatus(IStatus.WARNING, message, null);
					}
				}
			}
		}	
	}

	private void parseDependency(Node taskNode, AbstractTask task, CompositeCheatSheetModel model) {
		NamedNodeMap attributes = taskNode.getAttributes();
		if (attributes != null) {
		     Node taskAttribute = attributes.getNamedItem(ICompositeCheatsheetTags.TASK);
		     if (taskAttribute != null) {
		    	 String requiredTaskId = taskAttribute.getNodeValue();
		    	 model.getDependencies().addDependency(task, requiredTaskId);
		     } else {
		    	 addStatus(IStatus.ERROR, Messages.ERROR_PARSING_NO_ID, null);
		     }
		}	
	}

	private void addParameter(AbstractTask parentTask, NamedNodeMap attributes) {
		String name = null;
		String value = null;

		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;
				if (attributeName.equals(ICompositeCheatsheetTags.NAME)) {
					name = attribute.getNodeValue();
				}
				if (attributeName.equals(ICompositeCheatsheetTags.VALUE)) {
					value= attribute.getNodeValue();
				}	
			}
		}
		if (name == null) {
			addStatus(IStatus.WARNING, Messages.ERROR_PARSING_NO_NAME, null);
			return;
		} else if (value == null) {
			addStatus(IStatus.WARNING, Messages.ERROR_PARSING_NO_VALUE, null);
			return;
		} else {
			parentTask.getParameters().put(name, value);
		}
		
	}

	private AbstractTask parseAbstractTask(Node taskNode, CompositeCheatSheetModel model) {
		AbstractTask task;
		NamedNodeMap attributes = taskNode.getAttributes();
		String kind = null;
		String name = null;
		String id = null;
		boolean skippable = false;
		if (attributes != null) {
			for (int x = 0; x < attributes.getLength(); x++) {
				Node attribute = attributes.item(x);
				String attributeName = attribute.getNodeName();
				if (attribute == null || attributeName == null)
					continue;
				if (attributeName.equals(ICompositeCheatsheetTags.KIND)) {
					kind = attribute.getNodeValue();
				}
				if (attributeName.equals(ICompositeCheatsheetTags.NAME)) {
					name= attribute.getNodeValue();
				}
				if (attributeName.equals(IParserTags.ID)) {
					id = attribute.getNodeValue();
				}			
				if (attributeName.equals(IParserTags.SKIP)) {
					skippable = "true".equalsIgnoreCase(attribute.getNodeValue()); //$NON-NLS-1$
				}				
			}
		}

		if (id == null) {
			id = autoGenerateId();
		}
		String nodeName = taskNode.getNodeName();
		task = createTask(nodeName, model, kind, id, name);
		task.setSkippable(skippable);

		if (model.getDependencies().getTask(id) != null) {
			String message = NLS.bind(Messages.ERROR_PARSING_DUPLICATE_TASK_ID, (new Object[] {id}));
			addStatus(IStatus.ERROR, message, null);
		} else {
		    model.getDependencies().saveId(task);
		}

		return task;
	}

	private AbstractTask createTask(String nodeKind, CompositeCheatSheetModel model, String kind, String id, String name) {
		AbstractTask task;
		if (ICompositeCheatsheetTags.TASK_GROUP.equals(nodeKind)) {
			task = new TaskGroup(model, id, name, kind);
		} else {
			task = new EditableTask(model, id, name, kind);
		}
		String completionMessage = NLS.bind(Messages.COMPLETED_TASK, (new Object[] {name}));
		task.setCompletionMessage(completionMessage);
		return task;
	}
	
	private String autoGenerateId() {
		return "TaskId_" + nextTaskId++; //$NON-NLS-1$
	}

	public static String parseTextMarkup(Node descriptionNode, 
			                             AbstractTask parentTask,
			                             IStatusContainer status) {
	    NodeList nodes = descriptionNode.getChildNodes();
		StringBuffer text = new StringBuffer();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.TEXT_NODE) {
				text.append(node.getNodeValue());
			} else if (node.getNodeType() == Node.ELEMENT_NODE) {
				// handle <b></b> and <br/>
				if (node.getNodeName().equals(IParserTags.BOLD)) {
					text.append(IParserTags.BOLD_START_TAG);
					text.append(node.getFirstChild().getNodeValue());
					text.append(IParserTags.BOLD_END_TAG);
				} else if (node.getNodeName().equals(IParserTags.BREAK)) {
					text.append(IParserTags.BREAK_TAG);
				} else {
					String message = NLS
							.bind(
									Messages.WARNING_PARSING_DESCRIPTION_UNKNOWN_ELEMENT,
									(new Object[] { parentTask.getName(),
											node.getNodeName() }));
					status.addStatus(IStatus.WARNING, message, null);
				}
			}
		}

		// Remove the new line, form feed and tab chars
		return text.toString().trim();
	}

}

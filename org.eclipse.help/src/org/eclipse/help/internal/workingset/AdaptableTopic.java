package org.eclipse.help.internal.workingset;

/*
 * (c) Copyright IBM Corp. 2002. 
 * All Rights Reserved.
 */

import java.util.*;
import java.util.Map;

import org.eclipse.help.*;
import org.eclipse.help.internal.util.FastStack;
import org.w3c.dom.Element;

/**
 * Makes help resources adaptable and persistable
 */
public class AdaptableTopic extends AdaptableHelpResource {
	/**
	 * Map of all topics with this topic as ancestor
	 */
	private Map topicMap;
	
	/**
	 * This constructor will be called when wrapping help resources.
	 */
	AdaptableTopic(ITopic element) {
		super(element);
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == ITopic.class)
			return element;
		else
			return super.getAdapter(adapter);
	}

	public AdaptableHelpResource[] getChildren() {
		return new AdaptableHelpResource[0];
	}

	/**
	 * @see org.eclipse.help.ITopic#getSubtopics()
	 */
	public ITopic[] getSubtopics() {
		return ((ITopic)element).getSubtopics();
	}
	
	/**
	 * Returns a topic with the specified href.
	 * <br> It is possible that multiple tocs have
	 * the same href, in which case there is no guarantee
	 * which one is returned.
	 * @param href The topic's href value.
	 */
	public ITopic getTopic(String href) {
		if (href == null)
			return null;

		if (topicMap == null) {
			// traverse TOC and fill in the topicMap
			topicMap = new HashMap();
			topicMap.put(getHref(),element);
			FastStack stack = new FastStack();
			ITopic[] topics = getSubtopics();
			for (int i = 0; i < topics.length; i++)
				stack.push(topics[i]);
			while (!stack.isEmpty()) {
				ITopic topic = (ITopic) stack.pop();
				if (topic != null) {
					String topicHref = topic.getHref();
					if (topicHref != null) {
						topicMap.put(topicHref, topic);
					}
					ITopic[] subtopics = topic.getSubtopics();
					for (int i = 0; i < subtopics.length; i++)
						stack.push(subtopics[i]);
				}
			}
		}
		return (ITopic) topicMap.get(href);
	}

	public void saveState(Element element) {
		AdaptableToc toc = (AdaptableToc)getParent();
		toc.saveState(element);
		AdaptableHelpResource[] topics = toc.getChildren();
		for (int i=0; i<topics.length; i++)
			if (topics[i] == this)		
				element.setAttribute("topic", String.valueOf(i));
	}
}

package org.eclipse.core.internal.utils;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.util.Enumeration;
/**
 * A Queue of objects.
 */
public class Queue {
	protected Object[] elements;
	protected int head;
	protected int tail;
	protected boolean reuse;
public Queue() {
	this(20, false);
}
/**
 * The parameter reuse indicates what do you want to happen with
 * the object reference when you remove it from the queue. If
 * reuse is false the queue no longer holds a reference to the
 * object when it is removed. If reuse is true you can use the
 * method getNextAvailableObject to get an used object, set its
 * new values and add it again to the queue.
 */
public Queue(int size, boolean reuse) {
	elements = new Object[size];
	head = tail = 0;
	this.reuse = reuse;
}
public void add(Object element) {
	int newTail = increment(tail);
	if (newTail == head) {
		grow();
		newTail = tail + 1;
	}
	elements[tail] = element;
	tail = newTail;
}
public void clear() {
	if (tail >= head) {
		for (int i = head; i < tail; i++)
			elements[i] = null;
	} else {
		for (int i = head; i < elements.length; i++)
			elements[i] = null;
		for (int i = 0; i < tail; i++)
			elements[i] = null;
	}
	tail = head = 0;
}
public boolean contains(Object o) {
	return get(o) != null;
}
/**
 * This method does not affect the queue itself. It is only a
 * helper to decrement an index in the queue.
 */
public int decrement(int index) {
	return (index == 0) ? (elements.length - 1) : index - 1;
}
public Object elementAt(int index) {
	return elements[index];
}
public Enumeration elements() {
	/**/
	if (isEmpty())
		return EmptyEnumeration.getEnumeration();

	/* if head < tail we can use the same array */
	if (head <= tail)
		return new ArrayEnumeration(elements, head, tail - 1);

	/* otherwise we need to create a new array */
	Object[] newElements = new Object[size()];
	int end = (elements.length - head);
	System.arraycopy(elements, head, newElements, 0, end);
	System.arraycopy(elements, 0, newElements, end, tail);
	return new ArrayEnumeration(newElements);
}
public Object get(Object o) {
	int index = head;
	while (index != tail) {
		if (elements[index].equals(o))
			return elements[index];
		index = increment(index);
	}
	return null;
}
/**
 * It returns null if there are no objects available.
 */
public Object getNextAvailableObject() {
	int index = tail;
	while (index != head) {
		if (elements[index] != null) {
			Object result = elements[index];
			elements[index] = null;
			return result;
		}
		index = increment(index);
	}
	return null;
}
protected void grow() {
	int newSize = (int) (elements.length * 1.5);
	Object[] newElements = new Object[newSize];
	if (tail >= head)
		System.arraycopy(elements, head, newElements, head, size());
	else {
		int newHead = newSize - (elements.length - head);
		System.arraycopy(elements, 0, newElements, 0, tail + 1);
		System.arraycopy(elements, head, newElements, newHead, (newSize - newHead));
		head = newHead;
	}
	elements = newElements;
}
/**
 * This method does not affect the queue itself. It is only a
 * helper to increment an index in the queue.
 */
public int increment(int index) {
	return (index == (elements.length - 1)) ? 0 : index + 1;
}
public int indexOf(Object target) {
	if (tail >= head) {
		for (int i = head; i < tail; i++)
			if (target.equals(elements[i]))
				return i;
	} else {
		for (int i = head; i < elements.length; i++)
			if (target.equals(elements[i]))
				return i;
		for (int i = 0; i < tail; i++)
			if (target.equals(elements[i]))
				return i;
	}
	return -1;
}
public boolean isEmpty() {
	return tail == head;
}
public Object peek() {
	return elements[head];
}
public Object peekTail() {
	return elements[decrement(tail)];
}
public Object remove() {
	if (isEmpty())
		return null;
	Object result = peek();
	if (!reuse)
		elements[head] = null;
	head = increment(head);
	return result;
}
public Object removeTail() {
	Object result = peekTail();
	tail = decrement(tail);
	if (!reuse)
		elements[tail] = null;
	return result;
}
public void reset() {
	tail = head = 0;
}
public int size() {
	return tail > head ? (tail - head) : ((elements.length - head) + tail);
}
public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append("[");
	Enumeration enum = elements();
	if (!isEmpty()) {
	    while (true) {
			sb.append(enum.nextElement());
			if (enum.hasMoreElements())
				sb.append(", ");
			else
				break;
		}
	}	    
	sb.append("]");
	return sb.toString();
}
}

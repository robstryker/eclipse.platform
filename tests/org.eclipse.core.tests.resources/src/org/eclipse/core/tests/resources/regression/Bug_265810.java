/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.resources.ResourceTest;

public class Bug_265810 extends ResourceTest {

	protected final static String VARIABLE_NAME = "ROOT";
	private final ArrayList<IPath> toDelete = new ArrayList<IPath>();
	List<IResourceDelta> resourceDeltas = new ArrayList<IResourceDelta>();

	/**
	 * Constructor for Bug_265810.
	 */
	public Bug_265810() {
		super();
	}

	/**
	 * Constructor for Bug_265810.
	 * @param name
	 */
	public Bug_265810(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(Bug_265810.class);
	}

	protected void setUp() throws Exception {
		IPath base = super.getRandomLocation();
		toDelete.add(base);
		super.setUp();
	}

	protected void tearDown() throws Exception {
		IPath[] paths = toDelete.toArray(new IPath[0]);
		toDelete.clear();
		for (int i = 0; i < paths.length; i++)
			Workspace.clear(paths[i].toFile());
		super.tearDown();
	}

	/**
	 * @see org.eclipse.core.tests.harness.ResourceTest#getRandomLocation()
	 */
	public IPath getRandomLocation() {
		IPath path = FileSystemHelper.computeRandomLocation(getTempDir());
		try {
			path.toFile().createNewFile();
		} catch (IOException e) {
			fail("can't create the file", e);
		}
		toDelete.add(path);
		return path;
	}

	public void testBug() {

		// create a project
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		try {
			project.create(new NullProgressMonitor());
			project.open(new NullProgressMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// create a linked resource
		final IFile file = project.getFile(getUniqueString());
		// the file should not exist yet
		assertDoesNotExistInWorkspace("2.0", file);
		try {
			file.createLink(getRandomLocation(), IResource.NONE, new NullProgressMonitor());
			file.setContents(getContents("contents for a file"), IResource.NONE, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// save the .project [1] content
		byte[] dotProject1 = storeDotProject(project);

		// create a new linked file
		final IFile newFile = project.getFile("newFile");
		// the file should not exist yet
		assertDoesNotExistInWorkspace("5.0", newFile);
		try {
			newFile.createLink(getRandomLocation(), IResource.NONE, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("6.0", e);
		}

		// save the .project [2] content
		byte[] dotProject2 = storeDotProject(project);

		try {
			resourceDeltas = new ArrayList<IResourceDelta>();
			getWorkspace().addResourceChangeListener(ll);

			// restore .project [1]
			restoreDotProject(project, dotProject1);

			assertEquals("9.0", 1, resourceDeltas.size());
			assertEquals("9.1", newFile, resourceDeltas.get(0).getResource());
			assertEquals("9.2", IResourceDelta.REMOVED, resourceDeltas.get(0).getKind());
		} finally {
			getWorkspace().removeResourceChangeListener(ll);
		}

		// create newFile as a non-linked resource
		try {
			newFile.create(getContents("content"), IResource.NONE, new NullProgressMonitor());
		} catch (CoreException e1) {
			fail("10.0", e1);
		}

		try {
			resourceDeltas = new ArrayList<IResourceDelta>();
			getWorkspace().addResourceChangeListener(ll);

			// restore .project [2]
			restoreDotProject(project, dotProject2);

			assertEquals("11.0", 1, resourceDeltas.size());
			assertEquals("11.1", newFile, resourceDeltas.get(0).getResource());
			assertEquals("11.2", IResourceDelta.REPLACED, resourceDeltas.get(0).getFlags() & IResourceDelta.REPLACED);
		} finally {
			getWorkspace().removeResourceChangeListener(ll);
		}
	}

	private byte[] storeDotProject(IProject project) {
		byte[] buffer = new byte[2048];
		int bytesRead = 0;
		byte[] doProject = new byte[0];

		try {
			InputStream iS = project.getFile(".project").getContents();
			bytesRead = iS.read(buffer);
			iS.close();
		} catch (IOException e) {
			fail("storing dotProject failed", e);
		} catch (CoreException e) {
			fail("storing dotProject failed", e);
		}

		doProject = new byte[bytesRead];
		System.arraycopy(buffer, 0, doProject, 0, bytesRead);

		return doProject;
	}

	private void restoreDotProject(IProject project, byte[] dotProject) {
		try {
			project.getFile(".project").setContents(new ByteArrayInputStream(dotProject), IResource.NONE, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("restoring dotProject failed", e);
		}
	}

	IResourceChangeListener ll = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					public boolean visit(IResourceDelta delta) {
						IResource resource = delta.getResource();
						if (resource instanceof IFile && !resource.getName().equals(".project"))
							addToResourceDelta(delta);
						if (delta.getAffectedChildren().length > 0)
							return true;
						return false;
					}
				});
			} catch (CoreException e) {
				fail("listener failed", e);
			}
		}
	};

	boolean addToResourceDelta(IResourceDelta delta) {
		return resourceDeltas.add(delta);
	}
}

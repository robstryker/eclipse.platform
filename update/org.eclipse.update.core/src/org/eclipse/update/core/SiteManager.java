package org.eclipse.update.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.internal.core.*;

public class SiteManager {
	

	private SiteManager() {
		//  Blocking instance creation
	}

	/**
	 * Returns the LocalSite i.e the different sites
	 * the user has access to (either read only or read write)
	 * 
	 * @return the local site
	 */	
	public static ILocalSite getLocalSite() throws CoreException{
		return InternalSiteManager.getLocalSite();
	}
	
	/** 
	 * Returns an site based on the protocol of the URL
	 * If the Site has a different Type/Site Handler not known up to now,
	 * it will be discovered when parsing the <code>site.xml</code> file.
	 * 
	 * @return the site which maps to this URL
	 */	
	public static ISite getSite(URL siteURL) throws CoreException {
		return InternalSiteManager.getSite(siteURL);
	}
	
	
	/**
	 * Returns a local temporary site where 
	 * some feature may be temporary transfered before
	 * being installed.
	 * 
	 * @return the temporary site
	 */
	public static ISite getTempSite() throws CoreException {
		return InternalSiteManager.getTempSite();
	}
	

	/** 
	 * Resolves a URL to the local file System.
	 * If the URL is already accessible through the file system 
	 * (i.e the protocol is file) it returns itself
	 * Otherwise, it copies the file of the URL to the TEMP
	 * directory and return a file URL pointing to the file
	 * in the TEMP directory
	 * 
	 * @param url teh URL to be resolve
	 * @return the locally resolved URL
	 * @throws IOException if the remote URL canot be found
	 * @throws MalformedURLException if the local URL cannot be created
	 * @throws CoreException	 if we cannot access the local TEMP directory
	 */
	public static URL resolveAsLocal(URL url) throws IOException, MalformedURLException, CoreException {
		return UpdateManagerUtils.resolveAsLocal(url);
	}
}
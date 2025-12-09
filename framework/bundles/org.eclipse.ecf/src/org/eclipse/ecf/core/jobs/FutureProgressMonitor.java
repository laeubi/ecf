/****************************************************************************
 * Copyright (c) 2024 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Progress monitor for futures that supports child progress monitors.
 * This class is a replacement for org.eclipse.equinox.concurrent.future.FutureProgressMonitor
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public class FutureProgressMonitor extends NullProgressMonitor {
	private IProgressMonitor childProgressMonitor;

	public FutureProgressMonitor() {
		super();
	}

	/**
	 * Set the child progress monitor.
	 * 
	 * @param child the child monitor. May be null.
	 */
	public void setChildProgressMonitor(IProgressMonitor child) {
		this.childProgressMonitor = child;
	}

	/**
	 * Get the child progress monitor.
	 * 
	 * @return IProgressMonitor the child monitor. May be null.
	 */
	public IProgressMonitor getChildProgressMonitor() {
		return childProgressMonitor;
	}

	@Override
	public void beginTask(String name, int totalWork) {
		if (childProgressMonitor != null)
			childProgressMonitor.beginTask(name, totalWork);
	}

	@Override
	public void done() {
		if (childProgressMonitor != null)
			childProgressMonitor.done();
	}

	@Override
	public void internalWorked(double work) {
		if (childProgressMonitor != null)
			childProgressMonitor.internalWorked(work);
	}

	@Override
	public boolean isCanceled() {
		return childProgressMonitor != null && childProgressMonitor.isCanceled();
	}

	@Override
	public void setCanceled(boolean value) {
		if (childProgressMonitor != null)
			childProgressMonitor.setCanceled(value);
	}

	@Override
	public void setTaskName(String name) {
		if (childProgressMonitor != null)
			childProgressMonitor.setTaskName(name);
	}

	@Override
	public void subTask(String name) {
		if (childProgressMonitor != null)
			childProgressMonitor.subTask(name);
	}

	@Override
	public void worked(int work) {
		if (childProgressMonitor != null)
			childProgressMonitor.worked(work);
	}
}

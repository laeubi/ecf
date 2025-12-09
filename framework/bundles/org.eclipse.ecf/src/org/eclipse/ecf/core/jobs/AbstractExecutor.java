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

/**
 * Abstract base class for IExecutor implementations.
 * This class is a replacement for org.eclipse.equinox.concurrent.future.AbstractExecutor
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public abstract class AbstractExecutor implements IExecutor {
	
	/**
	 * Execute the given runnable asynchronously.
	 * Subclasses must implement this method to define the execution strategy.
	 * 
	 * @param runnable the runnable to execute
	 * @param clientProgressMonitor optional progress monitor
	 * @return IFuture representing the asynchronous execution
	 */
	public abstract IFuture execute(IProgressRunnable runnable, IProgressMonitor clientProgressMonitor);

	/**
	 * Create a future for the given progress monitor.
	 * Subclasses may override this to provide custom future implementations.
	 * 
	 * @param progressMonitor the progress monitor
	 * @return AbstractFuture the created future
	 */
	protected AbstractFuture createFuture(IProgressMonitor progressMonitor) {
		return new SingleOperationFuture(progressMonitor);
	}

	/**
	 * Set the child progress monitor on the given parent monitor if it supports it.
	 * 
	 * @param parent the parent monitor
	 * @param child the child monitor
	 */
	protected void setChildProgressMonitor(IProgressMonitor parent, IProgressMonitor child) {
		if (parent instanceof FutureProgressMonitor) {
			((FutureProgressMonitor) parent).setChildProgressMonitor(child);
		}
	}

	/**
	 * Run the given runnable safely using the given runner.
	 * 
	 * @param runner the safe progress runner
	 * @param progressRunnable the runnable to run
	 */
	protected void safeRun(ISafeProgressRunner runner, IProgressRunnable progressRunnable) {
		runner.runWithProgress(progressRunnable);
	}
}

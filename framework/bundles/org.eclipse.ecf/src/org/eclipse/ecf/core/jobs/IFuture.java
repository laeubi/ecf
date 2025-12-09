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
import org.eclipse.core.runtime.IStatus;

/**
 * Interface representing an asynchronous computation result.
 * This interface is a replacement for org.eclipse.equinox.concurrent.future.IFuture
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public interface IFuture {
	/**
	 * Retrieve the result of the asynchronous computation. This method will block
	 * until the computation is complete.
	 * 
	 * @return Object the result of the computation. May be null.
	 * @throws InterruptedException if the current thread is interrupted while waiting
	 * @throws java.util.concurrent.ExecutionException if the computation threw an exception
	 */
	Object get() throws InterruptedException, java.util.concurrent.ExecutionException;

	/**
	 * Retrieve the result of the asynchronous computation, waiting up to the given timeout.
	 * 
	 * @param waitTimeInMillis the maximum time to wait in milliseconds
	 * @return Object the result of the computation. May be null.
	 * @throws InterruptedException if the current thread is interrupted while waiting
	 * @throws TimeoutException if the wait times out
	 * @throws java.util.concurrent.ExecutionException if the computation threw an exception
	 */
	Object get(long waitTimeInMillis) throws InterruptedException, TimeoutException, java.util.concurrent.ExecutionException;

	/**
	 * Returns true if the computation has completed.
	 * 
	 * @return boolean true if completed, false otherwise
	 */
	boolean isDone();

	/**
	 * Returns the status of the computation.
	 * 
	 * @return IStatus the status. Will not be null.
	 */
	IStatus getStatus();

	/**
	 * Cancel the computation.
	 * 
	 * @return boolean true if the computation was successfully cancelled, false otherwise
	 */
	boolean cancel();

	/**
	 * Returns true if the computation was cancelled.
	 * 
	 * @return boolean true if cancelled, false otherwise
	 */
	boolean isCanceled();

	/**
	 * Get the progress monitor associated with this future.
	 * 
	 * @return IProgressMonitor the progress monitor. May be null.
	 */
	IProgressMonitor getProgressMonitor();
}

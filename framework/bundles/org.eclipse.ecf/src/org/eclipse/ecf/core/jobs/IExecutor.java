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
 * Executor for running asynchronous tasks with progress monitoring.
 * This interface is a replacement for org.eclipse.equinox.concurrent.future.IExecutor
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public interface IExecutor {
	/**
	 * Execute the given runnable asynchronously and return an IFuture representing
	 * the result.
	 * 
	 * @param runnable the runnable to execute. Must not be null.
	 * @param clientProgressMonitor optional progress monitor for the client. May be null.
	 * @return IFuture representing the asynchronous execution. Will not be null.
	 */
	IFuture execute(IProgressRunnable runnable, IProgressMonitor clientProgressMonitor);
}

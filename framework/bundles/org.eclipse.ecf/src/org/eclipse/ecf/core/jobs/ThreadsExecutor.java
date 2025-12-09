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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Executor that runs tasks in a new thread.
 * This class is a replacement for org.eclipse.equinox.concurrent.future.ThreadsExecutor
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public class ThreadsExecutor extends AbstractExecutor {
	
	public IFuture execute(final IProgressRunnable runnable, final IProgressMonitor clientProgressMonitor) {
		Assert.isNotNull(runnable);
		final AbstractFuture future = createFuture(clientProgressMonitor);
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				safeRun(future, runnable);
			}
		}, "ThreadsExecutor-" + System.currentTimeMillis()); //$NON-NLS-1$
		
		thread.start();
		return future;
	}
}

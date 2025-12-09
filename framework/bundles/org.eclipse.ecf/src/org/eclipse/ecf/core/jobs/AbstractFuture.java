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

import java.util.concurrent.ExecutionException;
import org.eclipse.core.runtime.*;

/**
 * Abstract base class for IFuture implementations.
 * This class is a replacement for org.eclipse.equinox.concurrent.future.AbstractFuture
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public abstract class AbstractFuture implements IFuture, ISafeProgressRunner {
	private volatile boolean canceled = false;
	private volatile boolean done = false;
	private volatile Object result;
	private volatile Throwable exception;
	private IProgressMonitor progressMonitor;
	private IStatus status;

	protected AbstractFuture(IProgressMonitor progressMonitor) {
		this.progressMonitor = (progressMonitor != null) ? progressMonitor : new FutureProgressMonitor();
	}

	public IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public synchronized Object get() throws InterruptedException, ExecutionException {
		while (!done && !canceled) {
			wait();
		}
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return result;
	}

	public synchronized Object get(long waitTimeInMillis) throws InterruptedException, TimeoutException, ExecutionException {
		long startTime = System.currentTimeMillis();
		long waitTime = waitTimeInMillis;
		while (!done && !canceled && waitTime > 0) {
			wait(waitTime);
			long elapsed = System.currentTimeMillis() - startTime;
			waitTime = waitTimeInMillis - elapsed;
		}
		if (!done && !canceled) {
			throw new TimeoutException("Timeout waiting for result"); //$NON-NLS-1$
		}
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return result;
	}

	public synchronized boolean isDone() {
		return done;
	}

	public synchronized boolean cancel() {
		if (done) {
			return false;
		}
		canceled = true;
		if (progressMonitor != null) {
			progressMonitor.setCanceled(true);
		}
		notifyAll();
		return true;
	}

	public synchronized boolean isCanceled() {
		return canceled;
	}

	protected synchronized void setResult(Object result) {
		if (!done && !canceled) {
			this.result = result;
			done = true;
			notifyAll();
		}
	}

	protected synchronized void setException(Throwable exception) {
		if (!done && !canceled) {
			this.exception = exception;
			done = true;
			notifyAll();
		}
	}

	public synchronized IStatus getStatus() {
		if (status != null) {
			return status;
		}
		if (done) {
			if (exception != null) {
				return new Status(IStatus.ERROR, "org.eclipse.ecf", "Execution failed", exception); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return Status.OK_STATUS;
		}
		if (canceled) {
			return Status.CANCEL_STATUS;
		}
		return new Status(IStatus.INFO, "org.eclipse.ecf", "Running"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected synchronized void setStatus(IStatus status) {
		this.status = status;
	}

	public void runWithProgress(IProgressRunnable runnable) {
		try {
			Object r = runnable.run(progressMonitor);
			setResult(r);
			setStatus(Status.OK_STATUS);
		} catch (InterruptedException e) {
			cancel();
			setStatus(Status.CANCEL_STATUS);
		} catch (Exception e) {
			setException(e);
			setStatus(new Status(IStatus.ERROR, "org.eclipse.ecf", "Execution failed", e)); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}
}

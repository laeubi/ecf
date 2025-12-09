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
	private static final String BUNDLE_ID = "org.eclipse.ecf"; //$NON-NLS-1$
	
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
		long startTime = System.nanoTime();
		long waitTimeNanos = waitTimeInMillis * 1_000_000L;
		long remainingNanos = waitTimeNanos;
		while (!done && !canceled && remainingNanos > 0) {
			long waitMillis = remainingNanos / 1_000_000L;
			int waitNanos = (int) (remainingNanos % 1_000_000L);
			wait(waitMillis, waitNanos);
			long elapsed = System.nanoTime() - startTime;
			remainingNanos = waitTimeNanos - elapsed;
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
				return new Status(IStatus.ERROR, BUNDLE_ID, "Execution failed", exception); //$NON-NLS-1$
			}
			return Status.OK_STATUS;
		}
		if (canceled) {
			return Status.CANCEL_STATUS;
		}
		return new Status(IStatus.INFO, BUNDLE_ID, "Running"); //$NON-NLS-1$
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
			setStatus(new Status(IStatus.ERROR, BUNDLE_ID, "Execution failed", e)); //$NON-NLS-1$
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}
}

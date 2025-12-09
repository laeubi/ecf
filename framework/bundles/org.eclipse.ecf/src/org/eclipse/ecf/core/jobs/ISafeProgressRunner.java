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

/**
 * Interface for running progress runnables safely.
 * This interface is a replacement for org.eclipse.equinox.concurrent.future.ISafeProgressRunner
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public interface ISafeProgressRunner {
	/**
	 * Run the given progress runnable.
	 * 
	 * @param runnable the runnable to run. Must not be null.
	 */
	void runWithProgress(IProgressRunnable runnable);
}

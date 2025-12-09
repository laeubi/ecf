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
 * Interface for a runnable that supports progress monitoring.
 * This interface is a replacement for org.eclipse.equinox.concurrent.future.IProgressRunnable
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public interface IProgressRunnable {
	/**
	 * Run the operation with the given progress monitor.
	 * 
	 * @param monitor the progress monitor to use. May be null.
	 * @return Object the result of the operation. May be null.
	 * @throws Exception if the operation fails
	 */
	Object run(IProgressMonitor monitor) throws Exception;
}

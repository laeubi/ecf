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
 * Future implementation for a single operation.
 * This class is a replacement for org.eclipse.equinox.concurrent.future.SingleOperationFuture
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public class SingleOperationFuture extends AbstractFuture {
	public SingleOperationFuture(IProgressMonitor progressMonitor) {
		super(progressMonitor);
	}
}

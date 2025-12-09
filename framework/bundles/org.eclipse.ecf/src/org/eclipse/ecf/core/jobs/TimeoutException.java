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
 * Exception thrown when an operation times out.
 * This class is a replacement for org.eclipse.equinox.concurrent.future.TimeoutException
 * to eliminate the dependency on org.eclipse.equinox.concurrent.
 * 
 * @since 3.13
 */
public class TimeoutException extends Exception {
	private static final long serialVersionUID = 1L;

	public TimeoutException() {
		super();
	}

	public TimeoutException(String message) {
		super(message);
	}

	public TimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public TimeoutException(Throwable cause) {
		super(cause);
	}
}

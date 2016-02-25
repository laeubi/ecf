/*******************************************************************************
* Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.remoteservice.client;

import java.util.*;
import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.remoteservice.*;
import org.eclipse.ecf.remoteservice.events.IRemoteServiceRegisteredEvent;
import org.eclipse.ecf.remoteservice.util.EndpointDescriptionPropertiesUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @since 8.9
 */
public abstract class AbstractRSAClientContainer extends AbstractClientContainer implements IRSAConsumerContainerAdapter {

	public AbstractRSAClientContainer(ID containerID) {
		super(containerID);
	}

	public boolean setRemoteServiceCallPolicy(IRemoteServiceCallPolicy policy) {
		return false;
	}

	public Namespace getConnectNamespace() {
		return getID().getNamespace();
	}

	private Long getServiceId(Map<String, Object> endpointDescriptionProperties) {
		return EndpointDescriptionPropertiesUtil.verifyLongProperty(endpointDescriptionProperties, "endpoint.service.id"); //$NON-NLS-1$
	}

	private String getRemoteServiceFilter(Map<String, Object> endpointDescriptionProperties, Long rsId) {
		String edRsFilter = EndpointDescriptionPropertiesUtil.verifyStringProperty(endpointDescriptionProperties, "ecf.endpoint.rsfilter"); //$NON-NLS-1$
		// If it's *still* zero, then just use the raw filter
		if (rsId == 0)
			// It's not known...so we just return the 'raw' remote service
			// filter
			return edRsFilter;
		// It's a real remote service id...so we return
		StringBuffer result = new StringBuffer("(&(") //$NON-NLS-1$
				.append(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID).append("=").append(rsId).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (edRsFilter != null)
			result.append(edRsFilter);
		result.append(")"); //$NON-NLS-1$
		return result.toString();
	}

	protected void connectToEndpoint(ID connectTargetID) throws ContainerConnectException {
		connect(connectTargetID, connectContext);
	}

	protected IRemoteCallable[][] createRegistrationCallables(ID targetID, String[] interfaces, Dictionary endpointDescriptionProperties) {
		return new IRemoteCallable[][] {{RemoteCallableFactory.createCallable(getID().getName())}};
	}

	public class RSAClientRegistration extends RemoteServiceClientRegistration {
		public RSAClientRegistration(ID targetID, String[] classNames, IRemoteCallable[][] restCalls, Dictionary properties) {
			super(getConnectNamespace(), classNames, restCalls, properties, AbstractRSAClientContainer.this.registry);
			this.containerId = targetID;
			this.serviceID = new RemoteServiceID(getConnectNamespace(), this.containerId, (Long) properties.get(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID));
		}
	}

	protected Dictionary createRegistrationProperties(Map<String, Object> endpointDescriptionProperties) {
		return EndpointDescriptionPropertiesUtil.createDictionaryFromMap(endpointDescriptionProperties);
	}

	protected RemoteServiceClientRegistration createRSAClientRegistration(ID targetID, String[] interfaces, Map<String, Object> endpointDescriptionProperties) {
		Dictionary d = createRegistrationProperties(endpointDescriptionProperties);
		return new RSAClientRegistration(targetID, interfaces, createRegistrationCallables(targetID, interfaces, d), d);
	}

	public RemoteServiceClientRegistration registerEndpoint(ID targetID, String[] interfaces, Map<String, Object> endpointDescriptionProperties) {
		final RemoteServiceClientRegistration registration = createRSAClientRegistration(targetID, interfaces, endpointDescriptionProperties);
		this.registry.registerRegistration(registration);
		// notify
		fireRemoteServiceEvent(new IRemoteServiceRegisteredEvent() {

			public IRemoteServiceReference getReference() {
				return registration.getReference();
			}

			public ID getLocalContainerID() {
				return registration.getContainerID();
			}

			public ID getContainerID() {
				return getID();
			}

			public String[] getClazzes() {
				return registration.getClazzes();
			}
		});
		return registration;
	}

	public IRemoteServiceReference[] importEndpoint(Map<String, Object> endpointDescriptionProperties) throws ContainerConnectException, InvalidSyntaxException {
		// ecf.endpoint.id
		String ecfid = EndpointDescriptionPropertiesUtil.verifyStringProperty(endpointDescriptionProperties, "ecf.endpoint.id"); //$NON-NLS-1$
		if (ecfid == null)
			ecfid = EndpointDescriptionPropertiesUtil.verifyStringProperty(endpointDescriptionProperties, "endpoint.id"); //$NON-NLS-1$
		// ecf.endpoint.ts
		Long timestamp = EndpointDescriptionPropertiesUtil.verifyLongProperty(endpointDescriptionProperties, "ecf.endpoint.ts"); //$NON-NLS-1$
		if (timestamp == null)
			timestamp = getServiceId(endpointDescriptionProperties);
		// ecf.endpoint.ns
		String idNamespace = EndpointDescriptionPropertiesUtil.verifyStringProperty(endpointDescriptionProperties, "ecf.endpoint.id.ns"); //$NON-NLS-1$
		// Create/verify endpointContainerID
		ID endpointContainerID = EndpointDescriptionPropertiesUtil.verifyIDProperty(idNamespace, ecfid);
		// Get rsId
		Long rsId = EndpointDescriptionPropertiesUtil.verifyLongProperty(endpointDescriptionProperties, Constants.SERVICE_ID);
		// if null, then set to service.id
		if (rsId == null)
			rsId = EndpointDescriptionPropertiesUtil.verifyLongProperty(endpointDescriptionProperties, "endpoint.service.id"); //$NON-NLS-1$
		// Get connectTargetID
		ID connectTargetID = EndpointDescriptionPropertiesUtil.verifyIDProperty(idNamespace, EndpointDescriptionPropertiesUtil.verifyStringProperty(endpointDescriptionProperties, "ecf.endpoint.connecttarget.id")); //$NON-NLS-1$
		// If not explicitly set, then set to endpointContainerID
		if (connectTargetID == null)
			connectTargetID = endpointContainerID;
		// Get idFilter
		ID[] idFilter = EndpointDescriptionPropertiesUtil.verifyIDArray(endpointDescriptionProperties, "ecf.endpoint.idfilter.ids", idNamespace); //$NON-NLS-1$
		// If not set, then set to endpointContainerID
		idFilter = (idFilter == null) ? new ID[] {endpointContainerID} : idFilter;
		// Get rsFilter
		String rsFilter = getRemoteServiceFilter(endpointDescriptionProperties, rsId);
		// Get interfaces
		List<String> interfaces = EndpointDescriptionPropertiesUtil.verifyObjectClassProperty(endpointDescriptionProperties);
		// register locally
		registerEndpoint(connectTargetID, interfaces.toArray(new String[interfaces.size()]), endpointDescriptionProperties);
		// If we have a non-null targetID we connect 
		if (connectTargetID != null)
			connectToEndpoint(connectTargetID);
		return getRemoteServiceReferences(idFilter, interfaces.iterator().next(), rsFilter);
	}

	@Override
	protected abstract IRemoteService createRemoteService(RemoteServiceClientRegistration registration);

	@Override
	protected String prepareEndpointAddress(IRemoteCall call, IRemoteCallable callable) {
		return null;
	}

}

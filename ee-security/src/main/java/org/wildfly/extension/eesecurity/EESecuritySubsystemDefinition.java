/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.wildfly.extension.eesecurity;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 * @author Stuart Douglas
 */
public class EESecuritySubsystemDefinition extends PersistentResourceDefinition {

    static final String EE_SECURITY_CAPABILITY_NAME = "org.wildfly.ee.security";
    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    static final String ELYTRON_JAKARTA_SECURITY = "org.wildfly.security.jakarta.security";

    static final RuntimeCapability<Void> EE_SECURITY_CAPABILITY =
            RuntimeCapability.Builder.of(EE_SECURITY_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    EESecuritySubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(EESecurityExtension.SUBSYSTEM_PATH, EESecurityExtension.getResolver())
                .setAddHandler(EESecuritySubsystemAdd.INSTANCE)
                .addCapabilities(EE_SECURITY_CAPABILITY)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setAdditionalPackages(RuntimePackageDependency.required(ELYTRON_JAKARTA_SECURITY))
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }
}

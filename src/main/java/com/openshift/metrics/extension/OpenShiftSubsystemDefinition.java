package com.openshift.metrics.extension;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

public class OpenShiftSubsystemDefinition extends SimpleResourceDefinition {
    public static final OpenShiftSubsystemDefinition INSTANCE = new OpenShiftSubsystemDefinition();

    private OpenShiftSubsystemDefinition() {
        super(OpenShiftSubsystemExtension.SUBSYSTEM_PATH,
                OpenShiftSubsystemExtension.getResourceDescriptionResolver(),
                OpenShiftSubsystemAdd.INSTANCE,
                OpenShiftSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(MetricsGroupDefinition.INSTANCE);
    }
}

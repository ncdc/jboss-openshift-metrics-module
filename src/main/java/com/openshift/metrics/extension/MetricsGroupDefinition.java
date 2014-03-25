package com.openshift.metrics.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

public class MetricsGroupDefinition extends SimpleResourceDefinition {
    // subsystem=metrics/schedule="0 * * * * *"
    public static final PathElement SCHEDULE_PATH = PathElement.pathElement(Constants.METRICS_GROUP);

    public static final MetricsGroupDefinition INSTANCE = new MetricsGroupDefinition();

    private MetricsGroupDefinition() {
        super(SCHEDULE_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP), MetricsGroupAddHandler.INSTANCE, MetricsGroupRemoveHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(SourceDefinition.INSTANCE);
    }
}

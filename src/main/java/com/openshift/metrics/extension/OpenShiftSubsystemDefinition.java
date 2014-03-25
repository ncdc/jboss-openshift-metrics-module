package com.openshift.metrics.extension;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

public class OpenShiftSubsystemDefinition extends SimpleResourceDefinition {
    public static final OpenShiftSubsystemDefinition INSTANCE = new OpenShiftSubsystemDefinition();

    protected static final SimpleAttributeDefinition METRICS_GROUP =
            new SimpleAttributeDefinitionBuilder(Constants.METRICS_GROUP, ModelType.LIST)
                .setAllowExpression(false)
                .setXmlName(Constants.METRICS_GROUP)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .setAllowNull(false)
                .build();

    private OpenShiftSubsystemDefinition() {
        super(OpenShiftSubsystemExtension.SUBSYSTEM_PATH,
                OpenShiftSubsystemExtension.getResourceDescriptionResolver(),
                OpenShiftSubsystemAdd.INSTANCE,
                OpenShiftSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(ScheduleDefinition.INSTANCE);
    }
}

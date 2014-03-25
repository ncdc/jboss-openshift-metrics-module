package com.openshift.metrics.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

public class MetricDefinition extends SimpleResourceDefinition {
    public static final PathElement METRIC_PATH = PathElement.pathElement(Constants.METRIC);

    public static final MetricDefinition INSTANCE = new MetricDefinition();

    protected static final SimpleAttributeDefinition SOURCE_KEY =
            new SimpleAttributeDefinitionBuilder("source-key", ModelType.STRING)
                .setAllowExpression(false)
                .setXmlName("source-key")
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .setAllowNull(false)
                .build();

    private MetricDefinition() {
        super(METRIC_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE, Constants.METRIC), MetricAddHandler.INSTANCE, MetricRemoveHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(SOURCE_KEY, null);
    }
}

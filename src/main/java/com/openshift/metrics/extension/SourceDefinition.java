package com.openshift.metrics.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class SourceDefinition extends SimpleResourceDefinition {
    public static final PathElement SOURCE_PATH = PathElement.pathElement("source");
    
    public static final SourceDefinition INSTANCE = new SourceDefinition();
    
    protected static final SimpleAttributeDefinition MBEAN = 
            new SimpleAttributeDefinitionBuilder("mbean", ModelType.BOOLEAN)
                .setAllowExpression(false)
                .setXmlName("mbean")
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .setAllowNull(false)
                .setDefaultValue(new ModelNode().set(false))
                .build();
    
    private SourceDefinition() {
        super(SOURCE_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver("schedule", "source"), SourceAddHandler.INSTANCE, SourceRemoveHandler.INSTANCE);
    }
    
    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(MBEAN, null);
    }
    
    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(MetricDefinition.INSTANCE);
    }
}

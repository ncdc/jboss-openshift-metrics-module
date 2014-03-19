package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 */
public class OpenShiftSubsystemDefinition extends SimpleResourceDefinition {
    public static final OpenShiftSubsystemDefinition INSTANCE = new OpenShiftSubsystemDefinition();

    private static final OperationDefinition ADD_SCHEDULE =
    		new SimpleOperationDefinitionBuilder(ADD, OpenShiftSubsystemExtension.getResourceDescriptionResolver("schedule")).build();
    
    private OpenShiftSubsystemDefinition() {
        super(OpenShiftSubsystemExtension.SUBSYSTEM_PATH,
                OpenShiftSubsystemExtension.getResourceDescriptionResolver(null),
                //We always need to add an 'add' operation
                OpenShiftSubsystemAdd.INSTANCE,
                //Every resource that is added, normally needs a remove operation
                OpenShiftSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }
    
    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
    	super.registerChildren(resourceRegistration);
    	resourceRegistration.registerSubModel(ScheduleDefinition.INSTANCE);
    }
}

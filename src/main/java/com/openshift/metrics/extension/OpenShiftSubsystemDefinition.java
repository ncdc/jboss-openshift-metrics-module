package com.openshift.metrics.extension;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 */
public class OpenShiftSubsystemDefinition extends SimpleResourceDefinition {
    public static final OpenShiftSubsystemDefinition INSTANCE = new OpenShiftSubsystemDefinition();

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
        //you can register aditional operations here
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //you can register attributes here
    }
}

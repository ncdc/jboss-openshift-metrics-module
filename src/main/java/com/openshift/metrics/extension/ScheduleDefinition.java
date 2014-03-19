package com.openshift.metrics.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

public class ScheduleDefinition extends SimpleResourceDefinition {
	// subsystem=metrics/schedule="0 * * * * *"
	public static final PathElement SCHEDULE_PATH = PathElement.pathElement("schedule");

	public static final ScheduleDefinition INSTANCE = new ScheduleDefinition();
	
	private ScheduleDefinition() {
		super(SCHEDULE_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver("schedule"), ScheduleAddHandler.INSTANCE, ScheduleRemoveHandler.INSTANCE);
	}
	
	@Override
	public void registerChildren(ManagementResourceRegistration resourceRegistration) {
		super.registerChildren(resourceRegistration);
		resourceRegistration.registerSubModel(SourceDefinition.INSTANCE);
	}
}

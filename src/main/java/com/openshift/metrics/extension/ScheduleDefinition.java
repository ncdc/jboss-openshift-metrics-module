package com.openshift.metrics.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.quartz.SchedulerException;

public class ScheduleDefinition extends SimpleResourceDefinition {
	// subsystem=metrics/schedule="0 * * * * *"
	public static final PathElement SCHEDULE_PATH = PathElement.pathElement("schedule");

	public static final ScheduleDefinition INSTANCE = new ScheduleDefinition();
	
//	protected static final SimpleAttributeDefinition CRON =
//			new SimpleAttributeDefinitionBuilder("schedule", ModelType.STRING)
//				.setAllowExpression(false)
//				.setXmlName("schedule")
//				.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
//				.setAllowNull(false)
//				.setDefaultValue(new ModelNode("0 * * * * ?"))
//				.build();
	
	private ScheduleDefinition() {
		super(SCHEDULE_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver("schedule"), ScheduleAddHandler.INSTANCE, ScheduleRemoveHandler.INSTANCE);
	}
	
	@Override
	public void registerOperations(ManagementResourceRegistration resourceRegistration) {
		super.registerOperations(resourceRegistration);
	}
	
	@Override
	public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
//		resourceRegistration.registerReadWriteAttribute(CRON, null, MetricScheduleHandler.INSTANCE);
	}
	
	@Override
	public void registerChildren(ManagementResourceRegistration resourceRegistration) {
		super.registerChildren(resourceRegistration);
		resourceRegistration.registerSubModel(SourceDefinition.INSTANCE);
	}
	
//	static class MetricScheduleHandler extends AbstractWriteAttributeHandler<Void> {
//		public static final MetricScheduleHandler INSTANCE = new MetricScheduleHandler();
//
//		@Override
//		protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder) throws OperationFailedException {
//			modify(context, operation, currentValue.asString(), resolvedValue.asString());
//			return false;
//		}
//
//		private void modify(OperationContext context, ModelNode operation, String oldValue, String newValue) {
//			MetricsService service = (MetricsService)context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
//			try {
////				if(oldValue != null) {
////					service.removeSchedule(oldValue);
////				}
//				service.addSchedule(newValue);
//			} catch (SchedulerException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//		@Override
//		protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
//			modify(context, operation, valueToRevert.asString(), valueToRestore.asString());
//		}
//	}
}

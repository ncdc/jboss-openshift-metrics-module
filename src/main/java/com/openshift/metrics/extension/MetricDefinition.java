package com.openshift.metrics.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class MetricDefinition extends SimpleResourceDefinition {
	public static final PathElement METRIC_PATH = PathElement.pathElement("metric");
	
	public static final MetricDefinition INSTANCE = new MetricDefinition();
	
	protected static final SimpleAttributeDefinition KEY = 
			new SimpleAttributeDefinitionBuilder("key", ModelType.STRING)
				.setAllowExpression(false)
				.setXmlName("key")
				.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
				.setAllowNull(false)
				.build();
	
//	protected static final SimpleAttributeDefinition PUBLISH_NAME =
//			new SimpleAttributeDefinitionBuilder("publish-name", ModelType.STRING)
//				.setAllowExpression(false)
//				.setXmlName("publish-name")
//				.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
//				.setAllowNull(false)
//				.build();
	
	private MetricDefinition() {
		super(METRIC_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver("metric"), MetricAddHandler.INSTANCE, MetricRemoveHandler.INSTANCE);
	}
	
	@Override
	public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
		resourceRegistration.registerReadWriteAttribute(KEY, null, MetricKeyHandler.INSTANCE);
//		resourceRegistration.registerReadWriteAttribute(PUBLISH_NAME, null, MetricPublishNameHandler.INSTANCE);
	}
	
	static class MetricKeyHandler extends AbstractWriteAttributeHandler<Void> {
		public static final MetricKeyHandler INSTANCE = new MetricKeyHandler();

		@Override
		protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder) throws OperationFailedException {
			modify(context, operation, currentValue.asString(), resolvedValue.asString());
			return false;
		}

		@Override
		protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
			modify(context, operation, valueToRevert.asString(), valueToRestore.asString());
		}
		
		private void modify(OperationContext context, ModelNode operation, String oldValue, String newValue) {
			MetricsService service = (MetricsService)context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
		}
	}
	
//	static class MetricPublishNameHandler extends AbstractWriteAttributeHandler<Void> {
//		public static final MetricPublishNameHandler INSTANCE = new MetricPublishNameHandler();
//
//		@Override
//		protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder) throws OperationFailedException {
//			modify(context, operation, resolvedValue.asString());
//			return false;
//		}
//
//		private void modify(OperationContext context, ModelNode operation, String value) {
//		}
//
//		@Override
//		protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
//		}
//	}
}

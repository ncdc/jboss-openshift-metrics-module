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

public class SourceDefinition extends SimpleResourceDefinition {
	public static final PathElement SOURCE_PATH = PathElement.pathElement("source");
	
	public static final SourceDefinition INSTANCE = new SourceDefinition();
	
	protected static final SimpleAttributeDefinition NODE = 
			new SimpleAttributeDefinitionBuilder("node", ModelType.STRING)
				.setAllowExpression(false)
				.setXmlName("node")
				.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
				.setAllowNull(false)
				.build();
	
	private SourceDefinition() {
		super(SOURCE_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver("source"), SourceAddHandler.INSTANCE, SourceRemoveHandler.INSTANCE);
	}
	
	@Override
	public void registerOperations(ManagementResourceRegistration resourceRegistration) {
		super.registerOperations(resourceRegistration);
	}
	
	@Override
	public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
//		resourceRegistration.registerReadWriteAttribute(NODE, null, SourceNodeHandler.INSTANCE);
	}
	
	@Override
	public void registerChildren(ManagementResourceRegistration resourceRegistration) {
		super.registerChildren(resourceRegistration);
		resourceRegistration.registerSubModel(MetricDefinition.INSTANCE);
	}
	
	static class SourceNodeHandler extends AbstractWriteAttributeHandler<Void> {
		public static final SourceNodeHandler INSTANCE = new SourceNodeHandler();

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
}

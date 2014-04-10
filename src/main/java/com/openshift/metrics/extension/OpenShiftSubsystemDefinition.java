package com.openshift.metrics.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.quartz.SchedulerException;

public class OpenShiftSubsystemDefinition extends SimpleResourceDefinition {
    public static final OpenShiftSubsystemDefinition INSTANCE = new OpenShiftSubsystemDefinition();

    protected static final SimpleAttributeDefinition MAX_LINE_LENGTH =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_LINE_LENGTH, ModelType.INT)
                .setAllowExpression(false)
                .setXmlName(Constants.MAX_LINE_LENGTH)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .setAllowNull(true)
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
        resourceRegistration.registerSubModel(MetricsGroupDefinition.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(MAX_LINE_LENGTH, null, MaxLineLengthHandler.INSTANCE);
    }

    static class MaxLineLengthHandler extends AbstractWriteAttributeHandler<Boolean> {
        public static final MaxLineLengthHandler INSTANCE = new MaxLineLengthHandler();

        private MaxLineLengthHandler() {
            super(OpenShiftSubsystemDefinition.MAX_LINE_LENGTH);
        }

        @Override
        protected boolean applyUpdateToRuntime(
                OperationContext context,
                ModelNode operation,
                String attributeName,
                ModelNode resolvedValue,
                ModelNode currentValue,
                org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Boolean> handbackHolder)
                throws OperationFailedException {

            modify(context, operation, resolvedValue.asInt());

            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context,
                ModelNode operation, String attributeName,
                ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback)
                throws OperationFailedException {

            modify(context, operation, valueToRestore.asInt());
        }

        private void modify(OperationContext context, ModelNode operation, Integer value) {
            try {
                MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
                service.setMaxLineLength(value);
            } catch (SchedulerException e) {
            }
        }

    }
}

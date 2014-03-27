package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.quartz.SchedulerException;

public class MetricsGroupDefinition extends SimpleResourceDefinition {
    // subsystem=metrics/metrics-group="0 * * * * *"
    public static final PathElement METRICS_GROUP_PATH = PathElement.pathElement(Constants.METRICS_GROUP);

    public static final MetricsGroupDefinition INSTANCE = new MetricsGroupDefinition();

    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder("enabled", ModelType.BOOLEAN)
                .setAllowExpression(false)
                .setXmlName("enabled")
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(true))
                .build();

    private MetricsGroupDefinition() {
        super(METRICS_GROUP_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP), MetricsGroupAddHandler.INSTANCE, MetricsGroupRemoveHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(SourceDefinition.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(ENABLED, null, EnabledHandler.INSTANCE);
    }

    static class EnabledHandler extends AbstractWriteAttributeHandler<Boolean> {
        public static final EnabledHandler INSTANCE = new EnabledHandler();

        private EnabledHandler() {
            super(MetricsGroupDefinition.ENABLED);
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

            modify(context, operation, resolvedValue.asBoolean());

            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context,
                ModelNode operation, String attributeName,
                ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback)
                throws OperationFailedException {

            modify(context, operation, valueToRestore.asBoolean());
        }

        private void modify(OperationContext context, ModelNode operation, boolean enabled) {
            MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
            ModelNode address = operation.require(OP_ADDR);
            String schedule = PathAddress.pathAddress(address).getLastElement().getValue();
            String cronExpression = Util.decodeCronExpression(schedule);
            try {
                if(enabled) {
                    service.enableJob(cronExpression);
                } else {
                    service.disableJob(cronExpression);
                }
            } catch (SchedulerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}

package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.text.MessageFormat;

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
        resourceRegistration.registerReadWriteAttribute(MetricsGroupDefinition.ENABLED, null, EnabledHandler.INSTANCE);
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

        private void modify(OperationContext context, ModelNode operation, boolean enabled) throws OperationFailedException {
            MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
            ModelNode address = operation.require(OP_ADDR);
            final String schedule = PathAddress.pathAddress(address).getElement(1).getValue();
            final String sourcePath = PathAddress.pathAddress(address).getElement(2).getValue();
            String publishKey = PathAddress.pathAddress(address).getElement(3).getValue();
            String cronExpression = Util.decodeCronExpression(schedule);
            try {
                service.enableMetric(cronExpression, sourcePath, publishKey, enabled);
            } catch (SchedulerException e) {
                String message = MessageFormat.format("Encountered exception trying to enable/disable metric[schedule={0}, source={1}, publishKey={2}, enabled={3}]", cronExpression, sourcePath, publishKey, enabled);
                throw new OperationFailedException(message, e);
            }
        }
    }
}

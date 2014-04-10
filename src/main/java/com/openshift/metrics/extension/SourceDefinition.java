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

public class SourceDefinition extends SimpleResourceDefinition {
    public static final PathElement SOURCE_PATH = PathElement.pathElement(Constants.SOURCE);

    public static final SourceDefinition INSTANCE = new SourceDefinition();

    protected static final SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.TYPE, ModelType.STRING)
                .setAllowExpression(false)
                .setXmlName(Constants.TYPE)
                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                .setAllowNull(false)
                .build();

    private SourceDefinition() {
        super(SOURCE_PATH, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE), SourceAddHandler.INSTANCE, SourceRemoveHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(TYPE, null);
        resourceRegistration.registerReadWriteAttribute(MetricsGroupDefinition.ENABLED, null, EnabledHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(MetricDefinition.INSTANCE);
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
            String cronExpression = Util.decodeCronExpression(schedule);
            try {
                service.enableMetricSource(cronExpression, sourcePath, enabled);
            } catch (SchedulerException e) {
                String message = MessageFormat.format("Encountered exception trying to enable/disable metric source[schedule={0}, source={1}, enabled={2}]", cronExpression, sourcePath, enabled);
                throw new OperationFailedException(message, e);
            }
        }

    }
}

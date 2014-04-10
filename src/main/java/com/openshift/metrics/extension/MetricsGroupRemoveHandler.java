package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.text.MessageFormat;
import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

public class MetricsGroupRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {
    public static final MetricsGroupRemoveHandler INSTANCE = new MetricsGroupRemoveHandler();

    public MetricsGroupRemoveHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(REMOVE, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP), (AttributeDefinition[])null);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        ModelNode address = operation.require(OP_ADDR);
        String schedule = PathAddress.pathAddress(address).getLastElement().getValue();
        final String cronExpression = Util.decodeCronExpression(schedule);
        try {
            service.removeJob(cronExpression);
        } catch (Exception e) {
            String message = MessageFormat.format("Encountered exception trying to remove metrics group[schedule={0}]", cronExpression);
            throw new OperationFailedException(message, e);
        }
    }
}

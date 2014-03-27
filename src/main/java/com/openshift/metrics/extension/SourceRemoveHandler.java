package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

public class SourceRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {
    public static final SourceRemoveHandler INSTANCE = new SourceRemoveHandler();

    private final Logger log = Logger.getLogger(SourceRemoveHandler.class);

    public SourceRemoveHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(REMOVE, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE), (AttributeDefinition[])null);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        final ModelNode address = operation.get(ModelDescriptionConstants.ADDRESS);
        final String schedule = PathAddress.pathAddress(address).getElement(1).getValue();
        final String source = PathAddress.pathAddress(address).getElement(2).getValue();
        final String cronExpression = Util.decodeCronExpression(schedule);
        try {
            service.removeMetricSource(cronExpression, source);
        } catch (Exception e) {
            log.errorv(e, "Encountered exception trying to add source[schedule={0}, path={1}]", cronExpression, source);
        }
    }
}

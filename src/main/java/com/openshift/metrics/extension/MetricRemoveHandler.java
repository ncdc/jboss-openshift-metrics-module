package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

public class MetricRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {
    public static final MetricRemoveHandler INSTANCE = new MetricRemoveHandler();

    private final Logger log = Logger.getLogger(MetricRemoveHandler.class);

    public MetricRemoveHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final StandardResourceDescriptionResolver resourceDescriptionResolver = OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE, Constants.METRIC);
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(REMOVE,
                resourceDescriptionResolver, MetricDefinition.SOURCE_KEY);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        final ModelNode address = operation.get(ModelDescriptionConstants.ADDRESS);
        final String schedule = PathAddress.pathAddress(address).getElement(1).getValue();
        final String source = PathAddress.pathAddress(address).getElement(2).getValue();
        String publishKey = PathAddress.pathAddress(address).getElement(3).getValue();
        final String cronExpression = Util.decodeCronExpression(schedule);
        try {
            service.removeMetric(cronExpression, source, publishKey);
        } catch (Exception e) {
            log.errorv(e, "Encountered exception trying to remove metric[schedule={0}, source={1}, publishKey={2}]", cronExpression, source, publishKey);
        }
    }
}

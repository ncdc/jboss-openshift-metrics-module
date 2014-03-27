package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

public class MetricAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final MetricAddHandler INSTANCE = new MetricAddHandler();

    private final Logger log = Logger.getLogger(MetricAddHandler.class);

    public MetricAddHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final StandardResourceDescriptionResolver resourceDescriptionResolver = OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE, Constants.METRIC);
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(ADD,
                resourceDescriptionResolver, MetricDefinition.SOURCE_KEY, MetricsGroupDefinition.ENABLED);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricDefinition.SOURCE_KEY.validateAndSet(operation, model);
        MetricsGroupDefinition.ENABLED.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        String key = MetricDefinition.SOURCE_KEY.resolveModelAttribute(context, model).asString();
        final String schedule = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(1).getValue();
        final String sourceKey = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
        String publishKey = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(3).getValue();
        boolean enabled = MetricsGroupDefinition.ENABLED.resolveModelAttribute(context, operation).asBoolean();
        final String cronExpression = Util.decodeCronExpression(schedule);
        try {
            service.addMetric(cronExpression, sourceKey, key, publishKey, enabled);
        } catch (Exception e) {
            log.errorv(e, "Encountered exception trying to add metric[schedule={0}, source={1}, sourceKey={2}, publishKey={3}, enabled={4}]", cronExpression, sourceKey, sourceKey, publishKey, enabled);
        }
    }
}

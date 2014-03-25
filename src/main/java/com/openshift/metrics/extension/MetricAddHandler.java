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
import org.jboss.msc.service.ServiceController;
import org.quartz.SchedulerException;

public class MetricAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final MetricAddHandler INSTANCE = new MetricAddHandler();

    public MetricAddHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final StandardResourceDescriptionResolver resourceDescriptionResolver = OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE, Constants.METRIC);
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(ADD,
                resourceDescriptionResolver, MetricDefinition.SOURCE_KEY);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricDefinition.SOURCE_KEY.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        String key = MetricDefinition.SOURCE_KEY.resolveModelAttribute(context, model).asString();
        // subsystem=metrics/schedule=0 * * * * */source=src/metric=publishName
        final String schedule = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(1).getValue();
        final String source = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
        String publishName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(3).getValue();
        try {
            service.addMetric(schedule, source, key, publishName);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

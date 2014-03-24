package com.openshift.metrics.extension;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.quartz.SchedulerException;

public class MetricAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final MetricAddHandler INSTANCE = new MetricAddHandler();

    public MetricAddHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricDefinition.KEY.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        String key = MetricDefinition.KEY.resolveModelAttribute(context, model).asString();
        // subsystem=metrics/schedule=0 * * * * */source=src/metric=publishName
        final String schedule = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(1).getValue();
        final String sourceString = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
        boolean mBean = SourceDefinition.MBEAN.resolveModelAttribute(context,operation).asBoolean();
        final Source source = new Source(sourceString, mBean);
        String publishName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(3).getValue();
        try {
            service.addMetric(schedule, source, key, publishName);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

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
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.quartz.SchedulerException;

public class SourceAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final SourceAddHandler INSTANCE = new SourceAddHandler();

    public SourceAddHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(ADD, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE), SourceDefinition.TYPE);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
//        model.get(Constants.METRIC).setEmptyList();
        model.get(Constants.TYPE).set(operation.get(Constants.TYPE));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        final String schedule = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(1).getValue();
        final String sourceString = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
        String type = SourceDefinition.TYPE.resolveModelAttribute(context,operation).asString();
        final Source source = new Source(sourceString, type);
        try {
            service.addMetricSource(schedule, source);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

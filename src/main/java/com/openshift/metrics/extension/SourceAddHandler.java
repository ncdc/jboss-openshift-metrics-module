package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.text.MessageFormat;
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

public class SourceAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final SourceAddHandler INSTANCE = new SourceAddHandler();

    public SourceAddHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(ADD, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP, Constants.SOURCE), SourceDefinition.TYPE, MetricsGroupDefinition.ENABLED);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        SourceDefinition.TYPE.validateAndSet(operation, model);;
        MetricsGroupDefinition.ENABLED.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        final ModelNode address = operation.get(ModelDescriptionConstants.ADDRESS);
        final String schedule = PathAddress.pathAddress(address).getElement(1).getValue();
        final String sourcePath = PathAddress.pathAddress(address).getElement(2).getValue();
        String type = SourceDefinition.TYPE.resolveModelAttribute(context,operation).asString();
        final boolean enabled = MetricsGroupDefinition.ENABLED.resolveModelAttribute(context, operation).asBoolean();
        final Source source = new Source(sourcePath, type, enabled);
        final String cronExpression = Util.decodeCronExpression(schedule);
        try {
            service.addMetricSource(cronExpression, source);
        } catch (Exception e) {
            String message = MessageFormat.format("Encountered exception trying to add source[schedule={0}, path={1}]", cronExpression, source);
            throw new OperationFailedException(message, e);
        }
    }
}

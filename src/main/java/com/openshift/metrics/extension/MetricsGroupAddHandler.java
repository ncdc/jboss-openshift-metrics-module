package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

public class MetricsGroupAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final MetricsGroupAddHandler INSTANCE = new MetricsGroupAddHandler();

    private final Logger log = Logger.getLogger(MetricsGroupAddHandler.class);

    public MetricsGroupAddHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(ADD, OpenShiftSubsystemExtension.getResourceDescriptionResolver(Constants.METRICS_GROUP), MetricsGroupDefinition.ENABLED);
        return delegate.getModelDescription(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricsGroupDefinition.ENABLED.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        ModelNode address = operation.require(OP_ADDR);
        String schedule = PathAddress.pathAddress(address).getLastElement().getValue();
        boolean enabled = MetricsGroupDefinition.ENABLED.resolveModelAttribute(context, operation).asBoolean();
        final String cronExpression = Util.decodeCronExpression(schedule);
        try {
            service.createJob(cronExpression, enabled);
        } catch (Exception e) {
            log.errorv(e, "Encountered exception trying to add metrics group[schedule={0}]", cronExpression);
        }
    }
}

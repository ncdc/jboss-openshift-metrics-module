package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.quartz.SchedulerException;

/**
 * Handler responsible for adding the subsystem resource to the model
 */
class OpenShiftSubsystemAdd extends AbstractAddStepHandler implements DescriptionProvider {
    private final Logger log = Logger.getLogger(OpenShiftSubsystemAdd.class);

    static final OpenShiftSubsystemAdd INSTANCE = new OpenShiftSubsystemAdd();

    private OpenShiftSubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();//get(Constants.METRICS_GROUP).setEmptyList();
    }

    /** {@inheritDoc} */
    @Override
    protected void performRuntime(org.jboss.as.controller.OperationContext context, ModelNode operation, ModelNode model, org.jboss.as.controller.ServiceVerificationHandler verificationHandler, java.util.List<org.jboss.msc.service.ServiceController<?>> newControllers) throws OperationFailedException {
        try {
            MetricsService service = new MetricsService();

            if(operation.hasDefined(Constants.MAX_LINE_LENGTH)) {
                Integer value = operation.get(Constants.MAX_LINE_LENGTH).asInt();
                try {
                    service.setMaxLineLength(value);
                } catch (SchedulerException e) {
                    log.warnv(e, "Error setting max line length to {0}: {1}", value, e.getMessage());
                }
            }

            ServiceController<MetricsService> controller = context.getServiceTarget()
                    .addService(MetricsService.getServiceName(), service)
                    .addDependency(DependencyType.REQUIRED,
                            Services.JBOSS_SERVER_CONTROLLER,
                            ModelController.class,
                            service.getInjectedModelController())
                    .addListener(verificationHandler)
                    .setInitialMode(Mode.ACTIVE)
                    .install();

            newControllers.add(controller);
        } catch (SchedulerException e) {
            throw new OperationFailedException("Error adding metrics subsystem", e);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        final DefaultOperationDescriptionProvider delegate = new DefaultOperationDescriptionProvider(ADD, OpenShiftSubsystemExtension.getResourceDescriptionResolver(), (AttributeDefinition[])null);
        return delegate.getModelDescription(locale);
    }
}

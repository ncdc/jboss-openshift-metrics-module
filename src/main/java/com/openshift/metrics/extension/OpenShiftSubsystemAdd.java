package com.openshift.metrics.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Handler responsible for adding the subsystem resource to the model
 */
class OpenShiftSubsystemAdd extends AbstractAddStepHandler{

    static final OpenShiftSubsystemAdd INSTANCE = new OpenShiftSubsystemAdd();

    private OpenShiftSubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get("schedule").setEmptyList();
    }

    /** {@inheritDoc} */
    @Override
    protected void performRuntime(org.jboss.as.controller.OperationContext context, ModelNode operation, ModelNode model, org.jboss.as.controller.ServiceVerificationHandler verificationHandler, java.util.List<org.jboss.msc.service.ServiceController<?>> newControllers) throws OperationFailedException {
		MetricsService service = new MetricsService();
		
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
    }
}

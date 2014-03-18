package com.openshift.metrics.extension;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Handler responsible for adding the subsystem resource to the model
 */
class OpenShiftSubsystemAdd extends AbstractBoottimeAddStepHandler{

    static final OpenShiftSubsystemAdd INSTANCE = new OpenShiftSubsystemAdd();

    private final Logger log = Logger.getLogger(OpenShiftSubsystemAdd.class);

    private OpenShiftSubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        log.info("Populating the model");
        model.get("metric").setEmptyObject();
    }

    /** {@inheritDoc} */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
		MetricsService service = new MetricsService();
    	ServiceName name = MetricsService.getServiceName();
    	ServiceController<MetricsService> controller = context.getServiceTarget()
    			.addService(name, service)
    			.addListener(verificationHandler)
    			.setInitialMode(Mode.ACTIVE)
    			.install();
    	newControllers.add(controller);
    }
}

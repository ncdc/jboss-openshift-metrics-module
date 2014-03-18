package com.openshift.metrics.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for removing the subsystem resource from the model
 */
class OpenShiftSubsystemRemove extends AbstractRemoveStepHandler {
    static final OpenShiftSubsystemRemove INSTANCE = new OpenShiftSubsystemRemove();

    private OpenShiftSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.removeService(MetricsService.getServiceName());
    }
}

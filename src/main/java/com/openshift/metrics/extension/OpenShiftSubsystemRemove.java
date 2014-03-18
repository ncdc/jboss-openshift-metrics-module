package com.openshift.metrics.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Handler responsible for removing the subsystem resource from the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class OpenShiftSubsystemRemove extends AbstractRemoveStepHandler {

    static final OpenShiftSubsystemRemove INSTANCE = new OpenShiftSubsystemRemove();

    private final Logger log = Logger.getLogger(OpenShiftSubsystemRemove.class);

    private OpenShiftSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        //Remove any services installed by the corresponding add handler here
        //context.removeService(ServiceName.of("some", "name"));
    }


}

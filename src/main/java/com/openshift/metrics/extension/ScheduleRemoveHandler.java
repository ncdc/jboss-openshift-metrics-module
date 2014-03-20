package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.quartz.SchedulerException;

public class ScheduleRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {
    public static final ScheduleRemoveHandler INSTANCE = new ScheduleRemoveHandler();
    
    public ScheduleRemoveHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
        ModelNode address = operation.require(OP_ADDR);
        String schedule = PathAddress.pathAddress(address).getLastElement().getValue();
        try {
            service.removeJob(schedule);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

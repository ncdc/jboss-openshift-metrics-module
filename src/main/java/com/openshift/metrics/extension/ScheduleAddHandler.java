package com.openshift.metrics.extension;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.quartz.SchedulerException;

public class ScheduleAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
	public static final ScheduleAddHandler INSTANCE = new ScheduleAddHandler();
	
	public ScheduleAddHandler() {
	}
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
		ScheduleDefinition.CRON.validateAndSet(operation, model);
	}

	@Override
	protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
		MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName());
		String schedule = MetricDefinition.SCHEDULE.resolveModelAttribute(context, model).asString();
		try {
			service.addSchedule(schedule);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

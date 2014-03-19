package com.openshift.metrics.extension;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.quartz.SchedulerException;

public class SourceRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {
	public static final SourceRemoveHandler INSTANCE = new SourceRemoveHandler();
	
	public SourceRemoveHandler() {
	}
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
		MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
		final String schedule = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(1).getValue();
		final String sourceString = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
		boolean mBean = SourceDefinition.MBEAN.resolveModelAttribute(context,model).asBoolean();
		final Source source = new Source(sourceString, mBean);
		try {
			service.removeMetricSource(schedule, source);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

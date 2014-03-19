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

public class MetricRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {
	public static final MetricRemoveHandler INSTANCE = new MetricRemoveHandler();
	
	public MetricRemoveHandler() {
	}
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
		MetricsService service = (MetricsService) context.getServiceRegistry(true).getRequiredService(MetricsService.getServiceName()).getValue();
		String key = MetricDefinition.KEY.resolveModelAttribute(context, model).asString();
		// subsystem=metrics/schedule=0 * * * * */source=src/metric=publishName
		final String schedule = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(1).getValue();
		final String sourceString = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(2).getValue();
		boolean mBean = SourceDefinition.MBEAN.resolveModelAttribute(context,model).asBoolean();
		final Source source = new Source(sourceString, mBean);
		String publishName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getElement(3).getValue();
		try {
			service.removeMetric(schedule, source, key, publishName);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

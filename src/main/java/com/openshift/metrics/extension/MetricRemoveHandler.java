package com.openshift.metrics.extension;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

public class MetricRemoveHandler extends AbstractAddStepHandler implements DescriptionProvider {
	public static final MetricRemoveHandler INSTANCE = new MetricRemoveHandler();
	
	public MetricRemoveHandler() {
	}
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void populateModel(ModelNode operation, ModelNode model)
			throws OperationFailedException {
		// TODO Auto-generated method stub

	}

}

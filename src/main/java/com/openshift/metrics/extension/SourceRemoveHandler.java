package com.openshift.metrics.extension;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

public class SourceRemoveHandler extends AbstractAddStepHandler implements DescriptionProvider {
	public static final SourceRemoveHandler INSTANCE = new SourceRemoveHandler();
	
	public SourceRemoveHandler() {
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

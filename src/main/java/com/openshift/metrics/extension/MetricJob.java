package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

public class MetricJob implements Job {
	private final Logger log = Logger.getLogger(MetricJob.class);
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		ModelControllerClient modelControllerClient = null;
		try {
			modelControllerClient = (ModelControllerClient) context.getScheduler().getContext().get("modelControllerClient");
		} catch (SchedulerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(null == modelControllerClient) {
			//TODO log or something?
			return;
		}
		
		final JobDataMap jobDataMap = context.getMergedJobDataMap();

		Map<String, Map<String, String>> sources = (Map<String, Map<String, String>>) jobDataMap.get("metricSources");
		
		for (String source : sources.keySet()) {
			ModelNode op = new ModelNode();
			op.get(OP).set(READ_ATTRIBUTE_OPERATION);
			
			ModelNode address = op.get(OP_ADDR);
			StringTokenizer tokenizer = new StringTokenizer(source, "/");
			while(tokenizer.hasMoreTokens()) {
				String[] kv = tokenizer.nextToken().split("=");
				address.add(kv[0], kv[1]);
			}
			
			Map<String, String> metrics = sources.get(source);
			for(String originalKey : metrics.keySet()) {
				String key = originalKey;
				String subkey = null;
				
				//TODO consider supporting indexing into arrays
//				final int leftBracketIndex = key.indexOf('[');
				
				final int dotIndex = key.indexOf('.');
				if(dotIndex > -1) {
					subkey = key.substring(dotIndex + 1);
					key = key.substring(0, dotIndex);
				}
				
				op.get(ModelDescriptionConstants.NAME).set(key);
				try {
					ModelNode r = modelControllerClient.execute(op);
					ModelNode result = r.get(ModelDescriptionConstants.RESULT);
					if(subkey != null) {
						result = result.get(subkey);
					}
					log.info(metrics.get(originalKey) + "=" + result.asString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
}

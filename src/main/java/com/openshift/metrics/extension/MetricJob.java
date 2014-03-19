package com.openshift.metrics.extension;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

public class MetricJob implements Job {
	public void execute(JobExecutionContext context) throws JobExecutionException {
		ModelControllerClient modelControllerClient = null;
		try {
			modelControllerClient = (ModelControllerClient) context.getScheduler().getContext().get("modelControllerClient");
		} catch (SchedulerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(null == modelControllerClient) {
			return;
		}
		
		final JobDataMap jobDataMap = context.getMergedJobDataMap();
		Map<String, List<Metric>> sources = (Map<String, List<Metric>>) jobDataMap.get("metricSources");
		for (String source : sources.keySet()) {
			ModelNode op = new ModelNode();
			op.get("operation").set("read-attribute");
			
			ModelNode address = op.get("address");
			StringTokenizer tokenizer = new StringTokenizer(source, "/");
			while(tokenizer.hasMoreTokens()) {
				String[] kv = tokenizer.nextToken().split("=");
				address.add(kv[0], kv[1]);
			}
			
			List<Metric> metrics = sources.get(source);
			for(Metric metric : metrics) {
				String key = metric.getKey();
				String subkey = null;
				final int dotIndex = key.indexOf('.');
				final int leftBracketIndex = key.indexOf('[');
				if(dotIndex > -1) {
					subkey = key.substring(dotIndex + 1);
					key = key.substring(0, dotIndex);
				}
				
				op.get("name").set(key);
				ModelNode r;
				try {
					r = modelControllerClient.execute(op);
					ModelNode result = r.get("result");
					if(subkey != null) {
						result = result.get(subkey);
					}
					System.out.println(address.asString() + "|" + metric.getKey() + "=" + result.asString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
}

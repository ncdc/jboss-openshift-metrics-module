package com.openshift.metrics.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

public class MetricsService implements Service<MetricsService> {
	private Scheduler scheduler;
	private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();
	private ModelControllerClient modelControllerClient;
	private ExecutorService managementOperationExecutor;
	
	public MetricsService() {
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
		
			managementOperationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
	            @Override
	            public Thread newThread(Runnable run) {
	                Thread thread = new Thread(run);
	                thread.setName("Metrics Management Client Thread");
	                thread.setDaemon(true);
	                return thread;
	            }
	        });
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public MetricsService getValue() throws IllegalStateException, IllegalArgumentException {
		return this;
	}

	@Override
	public void start(StartContext context) throws StartException {
		try {
			modelControllerClient = injectedModelController.getValue().createClient(managementOperationExecutor);
			
			scheduler.getContext().put("modelControllerClient", modelControllerClient);
			
			scheduler.start();
		} catch(SchedulerException e) {
			throw new StartException("Error starting scheduler", e);
		}
	}

	@Override
	public void stop(StopContext context) {
		try {
			scheduler.shutdown();
			managementOperationExecutor.shutdown();
		} catch (SchedulerException e) {
			//TODO log error
		}
	}

	public static ServiceName getServiceName() {
		return ServiceName.JBOSS.append("metrics");
	}

	public JobDetail getJobDetail(String schedule) throws SchedulerException {
		return scheduler.getJobDetail(JobKey.jobKey(schedule));
	}
	
	public JobDetail createJob(String schedule) throws SchedulerException {
		JobDetail job = JobBuilder.newJob(MetricJob.class)
								  .withIdentity(schedule)
								  .storeDurably()
								  .build();
		
		scheduler.addJob(job, false);
		return job;
	}
	
	public void removeJob(String schedule) throws SchedulerException {
		scheduler.deleteJob(JobKey.jobKey(schedule));
	}
	
	public void addMetricSource(String schedule, String source) throws SchedulerException {
		final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<String, Map<String, String>> metricSourceMap = (Map<String, Map<String, String>>) jobDataMap.get("metricSources");
		if(null == metricSourceMap) {
			metricSourceMap = new HashMap<String, Map<String, String>>();
			jobDataMap.put("metricSources", metricSourceMap);
		}
		metricSourceMap.put(source, new HashMap<String, String>());
		scheduler.addJob(job, true);
	}
	
	public void removeMetricSource(String schedule, String source) throws SchedulerException {
		final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<String,Map<String, String>> metricSourceMap = (Map<String, Map<String, String>>) jobDataMap.get("metricSources");
		metricSourceMap.remove(source);
		scheduler.addJob(job, true);
	}
	
	public void addMetric(String schedule, String source, String key, String publishName) throws SchedulerException {
		JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<String,Map<String, String>> metricSourceMap = (Map<String, Map<String, String>>) jobDataMap.get("metricSources");
		final Map<String, String> metrics = metricSourceMap.get(source);
		metrics.put(key, publishName);
		
		scheduler.addJob(job, true);

		TriggerKey triggerKey = TriggerKey.triggerKey(schedule);
		
		if(!scheduler.checkExists(triggerKey)) {
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerKey)
					.forJob(job)
					.withSchedule(CronScheduleBuilder.cronSchedule(schedule))
					.build();
			
			scheduler.scheduleJob(trigger);
		}
	}
	
	public void removeMetric(String schedule, String source, String key, String publishName) throws SchedulerException {
		JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<String,Map<String, String>> metricSourceMap = (Map<String, Map<String, String>>) jobDataMap.get("metricSources");
		final Map<String, String> metrics = metricSourceMap.get(source);
		metrics.remove(key);
		
		scheduler.addJob(job, true);
	}
	
	public ModelControllerClient getModelControllerClient() {
		return modelControllerClient;
	}
	
	public InjectedValue<ModelController> getInjectedModelController() {
		return injectedModelController;
	}
}

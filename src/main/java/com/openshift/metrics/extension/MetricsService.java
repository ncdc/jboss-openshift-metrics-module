package com.openshift.metrics.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	                thread.setName("ConfigAdmin Management Thread");
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
	
	public void addMetricSource(String schedule, String source) throws SchedulerException {
		final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<String,List<Metric>> metricSourceMap = (Map<String, List<Metric>>) jobDataMap.get("metricSources");
		if(null == metricSourceMap) {
			metricSourceMap = new HashMap<String, List<Metric>>();
			jobDataMap.put("metricSources", metricSourceMap);
		}
		metricSourceMap.put(source, new ArrayList<Metric>());
		scheduler.addJob(job, true);
	}
	
	public void addMetric(String schedule, String source, String key, String publishName) throws SchedulerException {
		JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<String,List<Metric>> metricSourceMap = (Map<String, List<Metric>>) jobDataMap.get("metricSources");
		final List<Metric> metrics = metricSourceMap.get(source);
		metrics.add(new Metric(key, publishName));
		
		scheduler.addJob(job, true);

		Trigger trigger = TriggerBuilder.newTrigger()
				.forJob(job)
				.withSchedule(CronScheduleBuilder.cronSchedule(schedule))
				.build();
		
		if(!scheduler.checkExists(trigger.getKey())) {
			scheduler.scheduleJob(trigger);
		}
	}
	
//	public boolean removeJob(String metric) throws SchedulerException {
//		return scheduler.deleteJob(JobKey.jobKey(metric));
//	}
//	
//	public void updateSchedule(String oldSchedule, String newSchedule) throws SchedulerException {
//		Trigger newTrigger = TriggerBuilder.newTrigger()
//				.forJob(JobKey.jobKey(newS))
//				.withSchedule(CronScheduleBuilder.cronSchedule(newSchedule))
//				.build();
//		
//		scheduler.rescheduleJob(TriggerKey.triggerKey(metric), newTrigger);
//	}
	
	public ModelControllerClient getModelControllerClient() {
		return modelControllerClient;
	}
	
	public InjectedValue<ModelController> getInjectedModelController() {
		return injectedModelController;
	}
}

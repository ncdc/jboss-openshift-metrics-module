package com.openshift.metrics.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ResourceDefinition;
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

/**
 * Service that handles scheduling jobs that gather and publish metrics.
 * 
 * @author Andy Goldstein <agoldste@redhat.com>
 */
public class MetricsService implements Service<MetricsService> {
	private Scheduler scheduler;
	
	private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();
	
	private ModelControllerClient modelControllerClient;
	
	private ExecutorService managementOperationExecutor;
	
	public MetricsService() {
		try {
			// I originally had this in start() but it looks like start() and
			// the various ADD operations that are invoked after the subsystem
			// is parsed can run in separate threads, so I had a race condition
			// where something like createJob() would run before start(), resulting
			// in an NPE because the scheduler was null. Not sure of the best way
			// to do this...
			scheduler = StdSchedulerFactory.getDefaultScheduler();
		
			// Is this the most appropriate executor type?
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
			//TODO better error handling
			e.printStackTrace();
		}
	}
	
	/**
	 * @see org.jboss.msc.value.Value#getValue()
	 */
	@Override
	public MetricsService getValue() throws IllegalStateException, IllegalArgumentException {
		return this;
	}

	/**
	 * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
	 */
	@Override
	public void start(StartContext context) throws StartException {
		try {
			// should we just have a single ModelControllerClient, or a pool, or
			// create/destroy them on the fly as jobs use them?
			modelControllerClient = injectedModelController.getValue().createClient(managementOperationExecutor);
			
			// is this the best way to share the modelControllerClient with jobs?
			// is it ok for jobs to share a single client?
			scheduler.getContext().put("modelControllerClient", modelControllerClient);
			
			scheduler.start();
		} catch(SchedulerException e) {
			throw new StartException("Error starting scheduler", e);
		}
	}

	/**
	 * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
	 */
	@Override
	public void stop(StopContext context) {
		try {
			// is this the correct shutdown order?
			scheduler.shutdown();
			managementOperationExecutor.shutdown();
		} catch (SchedulerException e) {
			//TODO log error
		}
	}

	/**
	 * Get this service's {@link ServiceName} for the MSC
	 * 
	 * @return this service's {@link ServiceName}
	 */
	public static ServiceName getServiceName() {
		return ServiceName.JBOSS.append("metrics");
	}

	/**
	 * Create an empty job for the given cron schedule
	 * 
	 * @param schedule a cron expression
	 * @return the created {@link JobDetail}
	 * @throws SchedulerException
	 */
	public JobDetail createJob(String schedule) throws SchedulerException {
		JobDetail job = JobBuilder.newJob(MetricJob.class)
								  .withIdentity(schedule)
								  .storeDurably()
								  .build();
		
		scheduler.addJob(job, false);
		return job;
	}
	
	/**
	 * Remove a job for the given schedule
	 * 
	 * @param schedule a cron expression
	 * @throws SchedulerException
	 */
	public void removeJob(String schedule) throws SchedulerException {
		scheduler.deleteJob(JobKey.jobKey(schedule));
	}
	
	/**
	 * Add a metric source to the job for the given schedule
	 * 
	 * @param schedule a cron expression
	 * @param source a path to either a {@link ResourceDefinition} or an MBean
	 * @throws SchedulerException
	 */
	public void addMetricSource(String schedule, Source source) throws SchedulerException {
		final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<Source, Map<String, String>> metricSourceMap = (Map<Source, Map<String, String>>) jobDataMap.get("metricSources");
		if(null == metricSourceMap) {
			metricSourceMap = new HashMap<Source, Map<String, String>>();
			jobDataMap.put("metricSources", metricSourceMap);
		}
		metricSourceMap.put(source, new HashMap<String, String>());
		scheduler.addJob(job, true);
	}
	
	/**
	 * Remove a metric source from the job for the given schedule
	 * @param schedule a cron expression
	 * @param source a path to either a {@link ResourceDefinition} or an MBean
	 * @throws SchedulerException
	 */
	public void removeMetricSource(String schedule, Source source) throws SchedulerException {
		final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<Source,Map<String, String>> metricSourceMap = (Map<Source, Map<String, String>>) jobDataMap.get("metricSources");
		metricSourceMap.remove(source);
		scheduler.addJob(job, true);
	}
	
	/**
	 * Add a metric to the job for the given schedule and source
	 * 
	 * @param schedule a cron expression
	 * @param source a path to either a {@link ResourceDefinition} or an MBean
	 * @param key name of an attribute for the source to look up
	 * @param publishName name to use when publishing this metric
	 * @throws SchedulerException
	 */
	public void addMetric(String schedule, Source source, String key, String publishName) throws SchedulerException {
		JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		
		final JobDataMap jobDataMap = job.getJobDataMap();
		//changed this to Map<Source,Map<String, String>>
		Map<Source,Map<String, String>> metricSourceMap = (Map<Source, Map<String, String>>) jobDataMap.get("metricSources");
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
	
	/**
	 * Remove a metric from the job for the given schedule and source
	 * 
	 * @param schedule a cron expression
	 * @param source a path to either a {@link ResourceDefinition} or an MBean
	 * @param key name of an attribute for the source to look up
	 * @param publishName name to use when publishing this metric
	 * @throws SchedulerException
	 */
	public void removeMetric(String schedule, Source source, String key, String publishName) throws SchedulerException {
		JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
		final JobDataMap jobDataMap = job.getJobDataMap();
		Map<Source,Map<String, String>> metricSourceMap = (Map<Source, Map<String, String>>) jobDataMap.get("metricSources");
		final Map<String, String> metrics = metricSourceMap.get(source);
		metrics.remove(key);
		
		scheduler.addJob(job, true);
	}

	/**
	 * Get the model controller
	 * @return the model controller
	 */
	public InjectedValue<ModelController> getInjectedModelController() {
		return injectedModelController;
	}
}

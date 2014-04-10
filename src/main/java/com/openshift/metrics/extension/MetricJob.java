package com.openshift.metrics.extension;

import static com.openshift.metrics.extension.Constants.MAX_LINE_LENGTH;
import static com.openshift.metrics.extension.Constants.METRIC_SOURCES;
import static com.openshift.metrics.extension.Constants.MODEL_CONTROLLER_CLIENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class MetricJob implements Job {
    private final Logger log = Logger.getLogger(MetricJob.class);

    private List<String> metricsToPublish = new ArrayList<String>();

    private int outputLength = 0;

    private static final int DEFAULT_MAX_LINE_LENGTH = 1024;

    private Integer maxLineLength = null;

    private void logError(JobExecutionContext context, String message, Exception e) {
        @SuppressWarnings("unchecked")
        Set<String> errorsLogged = (Set<String>) context.getJobDetail().getJobDataMap().get(Constants.ERRORS_LOGGED);

        if(null == errorsLogged) {
            errorsLogged = new HashSet<String>();
            context.getJobDetail().getJobDataMap().put(Constants.ERRORS_LOGGED, errorsLogged);
        }

        if(!errorsLogged.contains(message)) {
            errorsLogged.add(message);
            log.error(message, e);
        }
    }

    private ModelControllerClient getModelControllerClient(JobExecutionContext context) {
        ModelControllerClient client = null;

        try {
            client = (ModelControllerClient) context.getScheduler().getContext().get(MODEL_CONTROLLER_CLIENT);
        } catch (Exception e) {
            logError(context, "Unable to retrieve model controller client from job/scheduler context", e);
        }

        return client;
    }

    private void lookupMaxLineLength(JobExecutionContext context) {
        try {
            maxLineLength = (Integer)context.getScheduler().getContext().get(MAX_LINE_LENGTH);
        } catch (Exception e) {
            // ignore exception - use default value instead
        }

        if(null == maxLineLength) {
            maxLineLength = DEFAULT_MAX_LINE_LENGTH;
        }
    }

    /**
     * Set the {@code operation}'s address from the {@code source}.
     *
     * @param operation the operation
     * @param path a String with a format such as subsystem=foo/type=bar/other=baz
     */
    private void setAddressFromSource(ModelNode operation, String path) {
        ModelNode address = operation.get(OP_ADDR);

        StringTokenizer tokenizer = new StringTokenizer(path, "/");

        // split on /
        while(tokenizer.hasMoreTokens()) {
            // each token will be of the form left=right
            // split into an array
            String[] kv = tokenizer.nextToken().split("=");

            address.add(kv[0], kv[1]);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        lookupMaxLineLength(context);

        // the job data map has the metric sources associated with this job
        final JobDataMap jobDataMap = context.getMergedJobDataMap();

        // grab the sources
        @SuppressWarnings("unchecked")
        Map<String, Source> sources = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);

        // loop through each source, processing all of each source's metrics
        for (String sourcePath : sources.keySet()) {
            Source source = sources.get(sourcePath);

            if(!source.isEnabled()) {
                continue;
            }

            if(Constants.MBEAN.equals(source.getType())) {
                doMBeanSource(source, context);
            } else {
                doNativeSource(source, context);
            }
        }

        if(metricsToPublish.size() > 0) {
            publish();
        }
    }

    private String[] extractKeyAndSubkey(String key) {
        String subkey = null;
        // extract subkey if exists

        // original key can be either one or two levels deep i.e. "name" or "usage.cpu"
        final int dotIndex = key.indexOf('.');
        if(dotIndex > -1) {

            // if we found a . then there's a subkey (e.g. bytes from usage.bytes)
            subkey = key.substring(dotIndex + 1);

            // update key to be the actual attribute name (e.g. usage from usage.bytes)
            key = key.substring(0, dotIndex);
        }

        return new String[] {key, subkey};
    }

    private void doMBeanSource(Source source, JobExecutionContext context) {
        // Get the mBeanServer to access source
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        Map<String, Metric> metrics = source.getMetrics();

        // Iterate over all metrics in source
        for(String publishKey : metrics.keySet()) {
            final Metric metric = metrics.get(publishKey);

            if(!metric.isEnabled()) {
                continue;
            }

            String sourceKey = metric.getSourceKey();
            String keyAndSubkey[] = extractKeyAndSubkey(sourceKey);
            String key = keyAndSubkey[0];
            String subkey = keyAndSubkey[1];

            // Execute getAttribute on mBeanServer with source
            try {
                ObjectName name = new ObjectName(source.getPath());

                Object result = mBeanServer.getAttribute(name, key);

                if(subkey != null) {
                    if(result instanceof CompositeData) {
                        CompositeData cd = (CompositeData) result;

                        if(cd.containsKey(subkey)) {
                            result = cd.get(subkey);
                        } else {
                            continue;
                        }
                    }
                }

                String metricValue  = result.toString();
                storeMetric(publishKey, metricValue);
            } catch (Exception e) {
                String message = MessageFormat.format("Error retrieving mbean metric source={0}, source-key={1}", source.getPath(), metric.getSourceKey());
                logError(context, message, e);
            }
        }
    }
    private void doNativeSource(Source source, JobExecutionContext context) {
        ModelControllerClient modelControllerClient = getModelControllerClient(context);

        if(null == modelControllerClient) {
            // without a client, we can't do much
            return;
        }

        // create an operation request
        ModelNode op = new ModelNode();

        // set the operation to read-attribute
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);

        // set the address appropriately
        setAddressFromSource(op, source.getPath());

        Map<String, Metric> metrics = source.getMetrics();

        // loop through them
        for(String publishKey : metrics.keySet()) {
            final Metric metric = metrics.get(publishKey);

            if(!metric.isEnabled()) {
                continue;
            }

            String sourceKey = metric.getSourceKey();
            String keyAndSubkey[] = extractKeyAndSubkey(sourceKey);
            String key = keyAndSubkey[0];
            String subkey = keyAndSubkey[1];

            // set the name of the attribute we want
            op.get(ModelDescriptionConstants.NAME).set(key);

            try {
                // execute the request
                ModelNode r = modelControllerClient.execute(op);

                final ModelNode outcome = r.get(ModelDescriptionConstants.OUTCOME);
                if(null == outcome || !ModelDescriptionConstants.SUCCESS.equals(outcome.asString())) {
                    String message = MessageFormat.format("Error retrieving jboss metric source={0}, source-key={1}: {2}", source.getPath(), metric.getSourceKey(), r.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString());
                    logError(context, message, null);

                    continue;
                }

                ModelNode result = r.get(ModelDescriptionConstants.RESULT);

                if(subkey != null) {
                    // if we had a subkey, set the result to its value
                    result = result.get(subkey);
                }

                if (result.isDefined()) {
                    String metricValue = result.asString();
                    storeMetric(publishKey, metricValue);
                }
            } catch (Exception e) {
                String message = MessageFormat.format("Error retrieving jboss metric source={0}, source-key={1}", source.getPath(), metric.getSourceKey());
                logError(context, message, e);
            }
        }
    }

    private void storeMetric(String publishKey, String metricValue) {
        final String newMetric = publishKey + "=" + metricValue;
        final int newMetricLength = newMetric.length() + 1;
        if (outputLength + newMetricLength > maxLineLength) {
            publish();
        }
        metricsToPublish.add(newMetric);
        outputLength += newMetricLength;
    }

    private void publish() {
        //TODO consider allowing user to configure "cart" text in config file
        StringBuilder sb = new StringBuilder("type=metric cart=jboss ");
        for(int i = 0, n = metricsToPublish.size(); i < n; i++) {
            String s = metricsToPublish.get(i);
            sb.append(s);
            if(i < n - 1) {
                sb.append(" ");
            }
        }

        log.info(sb.toString());

        metricsToPublish.clear();
        outputLength = 0;
    }
}

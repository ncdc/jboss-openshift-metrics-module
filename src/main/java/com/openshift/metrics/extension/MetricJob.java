package com.openshift.metrics.extension;

import static com.openshift.metrics.extension.Constants.METRIC_SOURCES;
import static com.openshift.metrics.extension.Constants.MODEL_CONTROLLER_CLIENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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

    private ModelControllerClient getModelControllerClient(JobExecutionContext context) {
        ModelControllerClient client = null;

        try {
            client = (ModelControllerClient) context.getScheduler().getContext().get(MODEL_CONTROLLER_CLIENT);
        } catch (SchedulerException e) {
            log.error("Unable to retrieve model controller client from job/scheduler context", e);
            //TODO do we need to consider unscheduling this job if we fail to get the client
            //some # of times?
        }

        return client;
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
        // the job data map has the metric sources associated with this job
        final JobDataMap jobDataMap = context.getMergedJobDataMap();

        // grab the sources
        Map<Source, Map<String, String>> sources = (Map<Source, Map<String, String>>) jobDataMap.get(METRIC_SOURCES);

        // loop through each source, processing all of each source's metrics
        for (Source source : sources.keySet()) {
            if(source.ismBean()) {
                doMBeanSource(source,sources.get(source));
            } else {
                //TODO consider switching jobDataMap(METRIC_SOURCES) to a List or Set
                //and add the metrics to the Source class itself
                doNativeSource(source, sources.get(source), context);
            }
        }
    }
    private void doMBeanSource(Source source, Map<String,String> metrics) {
        // Get the mBeanServer to access source
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        // Iterate over all metrics in source
        for(String originalKey : metrics.keySet()) {

            // original key can be either one or two levels deep i.e. "name" or "usage.cpu"
            String key = originalKey;

            String subkey = null;
            // extract subkey if exists

            final int dotIndex = key.indexOf('.');
            if(dotIndex > -1) {

                // if we found a . then there's a subkey (e.g. bytes from usage.bytes)
                subkey = key.substring(dotIndex + 1);

                // update key to be the actual attribute name (e.g. usage from usage.bytes)
                key = key.substring(0, dotIndex);
            }

            // Execute getAttribute on mBeanServer with source
            try {
                ObjectName name = new ObjectName(source.getPath());
                Object result = mBeanServer.getAttribute(name, key);
                if(subkey != null) {
                    result = ((Map)result).get(key);
                }
                String metricValue  = result.toString();
                publishMetric(metrics.get(originalKey),metricValue);
            } catch (AttributeNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InstanceNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MBeanException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ReflectionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MalformedObjectNameException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private void doNativeSource(Source source, Map<String, String> metrics, JobExecutionContext context) {
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

        // loop through them
        for(String originalKey : metrics.keySet()) {
            // originalKey could be something like profile-name or it could be
            // an index into a map, such as usage.bytes
            String key = originalKey;

            // subkey will represent the index into a map, if it exists
            String subkey = null;

            //TODO consider supporting indexing into arrays
            //          final int leftBracketIndex = key.indexOf('[');

            final int dotIndex = key.indexOf('.');
            if(dotIndex > -1) {
                // if we found a . then there's a subkey (e.g. bytes from usage.bytes)
                subkey = key.substring(dotIndex + 1);

                // update key to be the actual attribute name (e.g. usage from usage.bytes)
                key = key.substring(0, dotIndex);
            }

            // set the name of the attribute we want
            op.get(ModelDescriptionConstants.NAME).set(key);

            try {
                // execute the request
                ModelNode r = modelControllerClient.execute(op);

                // TODO consider checking the status
                // get the result
                ModelNode result = r.get(ModelDescriptionConstants.RESULT);

                if(subkey != null) {
                    // if we had a subkey, set the result to its value
                    result = result.get(subkey);
                }

                String publishName = metrics.get(originalKey);
                String metricValue = result.asString();
                publishMetric(publishName, metricValue);
            } catch (IOException e) {
                log.error("Error executing operation " + op, e);
                // TODO anything else to do here?
            }
        }
    }

    private void publishMetric(String publishName, String metricValue) {
        //TODO switch to syslog?
        log.info(publishName + "=" + metricValue);
    }

}

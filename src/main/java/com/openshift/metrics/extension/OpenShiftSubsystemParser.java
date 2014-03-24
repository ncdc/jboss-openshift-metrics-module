package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
 */
public class OpenShiftSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final String CRON = "cron";
    private static final String METRIC_SCHEDULE = "metric-schedule";

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(OpenShiftSubsystemExtension.NAMESPACE, false);
        ModelNode node = context.getModelNode();
        ModelNode schedules = node.get(Constants.SCHEDULE);
        writeSchedules(writer, schedules);
        writer.writeEndElement();
    }

    private void writeSchedules(XMLExtendedStreamWriter writer, ModelNode schedules) throws XMLStreamException {
        for(ModelNode schedule : schedules.asList()){
            writer.writeStartElement(METRIC_SCHEDULE);
            final Property scheduleProperty = schedule.asProperty();
            writer.writeAttribute(CRON, scheduleProperty.getName().replaceAll("_", " "));

            ModelNode sources = scheduleProperty.getValue().get(Constants.SOURCE);
            writeSources(writer, sources);

            writer.writeEndElement();

        }
    }

    private void writeSources(XMLExtendedStreamWriter writer, ModelNode sources) throws XMLStreamException {
        for(String sourcePath : sources.keys()) {
            ModelNode source = sources.get(sourcePath);
            writer.writeStartElement(Constants.SOURCE);
            writer.writeAttribute(Constants.NODE, sourcePath);
            if(source.hasDefined(Constants.MBEAN) && source.get(Constants.MBEAN).asBoolean()) {
                writer.writeAttribute(Constants.MBEAN, "true");
            }

            ModelNode metrics = source.get(Constants.METRIC);
            writeMetrics(writer, metrics);

            writer.writeEndElement();
        }
    }

    private void writeMetrics(XMLExtendedStreamWriter writer, ModelNode metrics) throws XMLStreamException {
        for(String publishName : metrics.keys()) {
            ModelNode metric = metrics.get(publishName);
            String key = metric.get(Constants.KEY).asString();
            writer.writeStartElement(Constants.METRIC);
            writer.writeAttribute(Constants.KEY, key);
            writer.writeAttribute(Constants.PUBLISH_NAME, publishName);
            writer.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        final PathAddress address = PathAddress.pathAddress(OpenShiftSubsystemExtension.SUBSYSTEM_PATH);
        final ModelNode subsystem = Util.getEmptyOperation(ADD, address.toModelNode());
        list.add(subsystem);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if(!reader.getLocalName().equals(METRIC_SCHEDULE)) {
                throw ParseUtils.unexpectedElement(reader);
            }

            readMetricSchedule(reader, address, list);
        }
    }

    private void readMetricSchedule(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        String schedule = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if(CRON.equals(attr)) {
                // need to convert spaces to underscores
                schedule = value.replaceAll(" ", "_");
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        final PathAddress address = parentAddress.append(PathElement.pathElement(Constants.SCHEDULE, schedule));
        final ModelNode addScheduleOperation = Util.getEmptyOperation(ADD, address.toModelNode());
        list.add(addScheduleOperation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if(!reader.getLocalName().equals(Constants.SOURCE)) {
                throw ParseUtils.unexpectedElement(reader);
            }
            readMetricSource(reader, address, list);
        }
    }

    private void readMetricSource(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        String node = null;
        final ModelNode addSourceOperation = Util.getEmptyOperation(ADD, null);
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if(Constants.NODE.equals(attr)) {
                node= value;
            } else if(Constants.MBEAN.equals(attr)) {
                SourceDefinition.MBEAN.parseAndSetParameter(value, addSourceOperation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        PathAddress address = parentAddress.append(PathElement.pathElement(Constants.SOURCE, node));
        addSourceOperation.get(OP_ADDR).set(address.toModelNode());
        list.add(addSourceOperation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if(!reader.getLocalName().equals(Constants.METRIC)) {
                throw ParseUtils.unexpectedElement(reader);
            }

            readMetric(reader, address, list);
        }
    }

    private void readMetric(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        final ModelNode addMetricOperation = Util.getEmptyOperation(ADD, null);

        String publishName = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if(Constants.KEY.equals(attr)) {
                MetricDefinition.KEY.parseAndSetParameter(value, addMetricOperation, reader);
            } else if (Constants.PUBLISH_NAME.equals(attr)) {
                publishName = value;
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        ParseUtils.requireNoContent(reader);

        if (null == publishName) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Constants.PUBLISH_NAME));
        }

        PathAddress address = parentAddress.append(PathElement.pathElement(Constants.METRIC, publishName));
        addMetricOperation.get(OP_ADDR).set(address.toModelNode());
        list.add(addMetricOperation);
    }
}
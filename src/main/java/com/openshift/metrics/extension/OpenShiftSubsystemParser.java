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
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
 */
public class OpenShiftSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(OpenShiftSubsystemExtension.NAMESPACE, false);

        ModelNode node = context.getModelNode();

        if(node.hasDefined(Constants.MAX_LINE_LENGTH)) {
            OpenShiftSubsystemDefinition.MAX_LINE_LENGTH.marshallAsElement(node, writer);
        }

        ModelNode metricsGroups = node.get(Constants.METRICS_GROUP);
        writeMetricsGroups(writer, metricsGroups);
        writer.writeEndElement();
    }

    private void writeMetricsGroups(XMLExtendedStreamWriter writer, ModelNode metricsGroups) throws XMLStreamException {
        for(String key : metricsGroups.keys()){
            writer.writeStartElement(Constants.METRICS_GROUP);

            ModelNode metricsGroup = metricsGroups.get(key);

            String cronExpression = com.openshift.metrics.extension.Util.decodeCronExpression(key);

            writer.writeAttribute(Constants.CRON, cronExpression);

            if(metricsGroup.hasDefined(Constants.ENABLED) && !metricsGroup.get(Constants.ENABLED).asBoolean()) {
                writer.writeAttribute(Constants.ENABLED, "false");
            }

            ModelNode sources = metricsGroup.get(Constants.SOURCE);
            writeSources(writer, sources);

            writer.writeEndElement();

        }
    }

    private void writeSources(XMLExtendedStreamWriter writer, ModelNode sources) throws XMLStreamException {
        for(String sourcePath : sources.keys()) {
            ModelNode source = sources.get(sourcePath);
            writer.writeStartElement(Constants.SOURCE);
            writer.writeAttribute(Constants.PATH, sourcePath);
            writer.writeAttribute(Constants.TYPE, source.get(Constants.TYPE).asString());

            if(source.hasDefined(Constants.ENABLED) && !source.get(Constants.ENABLED).asBoolean()) {
                writer.writeAttribute(Constants.ENABLED, "false");
            }

            ModelNode metrics = source.get(Constants.METRIC);
            writeMetrics(writer, metrics);

            writer.writeEndElement();
        }
    }

    private void writeMetrics(XMLExtendedStreamWriter writer, ModelNode metrics) throws XMLStreamException {
        for(String publishName : metrics.keys()) {
            ModelNode metric = metrics.get(publishName);
            String key = metric.get(Constants.SOURCE_KEY).asString();
            writer.writeStartElement(Constants.METRIC);
            writer.writeAttribute(Constants.SOURCE_KEY, key);
            writer.writeAttribute(Constants.PUBLISH_KEY, publishName);

            if(metric.hasDefined(Constants.ENABLED) && !metric.get(Constants.ENABLED).asBoolean()) {
                writer.writeAttribute(Constants.ENABLED, "false");
            }

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
            if(reader.getLocalName().equals(Constants.MAX_LINE_LENGTH)) {
                final String value = parseElementNoAttributes(reader);
                OpenShiftSubsystemDefinition.MAX_LINE_LENGTH.parseAndSetParameter(value, subsystem, reader);
            } else if(reader.getLocalName().equals(Constants.METRICS_GROUP)) {
                readMetricsGroups(reader, address, list);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void readMetricsGroups(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        final ModelNode addScheduleOperation = Util.getEmptyOperation(ADD, null);
        String schedule = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);

            if(Constants.CRON.equals(attr)) {
                schedule = com.openshift.metrics.extension.Util.encodeCronExpression(value);
            } else if(Constants.ENABLED.equals(attr)) {
                MetricsGroupDefinition.ENABLED.parseAndSetParameter(value, addScheduleOperation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        final PathAddress address = parentAddress.append(PathElement.pathElement(Constants.METRICS_GROUP, schedule));
        addScheduleOperation.get(OP_ADDR).set(address.toModelNode());
        list.add(addScheduleOperation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if(!reader.getLocalName().equals(Constants.SOURCE)) {
                throw ParseUtils.unexpectedElement(reader);
            }
            readSource(reader, address, list);
        }
    }

    private void readSource(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        String path = null;
        final ModelNode addSourceOperation = Util.getEmptyOperation(ADD, null);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if(Constants.PATH.equals(attr)) {
                path = value;
            } else if(Constants.TYPE.equals(attr)) {
                SourceDefinition.TYPE.parseAndSetParameter(value, addSourceOperation, reader);
            } else if(Constants.ENABLED.equals(attr)) {
                MetricsGroupDefinition.ENABLED.parseAndSetParameter(value, addSourceOperation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        PathAddress address = parentAddress.append(PathElement.pathElement(Constants.SOURCE, path));
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
            if(Constants.SOURCE_KEY.equals(attr)) {
                MetricDefinition.SOURCE_KEY.parseAndSetParameter(value, addMetricOperation, reader);
            } else if (Constants.PUBLISH_KEY.equals(attr)) {
                publishName = value;
            } else if(Constants.ENABLED.equals(attr)) {
                MetricsGroupDefinition.ENABLED.parseAndSetParameter(value, addMetricOperation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        ParseUtils.requireNoContent(reader);

        if (null == publishName) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Constants.PUBLISH_KEY));
        }

        PathAddress address = parentAddress.append(PathElement.pathElement(Constants.METRIC, publishName));
        addMetricOperation.get(OP_ADDR).set(address.toModelNode());
        list.add(addMetricOperation);
    }

    private String parseElementNoAttributes(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // no attributes
        ParseUtils.requireNoAttributes(reader);

        return reader.getElementText().trim();
    }
}
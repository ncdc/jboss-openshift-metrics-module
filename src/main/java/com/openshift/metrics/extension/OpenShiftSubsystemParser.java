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
        //TODO!!!
        writer.writeEndElement();
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
            if(!reader.getLocalName().equals("metric-schedule")) {
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
            if("cron".equals(attr)) {
                schedule = value;
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        final PathAddress address = parentAddress.append(PathElement.pathElement("schedule", schedule));
        final ModelNode addScheduleOperation = Util.getEmptyOperation(ADD, address.toModelNode());
        list.add(addScheduleOperation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if(!reader.getLocalName().equals("source")) {
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
            if("node".equals(attr)) {
                node= value;
            } else if("mbean".equals(attr)) {
                SourceDefinition.MBEAN.parseAndSetParameter(value, addSourceOperation, reader);
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        PathAddress address = parentAddress.append(PathElement.pathElement("source", node));
        addSourceOperation.get(OP_ADDR).set(address.toModelNode());
        list.add(addSourceOperation);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if(!reader.getLocalName().equals("metric")) {
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
            if("key".equals(attr)) {
                MetricDefinition.KEY.parseAndSetParameter(value, addMetricOperation, reader);
            } else if ("publish-name".equals(attr)) {
                publishName = value;
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        ParseUtils.requireNoContent(reader);

        if (null == publishName) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton("publish-name"));
        }

        PathAddress address = parentAddress.append(PathElement.pathElement("metric", publishName));
        addMetricOperation.get(OP_ADDR).set(address.toModelNode());
        list.add(addMetricOperation);
    }
}
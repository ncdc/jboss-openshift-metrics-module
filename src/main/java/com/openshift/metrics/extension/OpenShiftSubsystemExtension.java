package com.openshift.metrics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


public class OpenShiftSubsystemExtension implements Extension {
    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:redhat:openshift:metrics:1.0";

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "metrics";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    
    /**
     * The parser used for parsing our subsystem
     */
    private final SubsystemParser parser = new SubsystemParser();

    private static final String RESOURCE_NAME = OpenShiftSubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, OpenShiftSubsystemExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }


    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(OpenShiftSubsystemDefinition.INSTANCE);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        subsystem.registerXMLElementWriter(parser);
    }

    private static ModelNode createAddSubsystemOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    /**
     * The subsystem parser, which uses stax to read and write to and from xml
     */
    private static class SubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(OpenShiftSubsystemExtension.NAMESPACE, false);
            writer.writeEndElement();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        	ParseUtils.requireNoAttributes(reader);
            list.add(createAddSubsystemOperation());
            
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            	if(!reader.getLocalName().equals("metric-schedules")) {
            		throw ParseUtils.unexpectedElement(reader);
            	}
            	while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            		readMetricSchedule(reader, list);
            	}
            }
        }
        
        private void readMetricSchedule(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        	if (!reader.getLocalName().equals("metric-schedule")) {
        		throw ParseUtils.unexpectedElement(reader);
        	}
        	
        	ModelNode addScheduleOperation = new ModelNode();
        	addScheduleOperation.get(OP).set(ModelDescriptionConstants.ADD);
        	
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
        	
        	PathAddress address = PathAddress.pathAddress(SUBSYSTEM_PATH, PathElement.pathElement("schedule", schedule));
        	addScheduleOperation.get(OP_ADDR).set(address.toModelNode());
        	list.add(addScheduleOperation);
        	
        	while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
        		if(!reader.getLocalName().equals("source")) {
        			throw ParseUtils.unexpectedElement(reader);
        		}
//        		while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            		readMetricSource(reader, address, list);
//            	}
        	}
        }
        
        private void readMetricSource(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        	if (!reader.getLocalName().equals("source")) {
        		throw ParseUtils.unexpectedElement(reader);
        	}
        	
        	ModelNode addSourceOperation = new ModelNode();
        	addSourceOperation.get(OP).set(ModelDescriptionConstants.ADD);
        	
        	String node = null;
        	for (int i = 0; i < reader.getAttributeCount(); i++) {
        		String attr = reader.getAttributeLocalName(i);
                String value = reader.getAttributeValue(i);
                if("node".equals(attr)) {
                	node= value;
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
//        		while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            		readMetric(reader, address, list);
//            	}
        	}
        }
        
        private void readMetric(XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        	if (!reader.getLocalName().equals("metric")) {
        		throw ParseUtils.unexpectedElement(reader);
        	}
        	
        	ModelNode addMetricOperation = new ModelNode();
        	addMetricOperation.get(OP).set(ModelDescriptionConstants.ADD);
        	
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

}

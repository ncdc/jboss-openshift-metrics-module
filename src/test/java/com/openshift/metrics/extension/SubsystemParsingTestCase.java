package com.openshift.metrics.extension;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;


public class SubsystemParsingTestCase extends AbstractSubsystemTest {
    static final String subsystemXml =
            "<subsystem xmlns=\"" + OpenShiftSubsystemExtension.NAMESPACE + "\">"        +
            "   <metrics-group cron=\"*/5 * * * * ?\">"                                  +
            "       <source type=\"jboss\" path=\"g1.s1\">"                              +
            "           <metric source-key=\"g1.s1.sk1\" publish-key=\"g1.s1.pk1\" />"   +
            "           <metric source-key=\"g1.s1.sk2\" publish-key=\"g1.s1.pk2\" />"   +
            "       </source>"                                                           +
            "       <source type=\"mbean\" path=\"g1.s2\">"                              +
            "           <metric source-key=\"g1.s2.sk1\" publish-key=\"g1.s2.pk1\" />"   +
            "       </source>"                                                           +
            "   </metrics-group>"                                                        +
            "   <metrics-group cron=\"* * * * * ?\">"                                      +
            "       <source type=\"jboss\" path=\"g2.s1\">"                              +
            "           <metric source-key=\"g2.s1.sk1\" publish-key=\"g2.s1.pk1\" />"   +
            "       </source>"                                                           +
            "   </metrics-group>"                                                        +
            "</subsystem>";

    public SubsystemParsingTestCase() {
        super(OpenShiftSubsystemExtension.SUBSYSTEM_NAME, new OpenShiftSubsystemExtension());
    }

    @Rule
    public MethodRule watchman = new TestWatchman() {
       @Override
       public void starting(FrameworkMethod method) {
          System.out.println("Starting test: " + method.getName());
       }
    };


    @Before
    public void before() throws SchedulerException {
        StdSchedulerFactory.getDefaultScheduler().clear();
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(subsystemXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(10, operations.size());

        //Check that each operation has the correct content

        // op1: add subsystem
        ModelNode op = operations.get(0);
        validateAddSubsystemOp(op);

        // op2: add group1
        op = operations.get(1);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^/5_^_^_^_^_?");

        // op3: add group1.source1
        op = operations.get(2);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^/5_^_^_^_^_?");
        validateAddSourceOp(op, "g1.s1", "jboss");

        // op4: add group1.source1.metric1
        op = operations.get(3);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^/5_^_^_^_^_?");
        validateAddSourceOp(op, "g1.s1", "jboss");
        validateAddMetricOp(op, "g1.s1.sk1", "g1.s1.pk1");

        // op5: add group1.source1.metric2
        op = operations.get(4);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^/5_^_^_^_^_?");
        validateAddSourceOp(op, "g1.s1", "jboss");
        validateAddMetricOp(op, "g1.s1.sk2", "g1.s1.pk2");

        // op6: add group1.source2
        op = operations.get(5);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^/5_^_^_^_^_?");
        validateAddSourceOp(op, "g1.s2", "mbean");

        // op7: add group1.source1.metric1
        op = operations.get(6);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^/5_^_^_^_^_?");
        validateAddSourceOp(op, "g1.s2", "jboss");
        validateAddMetricOp(op, "g1.s2.sk1", "g1.s2.pk1");

        // op8: add group2
        op = operations.get(7);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^_^_^_^_^_?");

        // op9: add group2.source1
        op = operations.get(8);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^_^_^_^_^_?");
        validateAddSourceOp(op, "g2.s1", "jboss");

        // op10: add group2.source1.metric1
        op = operations.get(9);
        validateAddSubsystemOp(op);
        validateAddMetricsGroupOp(op, "^_^_^_^_^_?");
        validateAddSourceOp(op, "g2.s1", "jboss");
        validateAddMetricOp(op, "g2.s1.sk1", "g2.s1.pk1");
    }

    private void validateAddMetricOp(ModelNode op, String sourceKey, String publishKey) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathElement element = addr.getElement(3);
        Assert.assertEquals(Constants.METRIC, element.getKey());
        Assert.assertEquals(publishKey, element.getValue());
        Assert.assertEquals(sourceKey, op.get(Constants.SOURCE_KEY).asString());
    }

    private void validateAddSourceOp(ModelNode op, String path, String type) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathElement element = addr.getElement(2);
        Assert.assertEquals(Constants.SOURCE, element.getKey());
        Assert.assertEquals(path, element.getValue());
        if(Constants.SOURCE.equals(addr.getLastElement().getKey())) {
            Assert.assertEquals(type, op.get(Constants.TYPE).asString());
        }
    }

    private void validateAddMetricsGroupOp(ModelNode op, String schedule) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathElement element = addr.getElement(1);
        Assert.assertEquals(Constants.METRICS_GROUP, element.getKey());
        Assert.assertEquals(schedule, element.getValue());
    }

    private void validateAddSubsystemOp(ModelNode op) {
        Assert.assertEquals(ADD, op.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(OpenShiftSubsystemExtension.SUBSYSTEM_NAME, element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        KernelServices services = super.installInController(subsystemXml);

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OpenShiftSubsystemExtension.SUBSYSTEM_NAME));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.installInController(subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        servicesA.shutdown();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second
     * controller started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.installInController(subsystemXml);
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, OpenShiftSubsystemExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

        servicesA.shutdown();

        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = super.installInController(operations);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Tests that the subsystem can be removed
     */
    @Test
    public void testSubsystemRemoval() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices services = super.installInController(subsystemXml);
        //Checks that the subsystem was removed from the model
        super.assertRemoveSubsystemResources(services);

        //TODO Chek that any services that were installed were removed here
    }
}

package com.openshift.metrics.extension;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * This is the barebone test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at  {@link SubsystemParsingTestCase}
 */
public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(OpenShiftSubsystemExtension.SUBSYSTEM_NAME, new OpenShiftSubsystemExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + OpenShiftSubsystemExtension.NAMESPACE + "\">"         +
                "   <metrics-group cron=\"*/5 * * * * ?\">"                                  +
                "       <source type=\"jboss\" path=\"g1.s1\">"                              +
                "           <metric source-key=\"g1.s1.sk1\" publish-key=\"g1.s1.pk1\" />"   +
                "           <metric source-key=\"g1.s1.sk2\" publish-key=\"g1.s1.pk2\" />"   +
                "       </source>"                                                           +
                "       <source type=\"mbean\" path=\"g1.s2\">"                              +
                "           <metric source-key=\"g1.s2.sk1\" publish-key=\"g1.s2.pk1\" />"   +
                "       </source>"                                                           +
                "   </metrics-group>"                                                        +
                "   <metrics-group cron=\"* * * * * ?\" enabled=\"false\">"                  +
                "       <source type=\"jboss\" path=\"g2.s1\" enabled=\"false\">"            +
                "           <metric source-key=\"g2.s1.sk1\" publish-key=\"g2.s1.pk1\" enabled=\"false\" />"   +
                "       </source>"                                                           +
                "   </metrics-group>"                                                        +
                "</subsystem>";
    }

}

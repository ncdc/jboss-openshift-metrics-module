# jboss-openshift-metrics-extension

## Prerequisites

Create the following directory structure if it does not exist: $JBOSS_HOME/modules/org/quartz/main

Create $JBOSS_HOME/modules/org/quartz/main/module.xml with the following contents:

    <module xmlns="urn:jboss:module:1.1" name="org.quartz">
      <resources>
        <resource-root path="quartz.jar"/>
      </resources>
      <dependencies>
        <module name="org.slf4j"/>
        <module name="javax.api"/>
      </dependencies>
    </module>

Copy quartz.jar to $JBOSS_HOME/modules/org/quartz/main (e.g. from Maven Central, such as http://repo1.maven.org/maven2/org/quartz-scheduler/quartz/2.2.1/quartz-2.2.1.jar). Note that the jar must be called quartz.jar, or you'll need to adjust the module.xml file you created above.

## Installation

1. Run `mvn package`
1. Create the following directory structure if it does not exist: $JBOSS_HOME/modules/com/openshift/metrics/main
1. From your project directory, copy the contents of target/module/com/openshift/metrcis/main to the corresponding directory you created in the previous step

## Configuration

In standalone.xml, add the following inside the `<extensions>` section:

`<extension module="com.openshift.metrics"/>`

Next, configure the metrics subsystem inside the `<profile>` section. Here is an example:

    <subsystem xmlns="urn:redhat:openshift:metrics:1.0">
      <metric-schedule cron="*/5 * * * * ?">
        <source node="core-service=platform-mbean/type=memory-pool/name=CMS_Old_Gen">
          <metric key="usage.used" publish-name="oldgen.used" />
          <metric key="usage.max" publish-name="oldgen.max" />
          <metric key="usage.committed" publish-name="oldgen.committed" />
        </source>
      </metric-schedule>
    </subsystem>

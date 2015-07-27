/*
 * Copyright 2015, Liberty Mutual Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmig.cf.metrics.opsmetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author David Ehringer
 */
@Component
public class OpsMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(OpsMetrics.class);

    private static final String WILDCARD_OBJECT_NAME = "org.cloudfoundry:deployment=*,job=*,index=*,ip=*";
    private static final Pattern APP_METRIC_PATTERN = Pattern
            .compile("appId=[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final boolean includeAppSpecificMetrics;
    private final String host;
    private final String port;
    private final String username;
    private final String password;

    @Autowired
    public OpsMetrics(@Value("${opsmetrics.host}") String host,
            @Value("${opsmetrics.port:44444}") String port,
            @Value("${opsmetrics.username}") String username,
            @Value("${opsmetrics.password}") String password,
            @Value("${opsmetrics.includeAppSpecificMetrics:false}") boolean includeAppSpecificMetrics) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.includeAppSpecificMetrics = includeAppSpecificMetrics;
    }

    public List<JmxMetric> getMetrics() throws OpsMetricsException {
        return execute(new JmxTemplate<List<JmxMetric>>() {
            @Override
            public List<JmxMetric> execute(MBeanServerConnection connection) throws Exception {
                ObjectName query = ObjectName.getInstance(WILDCARD_OBJECT_NAME);
                return searchForMetrics(connection, query);
            }
        });
    }

    private <T> T execute(JmxTemplate<T> template) throws OpsMetricsException {
        LOG.debug("Connecting to {}:{}", host, port);
        JMXConnector connector = null;
        try {
            JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":"
                    + port + "/jmxrmi");
            HashMap<String, String[]> env = getCredentials();
            connector = JMXConnectorFactory.connect(address, env);
            MBeanServerConnection mbs = connector.getMBeanServerConnection();
            return template.execute(mbs);
        } catch (Exception e) {
            LOG.error("Unabled to execute JMX command", e);
            throw new OpsMetricsException(e);
        } finally {
            close(connector);
        }
    }

    private HashMap<String, String[]> getCredentials() {
        HashMap<String, String[]> env = null;
        if (username != null && password != null) {
            env = new HashMap<String, String[]>();
            env.put("jmx.remote.credentials", new String[] { username, password });
        }
        return env;
    }

    private void close(JMXConnector connector) {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
            }
        }
    }
    
    private List<JmxMetric> searchForMetrics(MBeanServerConnection connection, ObjectName objectName) throws Exception {
        List<JmxMetric> metrics = new ArrayList<JmxMetric>();
        Set<ObjectInstance> matchingObjects = connection.queryMBeans(objectName, null);
        for (ObjectInstance object : matchingObjects) {
            List<String> attributes = getAttributeNames(connection, object);
            attributes
                .stream()
                .filter(attr -> !blacklisted(attr))
                .filter(attr -> metricValueIsNumber(connection, object, attr))
                .map(attr -> new JmxMetric(object, attr, getAttributeValue(connection, object, attr)))
                .forEach(metrics::add);
        }
        return metrics;
    }
    
    private Number getAttributeValue(MBeanServerConnection connection, ObjectInstance object,
            String attribute) {
        try {
            return (Number)connection.getAttribute(object.getObjectName(), attribute);
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
                | ReflectionException | IOException e) {
            return 0;
        }
    }

    private boolean metricValueIsNumber(MBeanServerConnection connection, ObjectInstance object, String attribute) {
        try {
            return connection.getAttribute(object.getObjectName(), attribute) instanceof Number;
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
                | ReflectionException | IOException e) {
            return false;
        }
    }

    private boolean blacklisted(String metricName) {
        if(includeAppSpecificMetrics){
            return false;
        }
        return appSpecficMetric(metricName);
    }

    private boolean appSpecficMetric(String metricName) {
        return APP_METRIC_PATTERN.matcher(metricName).find();
    }
    
    private List<String> getAttributeNames(MBeanServerConnection connection, ObjectInstance objectInstance) throws Exception {
        MBeanInfo mbeanInfo = connection.getMBeanInfo(objectInstance.getObjectName());
        MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
        return Arrays.asList(attributes).stream().map(attr -> attr.getName()).collect(Collectors.toList());
    }
}

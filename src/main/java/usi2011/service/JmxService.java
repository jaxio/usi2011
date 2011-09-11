package usi2011.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isDebugEnabled;

import java.util.Hashtable;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

@Service
@ManagedResource
public class JmxService {
    private static final Logger logger = getLogger(JmxService.class);

    @Autowired
    private AnnotationMBeanExporter mbeanExporter;

    private final List<ObjectName> objects = newArrayList();

    private JmxService() {
    }

    public void register(Object o) {
        mbeanExporter.registerManagedResource(o);
    }

    @ManagedOperation
    public void clear() {
        if (isDebugEnabled) {
            logger.debug("unregistering all references");
        }
        for (ObjectName objectName : objects) {
            unregister(objectName);
        }
    }

    public void unregister(ObjectName objectName) {
        if (isDebugEnabled) {
            logger.debug("unregistering {}", objectName);
        }
        mbeanExporter.unregisterManagedResource(objectName);
    }

    public ObjectName getMonitorName(Object managedBean, int question) {
        try {
            String domain = ClassUtils.getPackageName(managedBean.getClass());
            String clazz = ClassUtils.getShortName(managedBean.getClass());
            Hashtable<String, String> p = new Hashtable<String, String>();
            p.put("type", clazz);
            p.put("value", format("Question %03d", question));
            return ObjectNameManager.getInstance(domain, p);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @ManagedAttribute
    public int getNbObjects() {
        return objects.size();
    }

    public ObjectName createDummyMonitorName() throws MalformedObjectNameException {
        try {
            return ObjectNameManager.getInstance("dummy", "spring-prototype", "workaround");
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
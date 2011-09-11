package usi2011.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class ResourceUtil {
    
    public static String asString(Resource resource) {
        try {
            return IOUtils.toString(resource.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String asString(String resourceName) {
        return asString(new ClassPathResource("/" + resourceName));
    }

    public static String asJsonString(String resourceName) {
        String ret = asString(resourceName);
        ret.replace("\"", "\\\"");
        ret.replace("\n", "\\\n");
        return ret;
    }
}

package org.fenixedu.bennu.portal.service;

import com.mitchellbosecke.pebble.extension.AbstractExtension;

import jakarta.servlet.ServletContext;
import java.util.List;

public interface PebblePortalExtensionProvider {

    List<AbstractExtension> provide(ServletContext context);
}

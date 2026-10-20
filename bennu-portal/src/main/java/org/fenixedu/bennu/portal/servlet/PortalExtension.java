package org.fenixedu.bennu.portal.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;

import org.fenixedu.bennu.core.i18n.BundleUtil;

import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

public class PortalExtension extends AbstractExtension {

    private final ServletContext context;

    public PortalExtension(ServletContext context) {
        this.context = context;
    }

    @Override
    public Map<String, Filter> getFilters() {
        Map<String, Filter> filters = new HashMap<>();
        filters.put("base64", new Base64Filter());
        return filters;
    }

    private static class Base64Filter implements Filter {

        @Override
        public List<String> getArgumentNames() {
            return null;
        }

        @Override
        public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context,
                int lineNumber) {
            if (input instanceof byte[]) {
                byte[] bytes = (byte[]) input;
                return BaseEncoding.base64().encode(bytes);
            }
            return input;
        }

    }

    @Override
    public Map<String, Function> getFunctions() {
        Map<String, Function> functions = new HashMap<>();
        functions.put("i18n", new I18NFunction());
        functions.put("asset", new AssetFunction());
        functions.put("test", new ThrowException());
        return functions;
    }

    @Override
    public Map<String, Test> getTests() {
        Map<String, Test> tests = new HashMap<>();
        tests.put("in", new InTest());
        return tests;
    }

    private static class I18NFunction implements Function {
        @Override
        public List<String> getArgumentNames() {
            List<String> names = new ArrayList<>();
            names.add("bundle");
            names.add("key");
            return names;
        }

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            String bundle = (String) args.get("bundle");
            String key = args.get("key").toString();

            return BundleUtil.getString(bundle, key);
        }
    }

    private static class ThrowException implements Function {

        @Override
        public List<String> getArgumentNames() {
            return List.of();
        }

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            throw new ArrayIndexOutOfBoundsException();
        }

    }

    private class AssetFunction implements Function {

        private final Map<String, String> pathMapping = new ConcurrentHashMap<>();

        @Override
        public List<String> getArgumentNames() {
            return Collections.singletonList("path");
        }

        @Override
        public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            String path = (String) args.get("path");
            return pathMapping.computeIfAbsent(path, this::computePath);
        }

        private String computePath(String path) {
            String checksum = "_";
            try (InputStream stream = context.getResourceAsStream(path)) {
                if (stream != null) {
                    Hasher hasher = Hashing.sha1().newHasher();
                    try (OutputStream out = Funnels.asOutputStream(hasher)) {
                        ByteStreams.copy(stream, out);
                        checksum = hasher.hash().toString().substring(0, 12);
                    }
                }
            } catch (IOException e) {
                // Ignored, we'll simply use the default value
            }
            return context.getContextPath() + path + "?v=" + checksum;
        }
    }

    private static class InTest implements Test {
        @Override
        public List<String> getArgumentNames() {
            return Collections.singletonList("collection");
        }

        @Override
        public boolean apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context,
                int lineNumber) {
            Collection<?> collection = (Collection<?>) args.get("collection");
            return collection.contains(input);
        }
    }

    @Override
    public List<TokenParser> getTokenParsers() {
        return Collections.singletonList(new LazyForTokenParser());
    }

}

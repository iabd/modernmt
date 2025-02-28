package eu.modernmt.api;

import eu.modernmt.api.framework.JSONSerializer;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.framework.routing.RouterServlet;
import eu.modernmt.api.model.ContextVectorResult;
import eu.modernmt.api.model.TranslationResponse;
import eu.modernmt.api.serializers.*;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.ImportJob;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.reflections.Reflections;

import java.io.File;
import java.util.Collection;

/**
 * Created by davide on 15/12/15.
 */
public class ApiServer {

    static {
        JSONSerializer.registerCustomSerializer(TranslationResponse.class, new TranslationResponseSerializer());
        JSONSerializer.registerCustomSerializer(Alignment.class, new AlignmentSerializer());
        JSONSerializer.registerCustomSerializer(ContextVectorResult.class, new ContextVectorResultSerializer());
        JSONSerializer.registerCustomSerializer(Language.class, new LanguageSerializer());
        JSONSerializer.registerCustomSerializer(LanguageDirection.class, new LanguagePairSerializer());
        JSONSerializer.registerCustomSerializer(ImportJob.class, new ImportJobSerializer());
    }

    public static class ServerOptions {

        // The network port to bind
        public int port;

        // the API context path (aka API root path)
        public String contextPath = null;

        // Directory used for storing uploaded files
        public File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));

        // The maximum size allowed for uploaded files
        public long maxFileSize = 100L * 1024L * 1024L; // 100mb

        // The maximum size allowed for multipart/form-data requests
        public long maxRequestSize = 101L * 1024L * 1024L; // 101mb

        // the size threshold after which files will be written to disk
        public int fileSizeThreshold = 2 * 1024; // 2kb

        public ServerOptions(int port) {
            this.port = port;
        }
    }

    private final Server jettyServer;

    public ApiServer(ServerOptions options) {
        jettyServer = new Server(new QueuedThreadPool(450));

        ServerConnector connector = new ServerConnector(jettyServer);
        connector.setPort(options.port);
        jettyServer.setConnectors(new Connector[]{connector});

        Handler rootHandler;
        String contextPath = normalizeContextPath(options.contextPath);
        if (contextPath == null) {
            ServletHandler router = new ServletHandler();
            router.addServletWithMapping(Router.class, "/*");
            rootHandler = router;
        } else {
            ServletContextHandler contextHandler = new ServletContextHandler();
            contextHandler.setContextPath(contextPath);
            contextHandler.addServlet(Router.class, "/*");
            rootHandler = contextHandler;
        }

        MultipartConfigInjectionHandler multipartWrapper = new MultipartConfigInjectionHandler(
                options.temporaryDirectory, options.maxFileSize, options.maxRequestSize, options.fileSizeThreshold);
        multipartWrapper.setHandler(rootHandler);

        jettyServer.setHandler(multipartWrapper);
    }

    private String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.trim().isEmpty())
            return null;

        contextPath = contextPath.trim();
        if (contextPath.charAt(0) != '/')
            contextPath = '/' + contextPath;
        if (contextPath.charAt(contextPath.length() - 1) == '/')
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        return contextPath;
    }

    public void start() throws Exception {
        jettyServer.start();
    }

    public void stop() throws Exception {
        jettyServer.stop();
    }

    public void join() throws InterruptedException {
        jettyServer.join();
    }

    public static class Router extends RouterServlet {

        @Override
        protected Collection<Class<?>> getDeclaredActions() {
            Reflections reflections = new Reflections("eu.modernmt.api.actions");
            return reflections.getTypesAnnotatedWith(Route.class);
        }
    }

}

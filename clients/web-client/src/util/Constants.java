package util;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Constants {
    public static final int SERVER_PORT = initServerPort();
    public static final String SERVER_HOST = "fe80::5fdc:12f6:3534:6b02%17";
    public static final String OK_STATUS = "HTTP/1.0 200 OK\r\n";
    public static final String NOT_FOUND_STATUS = "HTTP/1.0 404 Not Found\r\n";
    public static final String ERROR_STATUS = "HTTP/1.0 500 Internal Server Error\r\n";
    public static final String CONTENT_TYPE = "Content-Type: text/html; charset=UTF-8\r\n";
    public static final String RESOURCES = initResourcesPath();
    public static final String NOT_FOUND_PATH = RESOURCES + "/not_found.html";
    public static final String DEFAULT_BACKEND_HOST = "localhost:8080";
    public static String BACKEND_HOST = initBackendHost();

    private static int initServerPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 8081;
    }

    private static String initResourcesPath() {
        String envPath = System.getenv("RESOURCES_PATH");
        if (envPath != null && !envPath.isBlank()) {
            return envPath;
        }
        if (Files.exists(Paths.get("clients/web-client/resources"))) {
            return "clients/web-client/resources";
        }
        return "resources";
    }

    private static String initBackendHost() {
        String backendHost = System.getenv("BACKEND_HOST");
        if (backendHost == null || backendHost.isBlank()) {
            backendHost = Constants.DEFAULT_BACKEND_HOST;
        }
        return backendHost;
    }
}

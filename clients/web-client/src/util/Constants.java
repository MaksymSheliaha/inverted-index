package util;

public class Constants {
    public static final int SERVER_PORT = 8081;
    public static final String SERVER_HOST = "fe80::5fdc:12f6:3534:6b02%17";
    public static final String OK_STATUS = "HTTP/1.0 200 OK\r\n";
    public static final String NOT_FOUND_STATUS = "HTTP/1.0 404 Not Found\r\n";
    public static final String ERROR_STATUS = "HTTP/1.0 500 Internal Server Error\r\n";
    public static final String CONTENT_TYPE = "Content-Type: text/html; charset=UTF-8\r\n";
    public static final String RESOURCES = "clients/web-client/resources";
    public static final String NOT_FOUND_PATH = RESOURCES + "/not_found.html";
    public static final String DEFAULT_BACKEND_HOST = "localhost:8080";
    public static String BACKEND_HOST = DEFAULT_BACKEND_HOST;
}

package maze;

import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Setup {

    public static final String   DATABASE_URI_PROPERTY           = "databaseUri";
    public static final String   WEB_PATH_PROPERTY               = "webPath";
    public static final String   WEB_PORT_PROPERTY               = "webPort";

    public static final String   DEFAULT_STORE_URI               = "postgresql://postgres:filechain@localhost:5432/";
    public static final String   DATABASE_PREFIX                 = "maze-";
    public static final String   DATABASE_ID                     = DATABASE_PREFIX + "db";

    public static final String   DEFAULT_WEB_PATH                = "https://unity.filechain.com";
    public static final Integer  DEFAULT_WEB_PORT                = 35353;

    public static final int      HTTP_THREAD_COUNT               = 5;

    public static final String   TEXT_MEDIA_TYPE                 = "text/plain";
    public static final String   HTML_MEDIA_TYPE                 = "text/html";
    public static final String   CSS_MEDIA_TYPE                  = "text/css";
    public static final String   JSON_MEDIA_TYPE                 = "application/json";

    public static final Path     SSL_PATH                        = Path.of("ssl");
    public static final Path     KEY_STORE_PATH                  = SSL_PATH.resolve("keystore.jks");
    public static final String   KEY_STORE_PASSWORD              = "OBF:1m821ku31jg81m0x1knm1kj01lx91jd21kqr1m4c";
    public static final Path     TRUST_STORE_PATH                = SSL_PATH.resolve("truststore");
    public static final String   TRUST_STORE_PASSWORD            = "OBF:1m821ku31jg81m0x1knm1kj01lx91jd21kqr1m4c";

    public static final Path     WEB_PATH                        = Path.of("web");
    public static final Path     LOGIN_PAGE                      = WEB_PATH.resolve("login.html");
    public static final Path     DASHBOARD_PAGE                  = WEB_PATH.resolve("dashboard.html");

    public static final String   UI_PATH                         = "/ui";

    public static final String   SESSION_TOKEN                   = "JSESSIONID";
    public static final String   SESSION_USER_ID_ATTRIBUTE       = "user-id";
    public static final String   SESSION_USER_PASSWORD_ATTRIBUTE = "user-password";

    public static final Duration SESSION_TIME_OUT                = Duration.ofMinutes(10);

    public static final Duration REST_CALL_TIME_OUT              = Duration.ofMinutes(2);

    public static final String   CONTENT_TYPE_ATTRIBUTE          = "Content-Type";
    public static final String   CONTENT_LENGTH_ATTRIBUTE        = "Content-Length";

    public static final String   REST_LOGGER                     = "maze.rest";

    // Pattern to accept a valid user name
    public static final Pattern  USER_NAME_PATTERN               = Pattern.compile("[A-Za-z0-9_-]+");

    // Pattern to accept a valid password
    public static final Pattern  USER_PASSWORD_PATTERN           = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%&])(?=.{8,})");

    // Set limits for the grid size of a maze (not necessarily a square)
    public static final String[] MAZE_COLUMNS                    = IntStream.rangeClosed(65, 90)
                                                                            .mapToObj(Character::toString)
                                                                            .toArray(String[]::new);

    public static final int      MAZE_ROW_MAX                    = 30;

    // Set maximum amount of empty space to actually end up with a maze
    public static final int      MAZE_EMPTY_AREA_MAX             = 4;

}

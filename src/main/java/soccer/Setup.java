package soccer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import soccer.model.PlayerType;

public class Setup {

    public static final String                   DATABASE_URI_PROPERTY       = "databaseUri";
    public static final String                   WEB_PATH_PROPERTY           = "webPath";
    public static final String                   WEB_PORT_PROPERTY           = "webPort";

    public static final String                   DEFAULT_STORE_URI           = "postgresql://postgres:filechain@localhost:5432/";
    public static final String                   DATABASE_PREFIX             = "soccer-";
    public static final String                   DATABASE_ID                 = DATABASE_PREFIX + "db";

    public static final String                   DEFAULT_WEB_PATH            = "https://unity.filechain.com";
    public static final Integer                  DEFAULT_WEB_PORT            = 35353;

    public static final int                      HTTP_THREAD_COUNT           = 5;

    public static final String                   TEXT_MEDIA_TYPE             = "text/plain";
    public static final String                   HTML_MEDIA_TYPE             = "text/html";
    public static final String                   CSS_MEDIA_TYPE              = "text/css";
    public static final String                   JSON_MEDIA_TYPE             = "application/json";

    public static final Path                     WEB_PATH                    = Path.of("web");
    public static final Path                     LOGIN_PAGE                  = WEB_PATH.resolve("login.html");
    public static final Path                     DASHBOARD_PAGE              = WEB_PATH.resolve("dashboard.html");

    public static final String                   UI_PATH                     = "/ui";

    public static final String                   USER_TOKEN                  = "User-Token";
    public static final Duration                 SESSION_TIME_OUT            = Duration.ofMinutes(10);

    public static final Duration                 REST_CALL_TIME_OUT          = Duration.ofMinutes(2);

    public static final String                   CONTENT_TYPE_ATTRIBUTE      = "Content-Type";
    public static final String                   CONTENT_LENGTH_ATTRIBUTE    = "Content-Length";

    public static final String                   REST_LOGGER                 = "soccer.rest";

    // Pattern to accept a valid email as username
    public static final Pattern                  USER_NAME_PATTERN           = Pattern.compile("^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");

    // Pattern to accept a valid password
    public static final Pattern                  USER_PASSWORD_PATTERN       = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%&])(?=.{8,})");

    // Initial set of new players
    public static final Map<PlayerType, Integer> DEFAULT_TEAM_SIZE           = Map.of(PlayerType.GOAL_KEEPER,
                                                                                      3,
                                                                                      PlayerType.DEFENDER,
                                                                                      6,
                                                                                      PlayerType.MIDFIELDER,
                                                                                      6,
                                                                                      PlayerType.ATTACKER,
                                                                                      5);

    // Initial age of a new player
    public static final Supplier<Integer>        DEFAULT_PLAYER_AGE_SUPPLIER = () -> 18 + ThreadLocalRandom.current()
                                                                                                           .nextInt(23);

    // Initial value of a new player
    public static final Long                     DEFAULT_PLAYER_ASSET_VALUE  = 1000000L;

    // Initial BALANCE of a new Team
    public static final Long                     DEFAULT_TEAM_BALANCE        = 5000000L;

    // Player NOT in Transfer
    public static final Long                     PLAYER_NO_TRANSFER_VALUE    = 0L;

}

package soccer.base;

public enum RestCode {
                      OK,
                      BAD_REQUEST,
                      FORBIDDEN,
                      NOT_FOUND,
                      TIMEOUT,
                      INTERNAL_FAILURE,
                      NOT_AVAILABLE;

    public static int toInt(RestCode restCode) {

        return restCode.ordinal();
    }

    public static RestCode fromInt(int restCode) {

        switch (restCode) {
        case 0:
            return OK;
        case 1:
            return BAD_REQUEST;
        case 2:
            return FORBIDDEN;
        case 3:
            return NOT_FOUND;
        case 4:
            return TIMEOUT;
        case 5:
            return INTERNAL_FAILURE;
        case 6:
            return NOT_AVAILABLE;
        default:
            return INTERNAL_FAILURE;
        }
    }

}

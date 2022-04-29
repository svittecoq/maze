package soccer.base;

import java.util.Objects;

public class RestOutput<T_Output> {

    public static final RestOutput<Result>  OK               = RestOutput.ok(Result.OK);
    public static final RestOutput<Boolean> TRUE             = RestOutput.ok(Boolean.TRUE);
    public static final RestOutput<Boolean> FALSE            = RestOutput.ok(Boolean.FALSE);

    private static final RestOutput<?>      BAD_REQUEST      = new RestOutput<>(RestCode.BAD_REQUEST);
    private static final RestOutput<?>      FORBIDDEN        = new RestOutput<>(RestCode.FORBIDDEN);
    private static final RestOutput<?>      NOT_FOUND        = new RestOutput<>(RestCode.NOT_FOUND);
    private static final RestOutput<?>      TIMEOUT          = new RestOutput<>(RestCode.TIMEOUT);
    private static final RestOutput<?>      INTERNAL_FAILURE = new RestOutput<>(RestCode.INTERNAL_FAILURE);
    private static final RestOutput<?>      NOT_AVAILABLE    = new RestOutput<>(RestCode.NOT_AVAILABLE);

    private final T_Output                  _output;
    private final RestCode                  _restCode;

    public RestOutput(RestCode restCode) {
        this(null, restCode);
    }

    public RestOutput(T_Output output, RestCode restCode) {

        _output = output;
        _restCode = restCode;
    }

    public T_Output output() {
        return _output;
    }

    public RestCode restCode() {
        return _restCode;
    }

    public boolean isOK() {

        return ((restCode() == RestCode.OK) && (output() != null));
    }

    public boolean isBadRequest() {

        return (restCode() == RestCode.BAD_REQUEST);
    }

    public boolean isForbidden() {

        return (restCode() == RestCode.FORBIDDEN);
    }

    public boolean isNotFound() {

        return (restCode() == RestCode.NOT_FOUND);
    }

    public boolean isTimeout() {

        return (restCode() == RestCode.TIMEOUT);
    }

    public boolean isInternalFailure() {

        return (restCode() == RestCode.INTERNAL_FAILURE);
    }

    public boolean isNotAvailable() {

        return (restCode() == RestCode.NOT_AVAILABLE);
    }

    public T_Output stream() {

        if (!isOK()) {
            Api.error("RestOutput is NOT OK", this);
            return null;
        }

        return output();
    }

    public static boolean isOK(RestOutput<?> restOutput) {

        if (restOutput == null) {
            return false;
        }
        return restOutput.isOK();
    }

    public static boolean isNOK(RestOutput<?> restOutput) {

        if (restOutput == RestOutput.OK) {
            return false;
        }

        if (restOutput == null) {
            return true;
        }

        return (restOutput.isOK() == false);
    }

    public static boolean isBadRequest(RestOutput<?> restOutput) {

        if (Api.isNull(restOutput)) {
            Api.error("restOutput is null");
            return false;
        }
        return restOutput.isBadRequest();
    }

    public static boolean isForbidden(RestOutput<?> restOutput) {

        if (Api.isNull(restOutput)) {
            Api.error("restOutput is null");
            return false;
        }
        return restOutput.isForbidden();
    }

    public static boolean isNotFound(RestOutput<?> restOutput) {

        if (Api.isNull(restOutput)) {
            Api.error("restOutput is null");
            return false;
        }
        return restOutput.isNotFound();
    }

    public static boolean isNotAvailable(RestOutput<?> restOutput) {

        if (Api.isNull(restOutput)) {
            Api.error("restOutput is null");
            return false;
        }
        return restOutput.isNotAvailable();
    }

    public static <T> boolean isOK(RestOutput<T> restOutput, T output) {

        if (isNOK(restOutput)) {
            return false;
        }
        if (output != restOutput.output()) {
            Api.error("output does not match", output, restOutput);
            return false;
        }
        return true;
    }

    public static <T> RestOutput<T> ok(T output) {

        if (Api.isNull(output)) {
            Api.error("output is null. INTERNAL FAILURE");
            return RestOutput.internalFailure();
        }
        return new RestOutput<T>(output, RestCode.OK);
    }

    @SuppressWarnings("unchecked")
    public static <T> RestOutput<T> badRequest() {

        return (RestOutput<T>) BAD_REQUEST;
    }

    @SuppressWarnings("unchecked")
    public static <T> RestOutput<T> forbidden() {

        return (RestOutput<T>) FORBIDDEN;
    }

    @SuppressWarnings("unchecked")
    public static <T> RestOutput<T> notFound() {

        return (RestOutput<T>) NOT_FOUND;
    }

    @SuppressWarnings("unchecked")
    public static <T> RestOutput<T> timeout() {

        return (RestOutput<T>) TIMEOUT;
    }

    @SuppressWarnings("unchecked")
    public static <T> RestOutput<T> internalFailure() {

        return (RestOutput<T>) INTERNAL_FAILURE;
    }

    @SuppressWarnings("unchecked")
    public static <T> RestOutput<T> notAvailable() {

        return (RestOutput<T>) NOT_AVAILABLE;
    }

    public static <T> RestOutput<T> of(RestOutput<?> restOutput) {

        if (Objects.isNull(restOutput)) {
            Api.error("restOutput is null");
            return RestOutput.internalFailure();
        }
        return new RestOutput<T>(restOutput.restCode());
    }

    @Override
    public String toString() {
        return "RestOutput [_output=" + _output + ", _restCode=" + _restCode + "]";
    }
}
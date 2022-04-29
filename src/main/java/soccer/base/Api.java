package soccer.base;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Api {

    private static final DateTimeFormatter TimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String            Margin        = " ".repeat(12);

    private static String buildLogMessage(Object... objects) {

        int index = 1;
        StringBuilder stringBuilder = new StringBuilder(LocalTime.now().format(TimeFormatter));

        for (Object object : objects) {

            if (index > 1) {
                stringBuilder.append(System.lineSeparator()).append(Margin);
            }
            stringBuilder.append("[").append(index).append("] ");
            stringBuilder.append(object);
            index++;
        }

        return stringBuilder.toString();
    }

    public static void error(Object... objects) {

        System.err.println(buildLogMessage(objects));
    }

    public static void error(Throwable throwable, Object... objects) {

        throwable.printStackTrace(System.err);
        System.err.println(buildLogMessage(objects));
    }

    public static void info(Object... objects) {

        System.out.println(buildLogMessage(objects));
    }

    private static boolean checkIfNull(Object object, int i) {

        StackTraceElement[] stackTrace;
        StringBuilder stringBuilder;

        if (object != null) {
            return false;
        }

        stackTrace = Thread.currentThread().getStackTrace();

        String methodName = stackTrace[3].getMethodName();

        stringBuilder = new StringBuilder(methodName + "() : parameter " + i + " is NULL");
        for (int index = 3; index < stackTrace.length; index++) {
            stringBuilder.append(System.lineSeparator()).append(stackTrace[index].toString());
        }

        error(stringBuilder.toString());

        return true;
    }

    public static boolean isNull(Object... objects) {

        boolean isNull = false;

        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                isNull = true;
                break;
            }
        }
        if (isNull == false) {
            return false;
        }

        for (int i = 0; i < objects.length; i++) {
            if (checkIfNull(objects[i], i + 1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNullArray(Object[] array) {

        if (array != null) {
            return false;
        }
        return (checkIfNull(array, 1));
    }

    public static RestOutput<URL> locateResource(Path resourcePath, ClassLoader classLoader) {

        URL resource;

        if (Api.isNull(resourcePath, classLoader)) {
            return RestOutput.badRequest();
        }

        resource = classLoader.getResource(resourcePath.toString().replace("\\", "/"));
        if (resource == null) {
            return RestOutput.notFound();
        }

        return RestOutput.ok(resource);
    }

    public static InputStream openResourceStream(Path resourcePath) {

        if (Api.isNull(resourcePath)) {
            return null;
        }

        InputStream inputStream = Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream(resourcePath.toString().replace("\\", "/"));
        if (inputStream == null) {
            error("Resource to openResourceStream can not be loaded", resourcePath);
            return null;
        }

        return inputStream;
    }

    public static RestOutput<byte[]> loadResource(Path resourcePath) {

        if (Api.isNull(resourcePath)) {
            return RestOutput.badRequest();
        }

        try (InputStream inputStream = Thread.currentThread()
                                             .getContextClassLoader()
                                             .getResourceAsStream(resourcePath.toString().replace("\\", "/"))) {
            if (inputStream == null) {
                error("Resource can not be loaded. NOT FOUND", resourcePath);
                return RestOutput.notFound();
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[8092];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                byteArrayOutputStream.write(data, 0, nRead);
            }

            byteArrayOutputStream.close();

            return RestOutput.ok(byteArrayOutputStream.toByteArray());
        } catch (Throwable t) {
            error(t, "Failure to load resource. INTERNAL FAILURE", resourcePath);
            return RestOutput.internalFailure();
        }
    }

    public static RestOutput<List<String>> loadHtmlPage(Path htmlPagePath) {

        RestOutput<byte[]> resourceByteArrayOutput;
        byte[] resourceByteArray;
        List<String> htmlPage;
        String line;

        if (Api.isNull(htmlPagePath)) {
            return RestOutput.badRequest();
        }

        // Load the Resource
        resourceByteArrayOutput = loadResource(htmlPagePath);
        if (RestOutput.isNOK(resourceByteArrayOutput)) {
            Api.error("loadHtmlPage is NOT OK", resourceByteArrayOutput, htmlPagePath);
            return RestOutput.of(resourceByteArrayOutput);
        }
        resourceByteArray = resourceByteArrayOutput.output();

        htmlPage = new ArrayList<String>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(resourceByteArray,
                                                                                                               0,
                                                                                                               resourceByteArray.length)))) {

            while ((line = bufferedReader.readLine()) != null) {
                htmlPage.add(line);
            }
        } catch (IOException e) {
            Api.error(e, "loadHtmlPage failed. INTERNAL FAILURE", htmlPagePath);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(htmlPage);
    }

    public static ExecutorService executorService(String poolName) {

        return Executors.newCachedThreadPool(new BaseThreadFactory(poolName));
    }

    public static URI URI(String uri) {

        try {

            return new URI(uri);

        } catch (URISyntaxException e) {
            Api.error("URI is not valid", uri);
            return null;
        }
    }
}
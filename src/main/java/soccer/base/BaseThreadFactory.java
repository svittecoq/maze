package soccer.base;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseThreadFactory implements ThreadFactory {

    private final ThreadFactory _threadFactory = Executors.defaultThreadFactory();

    private final AtomicInteger _idGenerator   = new AtomicInteger(1);
    private final String        _prefix;

    public BaseThreadFactory(String poolName) {

        _prefix = poolName + "-";
    }

    @Override
    public Thread newThread(Runnable r) {

        Thread thread = _threadFactory.newThread(r);

        if (thread == null) {
            Api.error("newThread Failed", this);
            return null;
        }
        thread.setName(_prefix + _idGenerator.getAndIncrement());

        return thread;
    }

    @Override
    public String toString() {
        return "BaseThreadFactory [_threadFactory=" + _threadFactory
               + ", _idGenerator="
               + _idGenerator
               + ", _prefix="
               + _prefix
               + "]";
    }
}

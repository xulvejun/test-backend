package cn.xeblog.server.service;

import cn.xeblog.commons.entity.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author anlingyi
 * @date 2021/9/11 5:10 下午
 */
public class DefaultResponseHistoryService extends AbstractResponseHistoryService {

    private final ExecutorService threadPool = new ThreadPoolExecutor(1, 2, 60,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.CallerRunsPolicy());

    private final int size;

    private final Response[] queue;

    private final ReentrantReadWriteLock readWriteLock;

    private int count;

    private int takeIndex;

    private int putIndex;

    public DefaultResponseHistoryService() {
        this(50);
    }

    public DefaultResponseHistoryService(int size) {
        this.size = size;
        this.queue = new Response[size];
        this.readWriteLock = new ReentrantReadWriteLock(true);
    }

    @Override
    protected void addHistoryHandler(Response response) {
        threadPool.execute(() -> offer(response));
    }

    private boolean offer(Response response) {
        readWriteLock.writeLock().lock();
        try {
            if (count == size) {
                poll();
            }

            queue[putIndex] = response;
            count++;
            if (++putIndex == size) {
                putIndex = 0;
            }

            return true;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private Response poll() {
        if (count == 0) {
            return null;
        }

        Response obj = queue[takeIndex];
        queue[takeIndex] = null;
        count--;
        if (++takeIndex == size) {
            takeIndex = 0;
        }

        return obj;
    }

    @Override
    public List<Response> getHistory(int limit) {
        List<Response> list = new ArrayList<>(limit);

        readWriteLock.readLock().lock();
        try {
            int readCount = Math.min(limit, count);
            int start = takeIndex;
            int outLen = start - readCount;
            if (outLen >= 0) {
                start = outLen;
            } else {
                start -= readCount;
                if (start < 0) {
                    start += count;
                }
            }
            for (int i = 0; i < readCount; i++) {
                list.add(queue[start]);
                if (++start == size) {
                    start = 0;
                }
            }

            return list;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

}

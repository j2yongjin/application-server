package niotcpserver.Executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-11
 * @desc :
 */
public abstract class DefaultNioExecutors {

    ThreadPoolExecutor threadPoolExecutor;

    protected void createExecutors(Integer threadPoolCount, ThreadFactory threadFactory){
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolCount,threadFactory);
    }

    protected void shutDown(){
        threadPoolExecutor.shutdown();
    }

    protected Integer size(){
        return threadPoolExecutor.getPoolSize();
    }

    protected  Integer activeSize(){
        return threadPoolExecutor.getActiveCount();
    }





}

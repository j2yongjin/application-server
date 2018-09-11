package niotcpserver.Executor;

import java.util.concurrent.ThreadFactory;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-11
 * @desc :
 */
public class BossNioExecutors  extends  DefaultNioExecutors{

    Integer threadPoolCount;
    ThreadFactory threadFactory;

    public void start(){
        createExecutors(threadPoolCount,threadFactory);
    }

    public void exec(Runnable runnable){
        threadPoolExecutor.execute(runnable);
    }



}

package niotcpserver.Executor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-11
 * @desc :
 */
public class DefaultThreadFactoryBuilder {

    private String namePrefix;
    private Boolean daemon;
    private Integer priority;

    public DefaultThreadFactoryBuilder naemPrefix(String namePrefix){
        this.namePrefix = namePrefix;
        return this;
    }

    public DefaultThreadFactoryBuilder daemon(Boolean daemon){
        this.daemon = daemon;
        return this;
    }

    public DefaultThreadFactoryBuilder priority(Integer priority){
        this.priority = priority;
        return this;
    }
    public ThreadFactory build(){
        return build(this);
    }

    private  ThreadFactory build(DefaultThreadFactoryBuilder builder){

        return new ThreadFactory() {
            AtomicInteger threadOrder = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(builder.daemon);
                thread.setName(builder.namePrefix + "-" + threadOrder);
                thread.setPriority(builder.priority);
                return thread;
            }
        };

    }


}

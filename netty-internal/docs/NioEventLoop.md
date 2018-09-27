
## EventLoop
등록 된 채널에 대한 모든 I / O 작업을 처리합니다. 하나의 EventLoop 인스턴스는 일반적으로 둘 이상의 채널을 처리하지만 구현 세부 사항 및 내부 구조에 따라 다를 수 있습니다

## EventLoopGroup
이벤트 루프 중에 나중에 선택하기 위해 처리되는 채널을 등록 할 수있는 특수 EventExecutorGroup

## NioEventLoop
SingleThreadEventLoop 구현은 채널을 셀렉터에 등록하고 이벤트 루프에서 이들을 멀티 플레 싱 (multi-plexing)합니다.

## AbstractNioChannel
Abstract base class for Channel implementations which use a Selector based approach
### NioEventLoopGroup

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/NioEventLoopGroup_Diagram.png)


#### 사용예시

    EventLoopGroup bossGroup;
    EventLoopGroup wokerGroup;

    public void start(){
        bossGroup = new NioEventLoopGroup();
        wokerGroup = new NioEventLoopGroup();
        

#### 생성자
EventLoop 객체 생성

    public class NioEventLoopGroup extends MultithreadEventLoopGroup {
    ...
    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }
    
    
    
    public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {
    ...
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                                EventExecutorChooserFactory chooserFactory, Object... args) {
            if (nThreads <= 0) {
                throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
            }
    
            if (executor == null) {
                executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
            }
    
            children = new EventExecutor[nThreads];
    
            for (int i = 0; i < nThreads; i ++) {
                boolean success = false;
                try {
                    children[i] = newChild(executor, args);
                    success = true;
                } catch (Exception e) {
                    // TODO: Think about if this is a good exception type
                    throw new IllegalStateException("failed to create a child event loop", e);
                } finally {
                    if (!success) {
                        for (int j = 0; j < i; j ++) {
                            children[j].shutdownGracefully();
                        }
    
                        for (int j = 0; j < i; j ++) {
                            EventExecutor e = children[j];
                            try {
                                while (!e.isTerminated()) {
                                    e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                                }
                            } catch (InterruptedException interrupted) {
                                // Let the caller handle the interruption.
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }
    
            chooser = chooserFactory.newChooser(children);
    
            final FutureListener<Object> terminationListener = new FutureListener<Object>() {
                @Override
                public void operationComplete(Future<Object> future) throws Exception {
                    if (terminatedChildren.incrementAndGet() == children.length) {
                        terminationFuture.setSuccess(null);
                    }
                }
            };
    
            for (EventExecutor e: children) {
                e.terminationFuture().addListener(terminationListener);
            }
    
            Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
            Collections.addAll(childrenSet, children);
            readonlyChildren = Collections.unmodifiableSet(childrenSet);
        }


##### ThreadPerTaskExecutor

    public final class ThreadPerTaskExecutor implements Executor {
        private final ThreadFactory threadFactory;
    
        public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
            if (threadFactory == null) {
                throw new NullPointerException("threadFactory");
            }
            this.threadFactory = threadFactory;
        }
    
        @Override
        public void execute(Runnable command) {
            threadFactory.newThread(command).start();
        }
    }
    
##### DefaultThreadFactory

    스레드 팩토리 생성자
    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        if (poolName == null) {
            throw new NullPointerException("poolName");
        }
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }

        prefix = poolName + '-' + poolId.incrementAndGet() + '-';
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }
    
    스레드 객체 생성
    @Override
    public Thread newThread(Runnable r) {
        Thread t = newThread(FastThreadLocalRunnable.wrap(r), prefix + nextId.incrementAndGet());
        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) {
            // Doesn't matter even if failed to set.
        }
        return t;
    }
    
##### EventExecutor

EventExecutor는 이벤트 루프에서 Thread가 실행되는지 확인하는 편리한 메소드와 함께 제공되는 특별한 EventExecutorGroup입니다. 이 외에도 EventExecutorGroup을 확장하여 메서드에 액세스 할 수있는 일반적인 방법을 제공합니다

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/EventExecutor.png)


##### DefaultEventExecutor
제출 된 모든 작업을 직렬 방식으로 실행하는 기본 SingleThreadEventExecutor 구현

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/DefaultEventExecutor.png)

###### SingleThreadEventExecutor
제출 된 모든 태스크를 단일 thread로 실행하는, OrderedEventExecutor의 추상 기본 클래스입니다.

    생성자
    
    protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor,
                                          boolean addTaskWakesUp, int maxPendingTasks,
                                          RejectedExecutionHandler rejectedHandler) {
          super(parent);
          this.addTaskWakesUp = addTaskWakesUp;
          this.maxPendingTasks = Math.max(16, maxPendingTasks);
          this.executor = ObjectUtil.checkNotNull(executor, "executor");
          taskQueue = newTaskQueue(this.maxPendingTasks);
          rejectedExecutionHandler = ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler");
     }

     protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
             return new LinkedBlockingQueue<Runnable>(maxPendingTasks);
     }

     protected static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
             for (;;) {
                 Runnable task = taskQueue.poll();
                 if (task == WAKEUP_TASK) {
                     continue;
                 }
                 return task;
             }
     }
     
     protected Runnable takeTask()
      ...
      
     private boolean fetchFromScheduledTaskQueue() {
     ...
     
   
###### OrderedEventExecutor
제출 된 모든 태스크를 순서 붙이고 / 직렬 방식으로 처리하는 EventExecutor의 마커 인터페이스입니다.

###### AbstractScheduledEventExecutor

스케줄링을 지원하고 싶은 EventExecutor의 추상 기본 클래스입니다.

    PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue() {
        if (scheduledTaskQueue == null) {
            scheduledTaskQueue = new DefaultPriorityQueue<ScheduledFutureTask<?>>(
                    SCHEDULED_FUTURE_TASK_COMPARATOR,
                    // Use same initial capacity as java.util.PriorityQueue
                    11);
        }
        return scheduledTaskQueue;
    }
    
    <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
            if (inEventLoop()) {
                scheduledTaskQueue().add(task);
            } else {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        scheduledTaskQueue().add(task);
                    }
                });
            }
    
            return task;
    }


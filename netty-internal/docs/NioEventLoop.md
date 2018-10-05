
## EventLoop
등록 된 채널에 대한 모든 I / O 작업을 처리합니다. 하나의 EventLoop 인스턴스는 일반적으로 둘 이상의 채널을 처리하지만 
구현 세부 사항 및 내부 구조에 따라 다를 수 있습니다

## EventLoopGroup
이벤트 루프 중에 나중에 선택하기 위해 처리되는 채널을 등록 할 수있는 
특별한 EventExecutorGroup

## NioEventLoop
SingleThreadEventLoop 구현은 채널을 셀렉터에 등록하고 이벤트 루프에서 이들을 멀티 플레 싱 (multi-plexing)합니다.

## AbstractNioChannel

Selector 기반의 방법을 사용하는 채널 구현의 추상 기본 클래스

## MultithreadEventLoopGroup

동시에 여러 스레드로 작업을 처리하는 EventLoopGroup 구현을위한 추상 기본 클래스입니다.

## MultithreadEventExecutorGroup

동시에 여러 스레드로 작업을 처리하는 EventExecutorGroup 구현을위한 추상 기본 클래스입니다.

## AbstractEventExecutorGroup

EventExecutorGroup 클래스에 대한 추상 베이스 클래스

## EventExecutorGroup
 EventExecutorGroup은 EventExecutor가 제공하는것을 next() 함수를 통해 제공합니다. 이외에도 그들의 생명주기를 관리하고
 글로벌 방식으로 차단할수 있습니다.
 


### NioEventLoopGroup
    NIO Selector 기반 채널에 사용되는 MultithreadEventLoopGroup 구현

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

사용사례

    if (executor == null) {
                executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
     }


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
    

    
### 이벤트 루프 객체 생성
    children = new EventExecutor[nThreads];

    for (int i = 0; i < nThreads; i ++) {
        boolean success = false;
        try {
            children[i] = newChild(executor, args);


    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return new NioEventLoop(this, executor, (SelectorProvider) args[0],
            ((SelectStrategyFactory) args[1]).newSelectStrategy(), (RejectedExecutionHandler) args[2]);
    }
    
    // NioEventLoop 객체를 생성한다.
    
## NioEventLoop
![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/NioEventLoop.png)

    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, false, DEFAULT_MAX_PENDING_TASKS, rejectedExecutionHandler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        provider = selectorProvider;
        final SelectorTuple selectorTuple = openSelector();
        selector = selectorTuple.selector;
        unwrappedSelector = selectorTuple.unwrappedSelector;
        selectStrategy = strategy;
    }

### SingleThreadEventLoop
제출 된 모든 작업을 단일 스레드에서 실행하는 EventLoop의 추상 기본 클래스입니다.

    protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor,
                                    boolean addTaskWakesUp, int maxPendingTasks,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
        tailTasks = newTaskQueue(maxPendingTasks);
    }

    @Override
    public EventLoopGroup parent() {
        return (EventLoopGroup) super.parent();
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return register(new DefaultChannelPromise(channel, this));
    }

    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        promise.channel().unsafe().register(this, promise);
        return promise;
    }

    @Deprecated
    @Override
    public ChannelFuture register(final Channel channel, final ChannelPromise promise) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (promise == null) {
            throw new NullPointerException("promise");
        }

        channel.unsafe().register(this, promise);
        return promise;
    }



### NioEventLoop
SingleThreadEventLoop 구현은 채널을 셀렉터에 등록하고 이벤트 루프에서 이들을 멀티 플레 싱 (multi-plexing)합니다.


    
##### EventExecutor
EventExecutor는 이벤트 루프에서 Thread가 실행되는지 확인하는 편리한 메소드와 함께 제공되는 특별한 EventExecutorGroup입니다. 이 외에도 EventExecutorGroup을 확장하여 메서드에 액세스 할 수있는 일반적인 방법을 제공합니다

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/EventExecutor.png)


##### DefaultEventExecutor
제출 된 모든 작업을 직렬 방식으로 실행하는 기본 SingleThreadEventExecutor 구현

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/DefaultEventExecutor.png)

###### SingleThreadEventExecutor
제출 된 모든 태스크를 단일 thread로 실행하는, OrderedEventExecutor의 추상 기본 클래스입니다.

    생성자
    public abstract class SingleThreadEventExecutor extends AbstractScheduledEventExecutor implements OrderedEventExecutor {
    ...
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
    
    
##### NioEventLoopGroup 

   NioEventLoopGroup은 

사용예시
    private final EventExecutor[] children;
    children[i] = newChild(executor, args);

함수 호출

    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return new NioEventLoop(this, executor, (SelectorProvider) args[0],
            ((SelectStrategyFactory) args[1]).newSelectStrategy(), (RejectedExecutionHandler) args[2]);
    }
    
NioEventLoop 생성자

    public final class NioEventLoop extends SingleThreadEventLoop {
    ...
    
    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                     SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler) {
            super(parent, executor, false, DEFAULT_MAX_PENDING_TASKS, rejectedExecutionHandler);
            if (selectorProvider == null) {
                throw new NullPointerException("selectorProvider");
            }
            if (strategy == null) {
                throw new NullPointerException("selectStrategy");
            }
            provider = selectorProvider;
            final SelectorTuple selectorTuple = openSelector();
            selector = selectorTuple.selector;
            unwrappedSelector = selectorTuple.unwrappedSelector;
            selectStrategy = strategy;
     }
    
SingleThreadEventLoop 생성자
     
     public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {
     ...
     private final Queue<Runnable> tailTasks;
        
    ...
    protected SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory,
                                    boolean addTaskWakesUp, int maxPendingTasks,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, threadFactory, addTaskWakesUp, maxPendingTasks, rejectedExecutionHandler);
        tailTasks = newTaskQueue(maxPendingTasks);  // 큐 생성
    }


##### DefaultEventExecutorChooserFactory

단순한 라운드 로빈을 사용해 다음의 EventExecutor를 선택하는 디폴트의 구현입니다

    public final class DefaultEventExecutorChooserFactory implements EventExecutorChooserFactory {
    
    public EventExecutorChooser newChooser(EventExecutor[] executors) {
            if (isPowerOfTwo(executors.length)) {
                return new PowerOfTwoEventExecutorChooser(executors);
            } else {
                return new GenericEventExecutorChooser(executors);
            }
    }
    
    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {
            private final AtomicInteger idx = new AtomicInteger();
            private final EventExecutor[] executors;
    
            PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
                this.executors = executors;
            }
    
            @Override
            public EventExecutor next() {
                return executors[idx.getAndIncrement() & executors.length - 1];
            }
        }
        
##### FutureListener , EventExecutor
FutureListener

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/FutureListener.png)

사용예시
이벤트 등록

    final FutureListener<Object> terminationListener = new FutureListener<Object>() {
        @Override
        public void operationComplete(Future<Object> future) throws Exception {
            if (terminatedChildren.incrementAndGet() == children.length) {
                terminationFuture.setSuccess(null);
            }
        }
    };
    
    
사용예시
정상적으로 child eventExecutor 생성 성공 이벤트 등록

이 이벤트 매니저에 의해 관리되고있는 모든 EventExecutor가 통지되었을 때에 통지되는 Future를 돌려줍니다.
EventExecutorGroup이 종료되었습니다.

    for (EventExecutor e: children) {
        e.terminationFuture().addListener(terminationListener);
    }
    
##### DefaultPromise
구현

    public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {
    ...
    @Override
    public <V> Promise<V> newPromise() {
        return new DefaultPromise<V>(this);
    }

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/DefaultPromise.png)
    

    
###### addListener

    @Override
    public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            addListener0(listener);
        }

        if (isDone()) {
            notifyListeners();
        }

        return this;
    }
###### setSuccess()
    public Promise<V> setSuccess(V result) {
            if (setSuccess0(result)) {
                notifyListeners();
                return this;
            }
            throw new IllegalStateException("complete already: " + this);
        }
###### notifyListeners()
    private void notifyListeners() {
        EventExecutor executor = executor();
        if (executor.inEventLoop()) {
            final InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
            final int stackDepth = threadLocals.futureListenerStackDepth();
            if (stackDepth < MAX_LISTENER_STACK_DEPTH) {
                threadLocals.setFutureListenerStackDepth(stackDepth + 1);
                try {
                    notifyListenersNow();
                } finally {
                    threadLocals.setFutureListenerStackDepth(stackDepth);
                }
                return;
            }
        }

        safeExecute(executor, new Runnable() {
            @Override
            public void run() {
                notifyListenersNow();
            }
        });
    }


#### GlobalEventExecutor
싱글 스레드 싱글톤 EventExecutor. 스레드를 자동으로 시작하고 1 초 동안 태스크 큐에 보류중인 태스크가 없을 때 스레드를 중지합니다. 이 집행자에게 많은 수의 작업을 예약하는 것은 확장 가능하지 않습니다. 
전용 executor를 사용한다



![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/GloalEventExecutor.png)

#### BossGroup 갭쳐

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/bossGroup_memory.png)

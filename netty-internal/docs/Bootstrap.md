
### Bootstrap


#### ServerBootstrp

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/ServerBootstrap.png)

##### 서버 사용예시
부트스트랩 설정 예시

    ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,wokerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new SocketChannelInitializer())
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(serverConfig.getTcpPort()));
            
       
##### 주요 함수

##### 생성자
    public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel>{
    ...
    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        childGroup = bootstrap.childGroup;
        childHandler = bootstrap.childHandler;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
        synchronized (bootstrap.childAttrs) {
            childAttrs.putAll(bootstrap.childAttrs);
        }
    }
    ...

    public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {
    ...
    AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        handler = bootstrap.handler;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options) {
            options.putAll(bootstrap.options);
        }
        synchronized (bootstrap.attrs) {
            attrs.putAll(bootstrap.attrs);
        }
    }
    ...

##### ChannelFactory
채널 팩토리 생성

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/channelFactory.png)

채널 팩토리 생성 예제

    public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {
    ...
    public B channel(Class<? extends C> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        return channelFactory(new ReflectiveChannelFactory<C>(channelClass));
    } 
...


##### ServerBootstrapAcceptor
신규 connection 발생시 호출하는 클래스 

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/ServerBootStrapAcceptor.png)

클래스 등록

    @Override
    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            setChannelOptions(channel, options, logger);
        }

        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(0));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(0));
        }

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }

##### group

    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }
        
#### 등록

    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            channel = channelFactory.newChannel();
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }

###### 채널 생성
NioServerChannel 생성

    public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {
    ...    
    
    @Override
    public T newChannel() {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + clazz, t);
        }
    }
    
사용예시
    channel = channelFactory.newChannel()
    
###### init
childGroup를 등록하고 bossGroup은  AbstractBootstrap 에서 관리

    public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {
    ...
    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            setChannelOptions(channel, options, logger);
        }

        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(0));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(0));
        }

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
    
###### 등록

    // config().group() 은 bossGroup
   
    ChannelFuture regFuture = config().group().register(channel);

    public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {
    ...
    public ChannelFuture register(Channel channel) {
        return register(new DefaultChannelPromise(channel, this));
    }
    
###### ChannelFuture
     
비동기 채널 I / O 작업의 결과입니다.
Netty의 모든 I / O 조작은 비동기입니다. 즉, 요청 된 I / O 작업이 통화의 끝에서 완료되었다는 보장없이 모든 I / O 호출이 즉시 반환됨을 의미합니다. 대신 I / O 작업의 결과 나 상태에 대한 정보를 제공하는 ChannelFuture 인스턴스와 함께 반환됩니다

ChannelFuture가 완료되지 않았거나 완료되었습니다. I / O 작업이 시작되면 새로운 미래 개체가 만들어집니다. 새로운 미래는 처음에는 완료되지 않았습니다. I / O 작업이 아직 완료되지 않았으므로 성공하지도 못했거나 취소되지도 않았습니다. I / O 작업이 성공적으로 완료되거나 실패 또는 취소로 완료되면 미래에는 실패 원인과 같은보다 구체적인 정보가 포함 된 완료된 것으로 표시됩니다. 실패 및 취소조차도 완료 상태에 속합니다.

    
                                          +---------------------------+
                                          | Completed successfully    |
                                          +---------------------------+
                                     +---->      isDone() = true      |
     +--------------------------+    |    |   isSuccess() = true      |
     |        Uncompleted       |    |    +===========================+
     +--------------------------+    |    | Completed with failure    |
     |      isDone() = false    |    |    +---------------------------+
     |   isSuccess() = false    |----+---->      isDone() = true      |
     | isCancelled() = false    |    |    |       cause() = non-null  |
     |       cause() = null     |    |    +===========================+
     +--------------------------+    |    | Completed by cancellation |
                                     |    +---------------------------+
                                     +---->      isDone() = true      |
                                          | isCancelled() = true      |
   

I / O 작업이 완료되었는지 확인하고 완료를 기다리고 I / O 작업 결과를 검색 할 수있는 다양한 방법이 제공됩니다. 또한 I / O 작업이 완료 될 때 알림을받을 수 있도록 ChannelFutureListeners를 추가 할 수 있습니다
                                          +---------------------------+
I / O 조작이 완료되었을 때 알림을 받고 후속 작업을 수행하려면 가능하면 addwaer (GenericFutureListener)를 await ()에 선호하는 것이 좋습니다.
addListener (GenericFutureListener)는 비 블로킹입니다. 지정된 ChannelFutureListener를 ChannelFuture에 추가하기 만하면 미래와 관련된 I / O 작업이 완료 될 때 I / O 스레드가 리스너에 알립니다. ChannelFutureListener는 전혀 차단하지 않기 때문에 최상의 성능과 리소스 사용률을 제공하지만 이벤트 중심 프로그래밍에 익숙하지 않은 경우 순차 논리를 구현하는 것이 까다로울 수 있습니다
반대로 await ()는 차단 작업입니다. 일단 호출되면 호출자 스레드는 작업이 완료 될 때까지 차단합니다. await ()를 사용하여 순차 논리를 구현하는 것이 더 쉽지만 호출자 스레드는 I / O 연산이 완료되고 스레드 간 알림의 비용이 상대적으로 많이들 때까지 불필요하게 차단됩니다. 더욱이, 아래에 설명 된 특정 상황에서 데드 록 (dead lock)의 가능성이 있습니다.
     
       
    
    
#######  DefaultChannelPromise
![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/DefaultChannelPromise.png)   
    
    public class DefaultChannelPromise extends DefaultPromise<Void> implements ChannelPromise, FlushCheckpoint {
    ...
    public DefaultChannelPromise(Channel channel, EventExecutor executor) {
        super(executor);
        this.channel = checkNotNull(channel, "channel");
    }
    
    public abstract class AbstractChannel extends DefaultAttributeMap implements Channel { 
    ...
    public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }
            if (isRegistered()) {
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) {
                promise.setFailure(
                        new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            AbstractChannel.this.eventLoop = eventLoop;

            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }

        
##### NioServerSocketChannel
    io.netty.channel.socket.ServerSocketChannel 구현체.
     새로운 연결을 받아들이는 NIO selector 기반 구현
     
![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/NioServerSocketChannel_diagram.png)     
     
###### AbstractNioMessageChannel
AbstractNioChannel 메세지를 조작하는 채널의 기본 클래스.

###### AbstractNioChannel
Selector 기반의 접근 방식을 사용하는 Channel 구현을위한 추상 기본 클래스입니다.

    protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "Failed to close a partially initialized socket.", e2);
                }
            }

            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }

 
###### AbstractChannel
골격(기본뼈대) 채널 구현

    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }
    
    /**
     * Returns a new {@link DefaultChannelPipeline} instance.
     */
    protected DefaultChannelPipeline newChannelPipeline() {
        return new DefaultChannelPipeline(this);
    }
    
    
    
    

##### DefaultChannelPipeline
기본 ChannelPipeline 구현입니다. 채널이 생성 될 때 채널 구현에 의해 일반적으로 만들어집니다.
![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/DefaultChannelPipeline.png)

    protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
    
        voidPromise =  new VoidChannelPromise(channel, true);

        tail = new TailContext(this);
        head = new HeadContext(this);

        head.next = tail;
        tail.prev = head;
    }

 
    private AbstractChannelHandlerContext newContext(EventExecutorGroup group, String name, ChannelHandler handler) {
         return new DefaultChannelHandlerContext(this, childExecutor(group), name, handler);
     }

###### ChannelPipeline


##### DefaultChannelHandlerContext

![클래스다이어그램](https://github.com/j2yongjin/application-server/blob/master/netty-internal/assets/DefaultChannelHandlerContext.png)

    DefaultChannelHandlerContext(
            DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {
        super(pipeline, executor, name, isInbound(handler), isOutbound(handler));
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
    }
    
##### DefaultChannelHandlerContext
    abstract class AbstractChannelHandlerContext extends DefaultAttributeMap
             implements ChannelHandlerContext, ResorceLeakHint {

    AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor, String name,
                                  boolean inbound, boolean outbound) {
        this.name = ObjectUtil.checkNotNull(name, "name");
        this.pipeline = pipeline;
        this.executor = executor;
        this.inbound = inbound;
        this.outbound = outbound;
        // Its ordered if its driven by the EventLoop or the given Executor is an instanceof OrderedEventExecutor.
        ordered = executor == null || executor instanceof OrderedEventExecutor;
    }
    
##### 신규 connection을 맺을 경우 flow    

채널 초기화 파이프 라인 등록

    p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });

신규 connection을 맺을 경우 실행
ch.eventLoop() : child eventLoop

    ch.eventLoop().execute(new Runnable() {
        @Override
        public void run() {
            pipeline.addLast(new ServerBootstrapAcceptor(
                    ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
        }
    });
    
스레드 생성
    public abstract class SingleThreadEventExecutor extends AbstractScheduledEventExecutor implements OrderedEventExecutor {
    ...
    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        boolean inEventLoop = inEventLoop();
        addTask(task);
        if (!inEventLoop) {
            startThread();
            if (isShutdown() && removeTask(task)) {
                reject();
            }
        }

        if (!addTaskWakesUp && wakesUpForTask(task)) {
            wakeup(inEventLoop);
        }
    }
     
                
    "nioEventLoopGroup-3-1@3530" prio=10 tid=0x12 nid=NA runnable
    java.lang.Thread.State: RUNNABLE
	  at org.groovy.debug.hotswap.ResetAgent.matches(Unknown Source:-1)
	  at org.groovy.debug.hotswap.ResetAgent.containsSubArray(Unknown Source:-1)
	  at org.groovy.debug.hotswap.ResetAgent.removeTimestampField(Unknown Source:-1)
	  at org.groovy.debug.hotswap.ResetAgent.access$000(Unknown Source:-1)
	  at org.groovy.debug.hotswap.ResetAgent$1.transform(Unknown Source:-1)
	  at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	  at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:428)
	  at java.lang.ClassLoader.defineClass1(ClassLoader.java:-1)
	  at java.lang.ClassLoader.defineClass(ClassLoader.java:763)
	  at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
	  at java.net.URLClassLoader.defineClass(URLClassLoader.java:467)
	  at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
	  at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
	  at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
	  at java.security.AccessController.doPrivileged(AccessController.java:-1)
	  at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
	  at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	  - locked <0xdf4> (a java.lang.Object)
	  at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:349)
	  at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
	  at java.lang.ClassLoader.defineClass1(ClassLoader.java:-1)
	  at java.lang.ClassLoader.defineClass(ClassLoader.java:763)
	  at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
	  at java.net.URLClassLoader.defineClass(URLClassLoader.java:467)
	  at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
	  at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
	  at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
	  at java.security.AccessController.doPrivileged(AccessController.java:-1)
	  at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
	  at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	  - locked <0xdf5> (a java.lang.Object)
	  at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:349)
	  at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
	  at nettyniotcpserver.init.SocketChannelInitializer.initChannel(SocketChannelInitializer.java:25)
	  at nettyniotcpserver.init.SocketChannelInitializer.initChannel(SocketChannelInitializer.java:19)
	  at io.netty.channel.ChannelInitializer.initChannel(ChannelInitializer.java:115)
	  at io.netty.channel.ChannelInitializer.handlerAdded(ChannelInitializer.java:107)
	  at io.netty.channel.DefaultChannelPipeline.callHandlerAdded0(DefaultChannelPipeline.java:637)
	  at io.netty.channel.DefaultChannelPipeline.access$000(DefaultChannelPipeline.java:46)
	  at io.netty.channel.DefaultChannelPipeline$PendingHandlerAddedTask.execute(DefaultChannelPipeline.java:1487)
	  at io.netty.channel.DefaultChannelPipeline.callHandlerAddedForAllHandlers(DefaultChannelPipeline.java:1161)
	  at io.netty.channel.DefaultChannelPipeline.invokeHandlerAddedIfNeeded(DefaultChannelPipeline.java:686)
	  at io.netty.channel.AbstractChannel$AbstractUnsafe.register0(AbstractChannel.java:510)
	  at io.netty.channel.AbstractChannel$AbstractUnsafe.access$200(AbstractChannel.java:423)
	  at io.netty.channel.AbstractChannel$AbstractUnsafe$1.run(AbstractChannel.java:482)
	  at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:163)
	  at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:404)
	  at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:446)
	  at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
	  at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	  at java.lang.Thread.run(Thread.java:748)

"main@1" prio=5 tid=0x1 nid=NA waiting
  java.lang.Thread.State: WAITING
	  at java.lang.Object.wait(Object.java:-1)
	  at java.lang.Object.wait(Object.java:502)
	  at io.netty.util.concurrent.DefaultPromise.await(DefaultPromise.java:231)
	  at io.netty.channel.DefaultChannelPromise.await(DefaultChannelPromise.java:131)
	  at io.netty.channel.DefaultChannelPromise.await(DefaultChannelPromise.java:30)
	  at io.netty.util.concurrent.DefaultPromise.sync(DefaultPromise.java:337)
	  at io.netty.channel.DefaultChannelPromise.sync(DefaultChannelPromise.java:119)
	  at io.netty.channel.DefaultChannelPromise.sync(DefaultChannelPromise.java:30)
	  at nettyniotcpserver.server.NettyServer.start(NettyServer.java:48)
	  at nettyniotcpserver.server.NettyServer.init(NettyServer.java:27)
	  at sun.reflect.NativeMethodAccessorImpl.invoke0(NativeMethodAccessorImpl.java:-1)
	  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	  at java.lang.reflect.Method.invoke(Method.java:498)
	  at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleElement.invoke(InitDestroyAnnotationBeanPostProcessor.java:369)
	  at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleMetadata.invokeInitMethods(InitDestroyAnnotationBeanPostProcessor.java:312)
	  at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:135)
	  at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:423)
	  at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1702)
	  at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:583)
	  at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:502)
	  at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:312)
	  at org.springframework.beans.factory.support.AbstractBeanFactory$$Lambda$78.2059572982.getObject(Unknown Source:-1)
	  at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:228)
	  - locked <0xdf8> (a java.util.concurrent.ConcurrentHashMap)
	  at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:310)
	  at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
	  at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:760)
	  at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:868)
	  at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:549)
	  - locked <0xdf9> (a java.lang.Object)
	  at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752)
	  at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:388)
	  at org.springframework.boot.SpringApplication.run(SpringApplication.java:327)
	  at nettyniotcpserver.application.ServerApplication.main(ServerApplication.java:24)

"nioEventLoopGroup-2-1@3344" prio=10 tid=0x11 nid=NA runnable
  java.lang.Thread.State: RUNNABLE
	  at io.netty.bootstrap.ServerBootstrap$ServerBootstrapAcceptor.channelRead(ServerBootstrap.java:266)
	  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
	  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
	  at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
	  at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434)
	  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
	  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
	  at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965)
	  at io.netty.channel.nio.AbstractNioMessageChannel$NioMessageUnsafe.read(AbstractNioMessageChannel.java:93)
	  at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:628)
	  at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:563)
	  at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:480)
	  at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:442)
	  at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884)
	  at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	  at java.lang.Thread.run(Thread.java:748)

"Finalizer@3570" daemon prio=8 tid=0x3 nid=NA waiting
  java.lang.Thread.State: WAITING
	  at java.lang.Object.wait(Object.java:-1)
	  at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:143)
	  at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:164)
	  at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:212)

"Reference Handler@3571" daemon prio=10 tid=0x2 nid=NA waiting
  java.lang.Thread.State: WAITING
	  at java.lang.Object.wait(Object.java:-1)
	  at java.lang.Object.wait(Object.java:502)
	  at java.lang.ref.Reference.tryHandlePending(Reference.java:191)
	  at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:153)

"Attach Listener@3568" daemon prio=5 tid=0x5 nid=NA runnable
  java.lang.Thread.State: RUNNABLE

"Signal Dispatcher@3569" daemon prio=9 tid=0x4 nid=NA runnable
  java.lang.Thread.State: RUNNABLE



##### NioServerSocketChannel vs NioSocketChannel

NioServerSocketChannel : socket bind

NioSocketChannel : read & accept
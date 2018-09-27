
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

        
    
    
    
    
    


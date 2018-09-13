
##1. NioEventLoopGroup
   ###1) Boss
   
   ###2) Worker
   
   ###3) Diagram
   
##2. MultithreadEventExecutorGroup

   ###1) 스레드 객체 생성
   
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
       
       
       

## 3.Bind

  ## 1) 소스
   
    private ChannelFuture doBind(final SocketAddress localAddress) {
           final ChannelFuture regFuture = initAndRegister();
           final Channel channel = regFuture.channel();
           if (regFuture.cause() != null) {
               return regFuture;
           }
   
           if (regFuture.isDone()) {
               // At this point we know that the registration was complete and successful.
               ChannelPromise promise = channel.newPromise();
               doBind0(regFuture, channel, localAddress, promise);
               return promise;
           } else {
               // Registration future is almost always fulfilled already, but just in case it's not.
               final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
               regFuture.addListener(new ChannelFutureListener() {
                   @Override
                   public void operationComplete(ChannelFuture future) throws Exception {
                       Throwable cause = future.cause();
                       if (cause != null) {
                           // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                           // IllegalStateException once we try to access the EventLoop of the Channel.
                           promise.setFailure(cause);
                       } else {
                           // Registration was successful, so set the correct executor to use.
                           // See https://github.com/netty/netty/issues/2586
                           promise.registered();
   
                           doBind0(regFuture, channel, localAddress, promise);
                       }
                   }
               });
               return promise;
           }
       }
       
       
   
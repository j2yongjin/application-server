
## 참고글 
    https://netty.io/wiki/reference-counted-objects.html

## Reference counted objects

    Netty 버전 4부터는 특정 객체의 수명주기가 참조 카운트에 의해 관리되므로 Netty는 더 이상 사용하지 않는 
    즉시 객체 풀 (또는 객체 할당 자)에 객체 또는 공유 자원을 반환 할 수 있습니다. 
    가비지 수집 및 참조 대기열은 접근 불가능성에 대한 효율적인 실시간 보장을 제공하지 않지만 
    참조 계산은 약간의 불편 함을 희생하면서 대체 메커니즘을 제공합니다.
    
    ByteBuf는 참조 counting을 활용하여 할당 및 할당 해지 성능을 향상시키는 가장 주목할만한 유형이며,
    이 페이지에서는 ByteBuf를 사용하여 Netty의 참조 계산이 어떻게 작동하는지 설명합니다.
    
## Basics of reference counting

    새로운 참조 카운트 된 객체의 초기 참조 카운트는 1입니다
    
    ByteBuf buf = ctx.alloc().directBuffer();
    assert buf.refCnt() == 1;

    참조 카운트 된 객체를 해제하면 참조 카운트가 1 씩 감소합니다. 
    참조 카운트가 0에 도달하면 참조 카운트 된 객체가 할당 해제되거나 객체 풀로 반환됩니다


    assert buf.refCnt() == 1;
    // release() returns true only if the reference count becomes 0.
    boolean destroyed = buf.release();
    assert destroyed;
    assert buf.refCnt() == 0;
    
### Dangling reference (매달려있는 참조)

    참조 카운트가 0의 참조 카운트 된 객체에 액세스하려고하면 (자), 
    IllegalReferenceCountException가 발생합니다.
    
    assert buf.refCnt() == 0;
    try {
      buf.writeLong(0xdeadbeef);
      throw new Error("should not reach here");
    } catch (IllegalReferenceCountExeception e) {
      // Expected
    }

### Increasing the reference count

    레퍼런스 카운트는 아직 소멸되지 않는 한 retain () 연산을 통해 증가 될 수있다.
    
    ByteBuf buf = ctx.alloc().directBuffer();
    assert buf.refCnt() == 1;
    
    buf.retain();
    assert buf.refCnt() == 2;
    
    boolean destroyed = buf.release();
    assert !destroyed;
    assert buf.refCnt() == 1;
    
### Who destroys it

    일반적으로, 참조 카운트 된 객체에 마지막으로 액세스 한 당사자가 
    참조 카운트 된 객체의 파기를 담당하는 것이 일반적입니다. 
    더 구체적으로
    
    - [송신] 구성 요소가 참조 카운팅 된 객체를 다른 [수신] 구성 요소로 전달해야하는 경우 
        전송 구성 요소는 일반적으로이를 파기 할 필요는 없지만 수신 구성 요소로 그 결정을 연기합니다.
        
    - 구성 요소가 참조 카운트 된 객체를 소비하고 더 이상 액세스하지 않는 
        (즉, 다른 구성 요소에 대한 참조를 전달하지 않음) 구성 요소가 있으면 
        해당 구성 요소가이를 파기해야합니다
        
     public ByteBuf a(ByteBuf input) {
         input.writeByte(42);
         return input;
     }
     
     public ByteBuf b(ByteBuf input) {
         try {
             output = input.alloc().directBuffer(input.readableBytes() + 1);
             output.writeBytes(input);
             output.writeByte(42);
             return output;
         } finally {
             input.release();
         }
     }
     
     public void c(ByteBuf input) {
         System.out.println(input);
         input.release();
     }
     
     public void main() {
         ...
         ByteBuf buf = ...;
         // This will print buf to System.out and destroy it.
         c(b(a(buf)));
         assert buf.refCnt() == 0;
     }   
        
### Derived buffers (파생 버퍼)

    ByteBuf.duplicate (), ByteBuf.slice () 및 ByteBuf.order (ByteOrder)는 
    부모 버퍼의 메모리 영역을 공유하는 파생 버퍼를 만듭니다. 
    파생 된 버퍼는 고유 한 참조 횟수를 가지지 않지만 상위 버퍼의 참조 횟수를 공유합니다
    
    ByteBuf parent = ctx.alloc().directBuffer();
    ByteBuf derived = parent.duplicate();
    
    // Creating a derived buffer does not increase the reference count.
    assert parent.refCnt() == 1;
    assert derived.refCnt() == 1;
    
    반대로, ByteBuf.copy () 및 ByteBuf.readBytes (int)는 파생 버퍼가 아닙니다. 
    반환 된 ByteBuf가 할당되어야합니다.
    
    부모 버퍼와 파생 된 버퍼는 동일한 참조 횟수를 공유하므로 파생 된 버퍼를 만들 때 
    참조 횟수가 증가하지 않습니다. 따라서 파생 된 버퍼를 
    응용 프로그램의 다른 구성 요소에 전달하려면 retain ()을 먼저 호출해야합니다
        
    ByteBuf parent = ctx.alloc().directBuffer(512);
    parent.writeBytes(...);
    
    try {
        while (parent.isReadable(16)) {
            ByteBuf derived = parent.readSlice(16);
            derived.retain();
            process(derived);
        }
    } finally {
        parent.release();
    }
    ...
    
    public void process(ByteBuf buf) {
        ...
        buf.release();
    }
    
### ByteBufHolder interface

    때로는 ByteBuf가 DatagramPacket, HttpContent 및 WebSocketframe과 같은 버퍼 홀더에 포함됩니다. 
    이러한 유형은 ByteBufHolder라는 공통 인터페이스를 확장합니다
    
    버퍼 홀더는 파생 된 버퍼와 마찬가지로 포함 된 버퍼의 참조 카운트를 공유합니다.
    
    
## Reference-counting in ChannelHandler

### Inbound messages

    이벤트 루프가 ByteBuf로 데이터를 읽고 channelRead () 이벤트를 트리거하면 해당 파이프 라인의 
    ChannelHandler가 버퍼를 해제해야합니다. 따라서 수신 된 데이터를 
    사용하는 핸들러는 channelRead () 핸들러 메소드의 데이터에서 release ()를 호출해야합니다
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            ...
        } finally {
            buf.release();
        }
    }
    
    '누가 해지 하는가?'에서 설명한 것처럼 이 문서의 섹션에서 
    핸들러가 버퍼 (또는 참조 카운팅 된 객체)를 다음 핸들러로 전달하는 
    경우이를 해제 할 필요가 없습니다
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        ...
        ctx.fireChannelRead(buf);
    }
    
    ByteBuf는 Netty의 유일한 참조 카운팅 유형이 아닙니다. 
    디코더에 의해 생성 된 메시지를 다루는 경우 메시지가 참조 카운팅 된 것일 가능성이 높습니다
    
    // Assuming your handler is placed next to `HttpRequestDecoder`
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            ...
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            try {
                ...
            } finally {
                content.release();
            }
        }
    }
    
    의문의 여지가 있거나 메시지 공개를 단순화하려면 ReferenceCountUtil.release ()를 사용할 수 있습니다.
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            ...
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
    
    또는 수신 한 모든 메시지에 대해 ReferenceCountUtil.release (msg)를 
    호출하는 SimpleChannelHandler를 확장하는 것도 고려해 볼 수 있습니다.


### Outbound messages

    인바운드 메시지와 달리 아웃 바운드 메시지는 응용 프로그램에서 만들어지며 
    유선 메시지를 작성한 후에 릴리스하는 것은 Netty의 책임입니다. 
    그러나 쓰기 요청을 인터셉트하는 핸들러는 중간 개체를 올바르게 릴리스해야합니다. (예 : 인코더)
    
    // Simple-pass through
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        System.err.println("Writing: " + message);
        ctx.write(message, promise);
    }
    
    // Transformation
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        if (message instanceof HttpContent) {
            // Transform HttpContent to ByteBuf.
            HttpContent content = (HttpContent) message;
            try {
                ByteBuf transformed = ctx.alloc().buffer();
                ....
                ctx.write(transformed, promise);
            } finally {
                content.release();
            }
        } else {
            // Pass non-HttpContent through.
            ctx.write(message, promise);
        }
    }

### Troubleshooting buffer leaks

    참조 카운팅의 단점은 참조 카운팅 된 객체를 누수하기 쉽다는 것입니다. 
    JVM은 Netty가 구현 한 참조 카운팅을 인식하지 못하기 때문에 
    참조 카운트가 0이 아니더라도 도달 할 수 없으면 자동으로 GC를 실행합니다. 
    일단 가비지 수집 된 객체는 부활 될 수 없으므로, 
    객체가 풀에서 반환 될 수 없기 때문에 메모리 누수가 발생합니다
    
    다행스럽게도 누수를 찾기가 어렵지만 Netty는 기본적으로 응용 프로그램에 누수가 있는지 
    확인하기 위해 약 1 %의 버퍼 할당을 샘플링합니다. 유출 된 경우 다음과 같은 로그 메시지가 나타납니다.
    
    
    LEAK: ByteBuf.release() was not called before it's garbage-collected. 
    Enable advanced leak reporting to find out where the leak occurred. 
    To enable advanced leak reporting
    , specify the JVM option '-Dio.netty.leakDetectionLevel=advanced' or call ResourceLeakDetector.setLevel()
    
    위에서 언급 한 JVM 옵션을 사용하여 응용 프로그램을 다시 시작하면 누출 된 버퍼에 액세스 한 응용 프로그램의 최근 위치가 표시됩니다. 
    다음 출력은 유닛 테스트 (XmlFrameDecoderTest.testDecodeWithXml ())의 누수를 보여줍니다
    
    Running io.netty.handler.codec.xml.XmlFrameDecoderTest
    15:03:36.886 [main] ERROR io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected.
    Recent access records: 1
    #1:
    	io.netty.buffer.AdvancedLeakAwareByteBuf.toString(AdvancedLeakAwareByteBuf.java:697)
    	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithXml(XmlFrameDecoderTest.java:157)
    	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithTwoMessages(XmlFrameDecoderTest.java:133)
    	...
    
    Created at:
    	io.netty.buffer.UnpooledByteBufAllocator.newDirectBuffer(UnpooledByteBufAllocator.java:55)
    	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:155)
    	io.netty.buffer.UnpooledUnsafeDirectByteBuf.copy(UnpooledUnsafeDirectByteBuf.java:465)
    	io.netty.buffer.WrappedByteBuf.copy(WrappedByteBuf.java:697)
    	io.netty.buffer.AdvancedLeakAwareByteBuf.copy(AdvancedLeakAwareByteBuf.java:656)
    	io.netty.handler.codec.xml.XmlFrameDecoder.extractFrame(XmlFrameDecoder.java:198)
    	io.netty.handler.codec.xml.XmlFrameDecoder.decode(XmlFrameDecoder.java:174)
    	io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:227)
    	io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:140)
    	io.netty.channel.ChannelHandlerInvokerUtil.invokeChannelReadNow(ChannelHandlerInvokerUtil.java:74)
    	io.netty.channel.embedded.EmbeddedEventLoop.invokeChannelRead(EmbeddedEventLoop.java:142)
    	io.netty.channel.DefaultChannelHandlerContext.fireChannelRead(DefaultChannelHandlerContext.java:317)
    	io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:846)
    	io.netty.channel.embedded.EmbeddedChannel.writeInbound(EmbeddedChannel.java:176)
    	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithXml(XmlFrameDecoderTest.java:147)
    	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithTwoMessages(XmlFrameDecoderTest.java:133)
    	...





    Netty 5 이상을 사용하면 누수 된 버퍼를 마지막으로 처리 한 핸들러를 찾는 데 도움이되는 
    추가 정보가 제공됩니다. 다음 예제에서는 이름이 EchoServerHandler # 0이고 가비지 수집 된 핸들러에서 누수 된 버퍼를 처리 한 것을 보여줍니다. 즉, EchoServerHandler # 0이 버퍼를 놓는 것을 잊어 버린 것 같습니다
    
    12:05:24.374 [nioEventLoop-1-1] ERROR io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected.
    Recent access records: 2
    #2:
    	Hint: 'EchoServerHandler#0' will handle the message from this point.
    	io.netty.channel.DefaultChannelHandlerContext.fireChannelRead(DefaultChannelHandlerContext.java:329)
    	io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:846)
    	io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:133)
    	io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:485)
    	io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:452)
    	io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:346)
    	io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:794)
    	java.lang.Thread.run(Thread.java:744)
    #1:
    	io.netty.buffer.AdvancedLeakAwareByteBuf.writeBytes(AdvancedLeakAwareByteBuf.java:589)
    	io.netty.channel.socket.nio.NioSocketChannel.doReadBytes(NioSocketChannel.java:208)
    	io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:125)
    	io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:485)
    	io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:452)
    	io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:346)
    	io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:794)
    	java.lang.Thread.run(Thread.java:744)
    Created at:
    	io.netty.buffer.UnpooledByteBufAllocator.newDirectBuffer(UnpooledByteBufAllocator.java:55)
    	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:155)
    	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:146)
    	io.netty.buffer.AbstractByteBufAllocator.ioBuffer(AbstractByteBufAllocator.java:107)
    	io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:123)
    	io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:485)
    	io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:452)
    	io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:346)
    	io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:794)
    	java.lang.Thread.run(Thread.java:744)

### Leak detection levels

    There are currently 4 levels of leak detection
    
    DISABLED - disables leak detection completely. Not recommended.
    • SIMPLE - tells if there is a leak or not for 1% of buffers. Default.
    • ADVANCED - tells where the leaked buffer was accessed for 1% of buffers.
    • PARANOID - Same with ADVANCED except that it's for every single buffer. Useful for automated testing phase. You could fail the build if the build output contains 'LEAK: '.
    
    
    You can specify the leak detection level as a JVM option -Dio.netty.leakDetection.level
    
    java -Dio.netty.leakDetection.level=advanced 
    
    This property used to be called io.netty.leakDetectionLevel
    
### Best practices to avoid leaks

    - PARANOID 누출 감지 레벨 및 SIMPLE 레벨에서 단위 테스트 및 통합 테스트를 실행하십시오.
    - Canary가 누출 여부를 알기 위해 합리적으로 오랜 시간 동안 SIMPLE 레벨에서 
        전체 클러스터로 롤아웃하기 전에 응용 프로그램을 사용하십시오.
    - 누출이있는 경우, ADVANCED 레벨에서 다시 canary를 사용하여 누설이 어디에서 왔는지에 
        대한 힌트를 얻으십시오.
    - 누수가있는 응용 프로그램을 전체 클러스터에 배포하지 마십시오.
    
### Fixing leaks in unit tests

    단위 테스트에서 버퍼 또는 메시지를 해제하는 것을 잊어 버리는 것은 매우 쉽습니다. 
    누수 경고가 발생하지만 반드시 응용 프로그램에 누출이 있음을 의미하지는 않습니다. 
    try-finally 블록으로 단위 테스트를 래핑하는 대신 
    모든 버퍼를 해제하는 대신 ReferenceCountUtil.releaseLater () 유틸리티 메소드를 사용할 수 있습니다
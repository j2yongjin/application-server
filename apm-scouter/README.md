> 기본 설치 메뉴얼

     [설치메뉴얼](https://github.com/scouter-project/scouter/blob/master/scouter.document/main/Quick-Start.md)

> 자바 데몬 모니터링
    - 클라이언트 설정 방법
       파일명 : scouter.conf
    
       server IP Address (Default : 127.0.0.1)
       net_collector_ip=123.2.134.246
       
       # Scouter Server Port (Default : 6100)
       net_collector_udp_port=6100
       net_collector_tcp_port=6100
       
       # Scouter Name(Default : tomcat1)
       obj_name=faxreceive
       #trace_interservice_enabled=true
       
       #hook_service_patterns=com.daou.penguin.handler.InputServiceHandler.channelRead
       
       
       _trace_auto_service_enabled=true
       _trace_auto_service_backstack_enabled=true
       
       hook_method_patterns=com.daou.penguin.handler.*.*,com.daou.penguin.codec.*.*,com.daou.penguin.init.*.*
       hook_method_access_public_enabled=true
       hook_method_access_private_enabled=false
       hook_method_access_protected_enabled=false
       hook_method_access_none_enabled=false
       hook_method_ignore_prefixes=get,set
       
       
> 애플리케이션 실행 옵션 
  

       
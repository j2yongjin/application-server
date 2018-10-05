
## 로그 모니터링 아키텍처

    1)서비스(애플리케이션: apache , tomcat , daemon) 로그 출력 
        --> 2)로그 수집 ( fluentd ) 
            --> 3)로그 파싱 (fluentd parser) 
                --> 4)로그 저장 ( influexDb) 
                    --> 5) 실시간 모니터링 그래프( grafana)
                         --> 6) 알림 메시지 전송
    
### 사용법

#### 서비스(애플리케이션) 로그 출력 
> 로그 표준화

> 로그 설정(logback)
    
#### fluentd
> 참고 URL : https://www.fluentd.org/

> 설치 : https://docs.fluentd.org/v1.0/categories/installation
   For Ubuntu Xenial : curl -L https://toolbelt.treasuredata.com/sh/install-ubuntu-xenial-td-agent3.sh | sh

  >> 설치 폴터 
     /etc/td-agent
           --> ad-agent
           --> plugin         : parser , filter plugin 파일 저장 
           --> sample.log     : console 로그 출력 
           --> sample.pos     : 임시 저장 파일 
     
     
> 실행 및 종료
 >> 서비스 실행 및 종료
 
    sudo /etc/init.d/td-agent start

    sudo /etc/init.d/td-agent stop
 
    sudo /etc/init.d/td-agent restart
 
    sudo /etc/init.d/td-agent status
    

> 로그 출력

    /var/log/td-agent/td-agent.log
     
> 테스트

>> 로그 write 테스트
   echo "2014-04-01T00:00:00 name=jake age=200 action=debugging1111" >> sample.log
   

#### fluentd plugin 개발

> rabbitmq plugin

https://github.com/nttcom/fluent-plugin-rabbitmq


> ndibs_log parser sample

    require 'fluent/plugin/parser'
    
    module Fluent::Plugin
      class TimeKeyValueParser < Parser
        # Register this parser as "time_key_value"
        Fluent::Plugin.register_parser("time_key_value", self)
    
        config_param :delimiter, :string, default: " "   # delimiter is configurable with " " as default
        config_param :time_format, :string, default: nil # time_format is configurable
    
        def configure(conf)
          super
    
          if @delimiter.length != 1
            raise ConfigError, "delimiter must be a single character. #{@delimiter} is not."
          end
    
          # TimeParser class is already given. It takes a single argument as the time format
          # to parse the time string with.
          @time_parser = Fluent::TimeParser.new(@time_format)
        end
    
        def parse(text)
          time, key_values = text.split(" ", 2)
          time = @time_parser.parse(time)
          record = {}
          key_values.split(@delimiter).each { |kv|
            k, v = kv.split("=", 2)
            record[k] = v
          }
          yield time, record
        end
      end
    end

  



#### influexDb
> 설치

wget https://dl.influxdata.com/influxdb/releases/influxdb_1.3.5_amd64.deb
sudo dpkg -i influxdb_1.3.5_amd64.deb


> query sample

  SELECT mean("bees") FROM "farm" GROUP BY time(30m) HAVING mean("bees") > 20
  
  SELECT mean(count("bees")) FROM "farm" GROUP BY time(30m)
  
  // 
  SELECT mean("count_bees") FROM "aggregate_bees" WHERE time >= <start_time> AND time <= <end_time>
  
  // measurement(table) join
  SELECT <field_key>[,<field_key>,<tag_key>] FROM <measurement_name>[,<measurement_name>]
   
   https://docs.influxdata.com/influxdb/v1.6/query_language/continuous_queries/#basic-syntax
   
   

> 어드민
  admin/amin
  

#### grafana
> 설치
  
   https://grafana.com/grafana/download
   
   wget https://s3-us-west-2.amazonaws.com/grafana-releases/release/grafana_5.2.4_amd64.deb 
   sudo dpkg -i grafana_5.2.4_amd64.deb 
 
> 실행

   sudo service grafana-server start
   
   sudo service grafana-server restart
   
> 웹페이지

  https://localhost:3000
  
  id/pw   : admin/admin
   
 
   
   

  


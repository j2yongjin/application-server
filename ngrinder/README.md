> 설치
 Docker 를 이용한 서버 / 클라이언트 설치

1. 어드민
  docker pull ngrinder/controller
  sudo docker run -d -v  ~/.ngrinder:/root/.ngrinder -p 8080:80 -p 16001:16001 -p 12000-12009:12000-12009 ngrinder/controller:latest

2. agent
  sudo docker pull ngrinder/agent
  sudo docker run -d -e CONTROLLER_ADDR=10.0.2.15:8080 ngrinder/agent
  
> 사용법
  https://github.com/naver/ngrinder/wiki/User-Guide
  
  https://github.com/naver/ngrinder/wiki/User-guide-in-Korean
  
  
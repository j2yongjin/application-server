> 설치
 Docker 를 이용한 서버 / 클라이언트 설치

1. 어드민

  docker pull ngrinder/controller
  docker run -d -v ~/ngrinder-controller:/opt/ngrinder-controller -p 8080:80 -p 16001:16001 -p 12000-12009:12000-12009 ngrinder/controller:latest
  
  -v : 공유 디렉토리 옵션(volume)
  host directory : ~/ngrinder-controller
  container directory : /opt/ngrinder-controller
  

2. agent

  sudo docker pull ngrinder/agent
  docker run -v ~/ngrinder-agent:/opt/ngrinder-agent -d ngrinder/agent 10.0.2.15:8080
  
> 사용법

  https://github.com/naver/ngrinder/wiki/User-Guide
  https://github.com/naver/ngrinder/wiki/User-guide-in-Korean
  
  
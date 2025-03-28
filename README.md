﻿# backend

<a href = "https://github.com/KGUCapstone/backend" > backend </a> <br/>
<a href = "https://github.com/KGUCapstone/frontend" > frontend </a>

# 2025_KGU_Basic_Capstone_Design

application-dev.yml, application-secret.yml 추가 <br>
(backend/src/main/resources/application-dev.yml ) <br>
(backend/src/main/resources/application-secret.yml )

```yml
spring:
  datasource:
    url : jdbc:mysql://localhost:3306/데이터베이스_이름?serverTimezone=UTC&characterEncoding=UTF-8
    username : root
    password : 비밀번호
    driver-class-name: com.mysql.cj.jdbc.Driver


  backend:
    url: http://localhost:8080
  frontend:
    url: http://localhost:3000
```
- **데이터베이스_이름**, **비밀번호** 환경에 맞게 수정.

```yaml
spring:
  jwt:
    secret: ${JWT_SECRET}

  security:
    oauth2:
      client:
        registration:
          naver:
            client-name: naver
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: ${BACKEND_URL}/login/oauth2/code/naver
            authorization-grant-type: authorization_code
            scope: name,email

      provider:
          naver:
              authorization-uri: https://nid.naver.com/oauth2.0/authorize
              token-uri: https://nid.naver.com/oauth2.0/token
              user-info-uri: https://openapi.naver.com/v1/nid/me
              user-name-attribute: response
```
- **NAVER_CLIENT_ID**, **NAVER_CLIENT_SECRET**,  **BACKEND_URL**  환경변수 설정
<br> 환경 변수 값 디스코드에서 확인


***

> git 컨벤션 

| 태그 이름   |사용 설명|
|---------|---|
| Feat:   |새로운 기능을 추가할 경우|
| Fix:    |버그를 고친 경우|
| Design: |	CSS 등 사용자 UI 디자인 변경|
| Docs:   |문서를 수정한 경우|
| Test:   |테스트 추가, 테스트 리팩토링 (프로덕션 코드 변경 X)|
| Refactor:|	코드 리팩토링|
| Chore:  |소스 코드를 건들지 않는 작업(빌드 업무 수정)|
| Style:  |코드 포맷 변경, 세미 콜론 누락, 코드 수정이 없는 경우|
| Remove: |파일이나 코드, 리소스 제거|
| Rename: |파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우|
| Hotfix: |긴급 수정이 필요한 경우|
| Etc:    |기타 사항|


좋은 예
git commit -m "Feat: 사용자 프로필 편집 기능 추가"

나쁜 예
git commit -m "기능 추가" # 유형 명시 누락

***


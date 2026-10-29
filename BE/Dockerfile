# 백엔드 이미지 (멀티 스테이지)
# 1단계: JDK 17로 bootJar 빌드  2단계: JRE만 담은 가벼운 실행 이미지
# 빌드: BE 폴더를 컨텍스트로 `docker build -t nyam-backend BE`

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 먼저 받아 두기 (build.gradle이 안 바뀌면 이 단계는 캐시 재사용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src
# 테스트는 DB가 필요한 @SpringBootTest라 이미지 빌드에서는 건너뜀 (CI 테스트 단계는 별도로 추가 예정)
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
# 업로드 이미지 저장 폴더(app.upload.dir). 서버는 도커 볼륨을 여기에 붙이며, 첫 생성 시 이 소유권을 물려받는다
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads
COPY --from=build /workspace/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:17 AS builder
COPY . /source
WORKDIR /source
RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:17
COPY --from=builder /source/build/libs/gitops-cloudflare-email-all.jar /gitops-cloudflare-email.jar
WORKDIR /config
ENTRYPOINT ["java", "-jar", "/gitops-cloudflare-email.jar"]

FROM eclipse-temurin:21-alpine

RUN apk add libjxl-tools

WORKDIR /workspace

COPY target/mango2j-*.jar /workspace/mango2j.jar

CMD ["java", "-jar", "mango2j.jar", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-Djava.library.path=/usr/lib"]
FROM eclipse-temurin:21-jre

RUN apt-get update \
	&& apt-get install -y --no-install-recommends libjxl-tools \
	&& rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY target/mango2j-*.jar /workspace/mango2j.jar

CMD ["java", "-Xmx4G", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-Djava.library.path=/usr/lib", "-jar", "mango2j.jar"]
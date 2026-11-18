# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY . .

ARG MODULE

RUN --mount=type=cache,target=/root/.m2 \
    test -n "${MODULE}" \
    && mvn -B -ntp -pl "${MODULE}" -am clean package -DskipTests \
    && JAR_FILE="$(find "${MODULE}/target" -maxdepth 1 -type f -name '*.jar' \
         ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)" \
    && test -n "${JAR_FILE}" \
    && cp "${JAR_FILE}" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy

ARG INSTALL_PYTHON=false

RUN if [ "${INSTALL_PYTHON}" = "true" ]; then \
      apt-get update \
      && apt-get install -y --no-install-recommends python3 python3-pip \
      && python3 -m pip install --no-cache-dir python-pptx requests \
      && rm -rf /var/lib/apt/lists/*; \
    fi

RUN groupadd --system spring \
    && useradd --system --gid spring --create-home --home-dir /home/spring spring

WORKDIR /app

COPY --from=builder --chown=spring:spring /workspace/app.jar /app/app.jar

RUN mkdir -p /app/logs /app/data/rocketmq-offsets /home/spring/nacos \
    && chown -R spring:spring /app /home/spring

ENV TZ=Asia/Shanghai

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

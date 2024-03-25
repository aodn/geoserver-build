FROM amazoncorretto:11-al2-jdk

ARG BUILDER_UID=9999
ARG DEBIAN_FRONTEND=noninteractive
ARG MAVEN_VERSION=3.9.6
ARG USER_HOME_DIR="/root"
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

ENV TZ="Australia"
ENV HOME /home/builder
ENV JAVA_TOOL_OPTIONS -Duser.home=/home/builder
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN yum install --quiet --assumeyes \
    git \
    libxml2-utils \
    python3 \
    python3-pip \
    unzip \
    wget \
    tar

RUN pip3 install \
    bump2version==1.0.1

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
 && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
 && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
 && rm -f /tmp/apache-maven.tar.gz \
 && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

RUN useradd --create-home --no-log-init --shell /bin/bash --uid $BUILDER_UID builder
RUN chown builder /home/builder
USER builder
WORKDIR /home/builder

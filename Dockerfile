FROM amazoncorretto:11-al2-jdk

ARG BUILDER_UID=9999
ARG DEBIAN_FRONTEND=noninteractive

ENV TZ="Australia"
ENV HOME /home/builder
ENV JAVA_TOOL_OPTIONS -Duser.home=/home/builder

RUN yum install --quiet --assumeyes \
    git \
    libxml2-utils \
    maven \
    python3 \
    python3-pip \
    unzip \
    wget

RUN pip3 install \
    bump2version==1.0.1

RUN useradd --create-home --no-log-init --shell /bin/bash --uid $BUILDER_UID builder
USER builder
WORKDIR /home/builder

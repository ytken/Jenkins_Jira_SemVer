FROM    jenkins/jenkins:2.382

ARG     UID=1000
ARG     GID=1000
ARG     JENKINS_HOME_PATH=/home/jenkins

USER    root
RUN     groupmod -g ${GID} jenkins \
        && usermod -u ${UID} -g ${GID} -d ${JENKINS_HOME_PATH} jenkins \ 
        && chown -R ${UID} /usr/share/jenkins/ref

USER    ${UID}
ENV     JAVA_OPTS -Djenkins.install.runSetupWizard=false

ENV     JENKINS_HOME=${JENKINS_HOME_PATH}/data
ENV     COPY_REFERENCE_FILE_LOG=${JENKINS_HOME_PATH}/copy_reference_file.log

COPY    --chown=${UID}:${GID} plugins.txt ${JENKINS_HOME}/plugins.txt

RUN     jenkins-plugin-cli \
            --war /usr/share/jenkins/jenkins.war \
            --plugin-file ${JENKINS_HOME_PATH}/data/plugins.txt

USER root

RUN    apt-get update \
       && apt-get install openssl \
       && apt-get install ca-certificates

RUN \
  apt-get update && \
  apt-get install -y python3 python3-dev python3-pip python3-venv 

RUN pip install pytest
RUN pip install pyopenssl
RUN pip install jira

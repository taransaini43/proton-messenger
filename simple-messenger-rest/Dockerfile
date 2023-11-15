# Stage 1: Build stage
FROM adoptopenjdk:11-jdk-hotspot AS builder

# Set environment variables for Scala and SBT versions
ENV SCALA_VERSION 2.13.12
ENV SBT_VERSION 1.8.2

# Set the working directory in the container
WORKDIR /home/app

# Increase memory allocated to the JVM during the build process
ENV SBT_OPTS="-Xmx2G"

# Install GPG, Scala, and SBT
RUN apt-get update && \
    apt-get install -y gnupg curl && \
    curl -Lo scala-$SCALA_VERSION.tgz https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz && \
    tar -xzvf scala-$SCALA_VERSION.tgz && \
    rm scala-$SCALA_VERSION.tgz && \
    mv scala-$SCALA_VERSION /usr/share/scala && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
    apt-get update && \
    apt-get install -y sbt && \
    sbt sbtVersion

# Install SQLite
RUN apt-get update && \
    apt-get install -y sqlite3

# Create the directory on the container
RUN mkdir -p /home/app/sqlite-dir/
RUN touch /home/app/sqlite-dir/localdatabase.db

# Copy project files
COPY ./project/build.properties /home/app/project/build.properties
COPY ./project/plugins.sbt /home/app/project/plugins.sbt

COPY ./src /home/app/src
COPY ./build.sbt /home/app/build.sbt

# Enable parallel compilation
RUN echo "parallelExecution in Compile := true" >> /home/app/build.sbt

# Run sbt to compile the application
RUN sbt assembly

# Stage 2: Runtime stage
FROM adoptopenjdk:11-jre-hotspot

# Set the working directory in the container
WORKDIR /home/app

# Copy only the necessary artifacts from the builder stage
COPY --from=builder /home/app/target /home/app/target

# Create an entry script that reads the environment variable and then runs the application
COPY entry-script.sh /home/app/entry-script.sh
RUN chmod +x /home/app/entry-script.sh

# Expose the ports your application needs
EXPOSE 8000 8080

# Use the entry script as the CMD
CMD ["/home/app/entry-script.sh"]

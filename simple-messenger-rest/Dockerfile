FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1
WORKDIR /home/app

COPY ./project/build.properties /home/app/project/build.properties
COPY ./src /home/app/src

COPY ./build.sbt /home/app/build.sbt

RUN sbt compile

EXPOSE 8000 8080
CMD sbt run
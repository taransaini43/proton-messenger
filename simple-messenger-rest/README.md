Steps to build and deploy the projects.

1. Have sqlite installed locally and update the conf in resources application.conf (can be driven externally via ENTRYPOINT)
2. go to parent project directory where Dockerfile is present.
3. run `docker build .`
You will see the following command trail getting executed : 
(please note that importing sbt dependencies may take good deal of time the very first time

JIVOXLAPMAC0105:simple-messenger-rest taranjitsaini$ docker build .
[+] Building 222.6s (9/10)                                                                                                                                                  
[+] Building 222.8s (9/10)                                                                                                                                                  
[+] Building 222.9s (9/10)                                                                                                                                                  
[+] Building 353.1s (9/10)                                                                                                                                                  
[+] Building 353.4s (9/10)                                                                                                                                                  
[+] Building 353.6s (9/10)                                                                                                                                                  
[+] Building 381.1s (9/10)                                                                                                                                                  
=> => transferring context: 18.95kB                                                                                                                                   0.0s
=> CACHED [2/6] WORKDIR /home/app                                                                                                                                     0.0s
=> CACHED [3/6] COPY ./project/build.properties /home/app/project/build.properties                                                                                    0.0s
=> [4/6] COPY ./src /home/app/src                                                                                                                                     0.3s
=> [5/6] COPY ./build.sbt /home/app/build.sbt                                                                                                                         0.1s
=> [6/6] RUN sbt compile   
=> exporting to image                                                                                                                                                 2.3s
=> => exporting layers                                                                                                                                                2.2s
=> => writing image sha256:01eb6b620911577d34f38f4bcb6332632027f6086c828da2b120c387028c3bdf 

4. check the created image id by listing the built images `docker images`
5. install sqlite in your local system
6. docker run -it <imageId> (image is failing at runtime because the base image is built off Java8 so won't work)
7. Alternate to 6. if you have Java 11 and sbt(1.8.2) already installed in your system (with sqlite), 
just execute `sbt run` in the directory where build.sbt is located.
8. Server will be up; feel free to try out the API endpoints. 
   [info]   Compilation completed in 9.643s.
   [info] running docs.http.scaladsl.ApplicationServer
   Server now online. Please navigate to http://localhost:8080/pingtest
   Press RETURN to stop...
9. NOTE : Since we are using service cache for convenience in the exercise, 
unread messages which are not accessed via /get/unread will get lost with restart 
(accessed ones are persisted in history in sqlite db )

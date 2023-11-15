Steps to build and deploy the projects.

1. Go to project directory containing docker-compose.yml
2. run docker-compose up --build -d
3. docker-compose up
Attaching to simple-messenger-rest_my-scala-app_1
   my-scala-app_1  | /home/app/sqlite-dir/
   my-scala-app_1  | /home/app/sqlite-dir/localdatabase.db
   my-scala-app_1  | Server now online. Please navigate to http://localhost:8080/pingtest
   my-scala-app_1  | Press RETURN to stop...
4. Try all end points
5.To use external sqlite DB, you can update SQLITE_DB_PATH env variable in docker-compose





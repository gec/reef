Building
=============================

We're using maven 3.0.x:

http://maven.apache.org/

Get Java6, we've run on both Sun and OpenJDK. 

Protoc is required to generate code. Install protoc 2.3.0 and make sure protoc 
is on the path. Maven will invoke the protobuf compiler.

http://code.google.com/p/protobuf/

Installing dependencies
==============================

Refer to INSTALL.markdown for instructions on installing the broker and database.

IntelliJIDEA 10
==============================

IntelliJ is recommened because of its solid scala support and great maven integration.

http://www.jetbrains.com/idea/

To import the maven project:
- File > New Project
- Import project from external model
- Select maven, click next
- Select the directory containing your base pom, check "import maven project automatically", click next


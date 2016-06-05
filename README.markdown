#Market

Forked from the (spray-can template)[https://github.com/spray/spray-template], specifically the `on_spray-can_1.3_scala-2.11` branch.

## Major Modifications

* Updated to Akka 2.4.6 and Scala 2.11.8
* Added `reference.conf` per http://doc.akka.io/docs/akka/2.4.7/scala/persistence.html#Local_LevelDB_journal
* Modified `build.sbt` with fork workaround per http://stackoverflow.com/questions/19425613/unsatisfiedlinkerror-with-native-library-under-sbt (This is a known sbt issue, having to do with native library exportation and the jvm classpath. See https://github.com/eligosource/eventsourced/wiki/Installation#native)


#To get started

1. Git-clone this repository.

        $ git clone git://github.com/bmorris458/spray-template.git my-project

2. Change directory into your clone:

        $ cd my-project

3. Launch SBT:

        $ sbt

4. Compile everything and run all tests:

        > test

5. Start the application:

        > re-start

6. Browse to [http://localhost:8080](http://localhost:8080/)

7. Stop the application:

        > re-stop

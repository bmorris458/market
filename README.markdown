#Market

Forked from the (spray-can template)[https://github.com/spray/spray-template], specifically the `on_spray-can_1.3_scala-2.11` branch.

##Design Considerations/Assumptions

* Preliminarily, each user will only have a name, an ID number, and a set of tags that describe subscriptions. Each item for sale will only have a title and a set of tags describing possible subscription criteria (including author, artist, etc.)
* A User may be an ActiveUser or an InactiveUser, likewise for an Item. This will be the sole means of discerning user or item status.
* User authentication and security will be handled outside of this module. Any function that modifies a user entry in this library (and associated databases) will not check for user permissions, so security should be implemented prior to calling.

##Major Modifications

* Updated to Akka 2.4.6 and Scala 2.11.8
* Added `reference.conf` per (the Akka Persistence docs)[http://doc.akka.io/docs/akka/2.4.7/scala/persistence.html#Local_LevelDB_journal]
* Added a hierarchical controller using the template from Vernon, "Reactive Messaging Patterns with the Actor Model" ch. 2, to avoid dead letters (most actor patterns were learned from Vernon).
* Modified `build.sbt` with fork workaround per (this post)[http://stackoverflow.com/questions/19425613/unsatisfiedlinkerror-with-native-library-under-sbt] (This is a known sbt issue, having to do with native library exportation and the jvm classpath. See (the eventsourced documentation)[https://github.com/eligosource/eventsourced/wiki/Installation#native])
* Added a Reaper pattern for clean shutdown of web server and unlocking of database per (shutdown patterns in akka)[http://letitcrash.com/post/30165507578/shutdown-patterns-in-akka-2]
* Utilized the Spray.io routing techniques described in (the spray-routing documentation)[http://spray.io/documentation/1.2.3/spray-routing/#spray-routing]

##To get started

1. Git-clone this repository.

        $ git clone git://github.com/bmorris458/spray-template.git my-project

2. Change directory into your clone:

        $ cd my-project

3. Launch application:

        $ sbt run

6. Browse to [http://localhost:8080](http://localhost:8080/)

7. Stop the application:

        > re-stop

##Development direction

* Add persistent query actor per http://doc.akka.io/docs/akka/2.4.6/scala/persistence-query.html#persistence-query-scala
* Add publish-subscribe channel per Vernon, p. 154
* Add applicative validation with scalaz per https://github.com/ironfish/akka-persistence-mongo-samples

#Market

Forked from the [spray-can template](https://github.com/spray/spray-template), specifically the `on_spray-can_1.3_scala-2.11` branch.

##Model

Underneath the System, the top-level actors are the Router (a.k.a. "Guardian"), the CommandProcessor ("Sarge"), and the Reaper ("Otto"). Underneath Sarge is the QueryProcessor ("MrEcho").

Otto watches any actor that sends a WatchMe request. Currently, Otto only watches Sarge.

Guardian routes incoming HttpRequests, directing PUT requests (commands) to Sarge, and GET requests (queries) to MrEcho. Guardian only waits for a response after sending a query.

Sarge creates MrEko, then introduces it to Guardian so Guardian can send queries. Upon receiving a command, Sarge performs any validation implemented, and then transforms successful commands into events, which it sends to MrEcho. Sarge also creates Gutenburg, which publishes notifications to the standard event stream.

MrEcho maintains the current state of records (including creating the actual User and Item objects, and the subscription watcher actors associated with each User). Upon receiving a query, it responds to the sender with the query result success or an error message failure. Upon receiving an event, it applies the event to the current state. The subscriber for each user watches the event stream and keeps track of notifications that are relevant to the User's tags.

For example purposes, Commands and Queries can be generated in a web browser by navigating to [the index](localhost:8080) and using the provided links.

##Design Considerations/Assumptions

* Preliminarily, each user will only have a name, an ID number, and a set of tags that describe subscriptions. Each item for sale will only have a title and a set of tags describing possible subscription criteria (including author, artist, etc.)
* Notifications will only be generated on tags, not titles or any other item fields, so to allow a user to subscribe to a title, each copy of that title would have to also be tagged with the title string (this can be viewed as a feature, making it simpler to subscribe to all variations of a series and sub-series, or to subscribe only to a sub-series, and any extra overhead could be mitigated by making a higher-level routine that batches the process of tagging each item with its own title).
* User authentication and security will be handled outside of this module. Any function that modifies a user entry in this library (and associated databases) will not check for user permissions, so security should be implemented prior to calling.

##Major Modifications

* Updated to Akka 2.4.6 and Scala 2.11.8
* Added `reference.conf` per [the Akka Persistence docs](http://doc.akka.io/docs/akka/2.4.7/scala/persistence.html#Local_LevelDB_journal)
* Modified `build.sbt` with fork workaround per [this post](http://stackoverflow.com/questions/19425613/unsatisfiedlinkerror-with-native-library-under-sbt) (This is a known sbt issue, having to do with native library exportation and the jvm classpath. See [the eventsourced documentation](https://github.com/eligosource/eventsourced/wiki/Installation#native))
* Added a Reaper pattern for clean shutdown of web server and unlocking of database per [shutdown patterns in akka](http://letitcrash.com/post/30165507578/shutdown-patterns-in-akka-2)
* Utilized the Spray.io routing techniques described in [the spray-routing documentation](http://spray.io/documentation/1.2.3/spray-routing/#spray-routing)
* Queries handled with ask pattern per [Alvin Alexander's blog](http://alvinalexander.com/scala/scala-akka-actors-ask-examples-future-await-timeout-result)

##To get started

1. Git-clone this repository.

        $ git clone git://github.com/bmorris458/market.git my-project

2. Change directory into your clone:

        $ cd my-project

3. Some very basic tests can be run preliminarily:

        $ sbt test

4. Launch application:

        $ sbt run

5. Once the message `[info] [INFO] ... Bound to localhost/127.0.0.1:8080` displays in the console, browse to [the index](http://localhost:8080/) and follow directions to test the system and demonstrate its behavior.

6. To stop the application, browse to [the stop page](http://localhost:8080/stop)

##Development direction

Tasks remaining for core functionality:

* ~~Hook up GetAll queries~~
* ~~Hook up Remove command for both classes~~
* ~~Add tag attribute to User and Item objects~~
* ~~Hook up AddTag and RemoveTag commands for both classes~~
* ~~Create Subscriber as recipient of Notification objects~~
* ~~Hook up Notifications to be sent as a by-product of AddTag and RemoveItem commands~~

Additional:

* Add applicative validation with scalaz per https://github.com/bmorris458/market-lib and  https://github.com/ironfish/akka-persistence-mongo-samples
* Clean up routes (make respond correctly to GETs and PUTs, rather than the sloppy front-end put together for this demo) and add view templates per http://tysonjh.com/blog/2014/05/05/spray-custom-404/

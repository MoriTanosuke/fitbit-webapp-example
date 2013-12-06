What is this?
=============

This is a simple webapp that shows how to do the [oauth jumble][0] and read data from the [Fitbit API][1].
It does almost nothing, just authorizing with fitbit and then displaying your steps of the last 7 days
on a graph.

How do I run this?
==================

[Register a *desktop* application][3] and copy your applications *consumer key* and *secret*.

Download the latest release JAR, open a Terminal and type this:

    java -DCONSUMER_KEY=XXX -DCONSUMER_SECRET=YYY -jar fitbit-webapp-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar

After that, open a browser and point it to http://127.0.0.1:4567 - that's it.

[0]: https://wiki.fitbit.com/display/API/OAuth+Authentication+in+the+Fitbit+API
[1]: http://dev.fitbit.com/
[3]: https://dev.fitbit.com/apps

# Maps integrated Calendar

This microservice is build for a calendar application which focuses on the geographic places of the entries.
Each entry is saved with the coordinates.
There are endpoints for retrieving all upcoming events in the specified range which can be used to place them on a map.
Additionally it allows to register a location which will mark all nearby events that are happening at the moment as attended.
This data can be used by another endpoint to get reports for the number of attended entries in a specified range of time.

## Usage
This is a gradle project with a MongoDB backend.
The path to the MongoDB needs to be specified in the [MongoConfig](https://github.com/TheSlimvReal/maps-calendar-backend/blob/master/src/main/kotlin/com/example/calendarapp/MongoConfig.kt).
After specifying the correct path the app can be run locally.

An example implementation of the frontend can be found [here](https://github.com/TheSlimvReal/maps-calendar-frontend).

Have fun and start contributing.
<p align="center">
<img src="screenshots/movie_night.svg" alt="MovieNight Logo" width="680" height="260"/>
</p>

# It's time for Movie Night!

MovieNight is a media-organizing and streaming application designed to work on a local network.
If you have a large local collection of movies, TV shows, and/or music videos, MovieNight
provides a simple and friendly interface to browse your collection and stream media to your devices.

An Admin UI is provided to add, edit, or delete media items in your collection. Then you can use
the Browse/Search UI to explore your collection and stream media!

## Getting started

MovieNight has been tested on Linux. To get started, clone the repository and build it (Maven/Java25):

```bash
git clone https://github.com/scorbo2/MovieNight.git

# We build from the "backend" directory, but this builds the whole thing:
cd MovieNight/backend
mvn clean package
```

This generates a standalone executable Jar file in the `backend/target` directory.
You can run it with default settings using:

```bash
# Note: version number may vary from this example:
java -jar target/MovieNight-2.0.jar
```

If this is your first time running, the application, it will enter "interactive config mode" and ask
you questions to create the initial config. See the "Interactive Config Mode" section below for more details.
You can avoid that by creating a config file yourself and specifying its location with the
`MOVIENIGHT_CONFIG_FILE` environment variable (see "Custom Config File" section).

## First time run

When the UI first comes up, it will look pretty empty!

![MovieNight UI - First Run](screenshots/ui-first-run.png)

Click the `Admin` link at the top of the page to access the Admin dashboard:

![MovieNight Admin UI - First Run](screenshots/ui-admin-mode.png)

At any point, you can optionally click the "dark mode" toggle in the very top right to stop burning your eyes:

![MovieNight UI - Dark Mode](screenshots/ui-admin-mode-dark.png)

### Setting up media groups

Click the "New Group" quick action to add your first media group. A "group" is a collection of media items, 
and can be used to organize your collection. A suggested use of groups:

- Movies
- TV Shows
- Music Videos

When creating your first groups, select "None (top-level)" for the "Parent group". This creates a "top-level" group.
These top-level groups will be displayed on the main browse page. You can also add a thumbnail image for each group,
which makes browse mode look a lot nicer. Once our initial groups are set up, click "Browse app" in the top right
to return to browse mode:

![MovieNight UI - Browse Mode](screenshots/ui-browse-mode.png)

Okay, looking a bit better now! But we don't have any media items yet. 

### Setting up media items



```bash
scorbett@sclaptop6:~/MovieNight/backend$ java -jar target/MovieNight-2.0-SNAPSHOT.jar 
No configuration file found!

What port shall we listen on? [8080]: 9999
Where are our media files? [/home/scorbett/MovieNight/backend/.]: 
Where are our thumbnails? [/home/scorbett/MovieNight/backend/thumbnails]: /home/scorbett/MovieNight/backend/
Where is our database file? [/home/scorbett/MovieNight/backend/MovieNight.db]: 
Database file does not exist. Will be created.
Enable file-based logging? [y/N]: y
Where should we write logs? [/home/scorbett/MovieNight/backend/MovieNight.log]: 
Would you like to save this config? [Y/n]: n
Config not saved. You will need to re-enter these values next time.
File logging enabled: /home/scorbett/MovieNight/backend/MovieNight.log
2026-05-29 11:18:03 P.M. [INFO] AppConfig {
  port=9999,
  mediaDir=/home/scorbett/MovieNight/backend/.,
  dbFile=/home/scorbett/MovieNight/backend/MovieNight.db,
  thumbnailDir=/home/scorbett/MovieNight/backend,
  defaultPageSize=50,
  apiBasePath='/api/',
  rangeLimitMB=32,
  logFile=/home/scorbett/MovieNight/backend/MovieNight.log

2026-05-29 11:18:04 P.M. [INFO] Connected to database at /home/scorbett/MovieNight/backend/MovieNight.db
2026-05-29 11:18:04 P.M. [INFO] Database initialized and connected.
2026-05-29 11:18:04 P.M. [INFO] MovieNight web UI: http://localhost:9999
2026-05-29 11:18:04 P.M. [INFO] MovieNight API: http://localhost:9999/api/
Server is running. Press Ctrl+C to stop.  
}
```

Mostly, you can just keep hitting `Enter` to accept the defaults, or modify them as needed. You have the option
of saving the config file to disk, or not. Once complete, the application echoes the active configuration and
then starts up. The web UI is then available at `http://localhost:9999`, and the back-end API is shown
for debugging purposes. 
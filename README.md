# chunkmaster [![](https://circleci.com/gh/Trivernis/spigot-chunkmaster.svg?style=shield)](https://app.circleci.com/pipelines/github/Trivernis/spigot-chunkmaster) [![CodeFactor](https://www.codefactor.io/repository/github/trivernis/spigot-chunkmaster/badge)](https://www.codefactor.io/repository/github/trivernis/spigot-chunkmaster) [![](https://img.shields.io/discord/729250668162056313)](https://discord.gg/KZcMAgN)

This plugin can be used to pre-generate the region of a world around the spawn chunk(s).
The generation automatically pauses when a player joins the server (assuming the server was empty before)
and resumes when the server is empty again. The generation also auto-resumes after a server
restart. The plugin tracks the ticks per second and pauses the generation when the tps
is lower than 2 (configurable).

## Built with

- [Gradle](https://gradle.org/) - Dependency Management and Build Tool
- [Sqlite JDBC](https://bitbucket.org/xerial/sqlite-jdbc/) - Database Driver for JDBC
- [bStats](https://bstats.org/) - Statistical Insights

## Features

- Pregeneration of a specific area around the world center
- Configuration of world centers
- Integration into dynmap
- Teleportation to chunks
- Auto-Pause/Resume on player join/leave
- Highly configurable

## Installing

Just download the jar from the latest release and place it into the servers plugins folder.

## Usage and Configuration

### Commands

All features can be accessed with the command `/chunkmaster` or the aliases `/chm`, `chunkm`, `cmaster`.

- `/chunkmaster generate [world] [radius] [<square|circle>]` Starts the generation until the specified chunk count or the world border is reached.
- `/chunkmaster list` Lists all running generation tasks
- `/chunkmaster cancel <Task id|world name>` Cancels the generation task with the specified id (if it is running).
- `/chunkmaster pause` Pauses all generation tasks until the resume command is executed.
- `/chunkmaster resume` Resumes all paused generation tasks.
- `/chunkmaster reload` Reloads the configuration file.
- `/chunkmaster tpchunk <X> <Z>` Teleports you to the specified chunk coordinates.
- `/<command> setCenter [[<world>] <chunkX> <chunkZ>]]` - sets the center chunk of the world
- `/<command> getCenter [<world>]` - returns the center chunk of the world
- `/<command> stats [<world>]` - returns the stats of the server or a specific world

#### Examples
**Generate a 100 chunks * 100 blocks square around the center:**

`/chm generate [world] 50`

**Generate a circle with a radius of 100 blocks around the center:**

`/chm generate [world] 100 circle`

### Config

```yaml

# The language settings.
# Supported out of the box are german (de) and english (en).
# Additional languages can be configured in the plugins folder under i18n.
# The file name must be in the format <language>.i18n.properties and the content
# must be in the java-property-file format.
# For non-defined translations the default (english) version is used.
# For built-in support please create a PullRequest with your translation.
language: en

# Actiates/deactivates the dynmap integration.
# With the setting set to 'true' the plugin tries to trigger the rendering
# of generated chunks right before unloading them. It also adds an area
# marker to the dynmap to show the area that will be pregenerated.
# The marker is removed automatically when the task is finished or canceled.
# The value should be a boolean <true/false>
dynmap: true

generation:

  # If set to true the plugin ignores the vanilla world border and doesn't stop
  # the chunk generation when reaching it.
  # The value should be a boolean <true/false>
  ignore-worldborder: false

  # The maximum amount of chunks that are loaded before unloading and saving them.
  # Higher values mean higher generation speed but greater memory usage.
  # The value should be a positive integer.
  max-loaded-chunks: 1000

  # Paper Only
  # The maximum amount of requested chunks with the asynchronous paper chunk
  # loading method. Higher values mean faster generation but more memory usage and
  # bigger performance impact. Configuring it too hight might crash the server.
  # The value should be a positive integer.
  max-pending-chunks: 500

  # The maximum milliseconds per tick the server is allowed to have
  # during the cunk generation process.
  # If the mspt is greather than this, the chunk generation task pauses.
  # The value should be a positive integer greater than 50. 
  mspt-pause-threshold: 500

  # The period in ticks for how often loaded chunks get unloaded.
  # Unloading happens in the main thread and can impact the server performance.
  # You can tweak this setting with the max-loaded-chunks setting to have either
  # a lot of chunks unloaded at once or fewer chunks unloaded more often.
  # If the maximum number of loaded chunks is reached the generation pauses until the
  # unloading task runs again so keep that in mind.
  # The value should be a positive integer.
  unloading-period: 50

  # Pauses the generation if the number of players on the server is larger or equal
  # to the configured value
  # Notice that playing on a server that constantly generates chunks can be
  # very laggy and can cause it to crash.
  # The value should be a posivitve integer > 1.
  pause-on-player-count: 1

  # if the generation should automatically start on server startup
  # the value should be a boolean
  autostart: true
```

### Spigot and Paper

The plugin works on spigot and paper servers but is significantly faster on paper servers
(because it profits from asynchronous chunk loading an the better implementation of the
isChunkGenerated method).

## Translation

The **Mandarin** translation is provided by [NPBeta](https://github.com/NPBeta) and
was validated by [ed3d3d](https://twitter.com/ed3d3d).

The **French** translation is provided by [Corenb](https://github.com/Corenb) and
was validated by [Fiwel00](https://github.com/Fiwel00) and [Youssef Habri](https://github.com/youssefhabri).

The **German** and **English** translation is provided by me.
You can translate the plugin yourself and start a PR to this repository to add it to the
provided translation.

1. create an i18n folder in the plugins folder (plugins/Chunkmaster)
2. copy the [default translations file](https://github.com/Trivernis/spigot-chunkmaster/blob/master/src/main/resources/i18n/DEFAULT.i18n.properties) 
into the newly created folder and rename it to <language-abbrevation>.i18n.properties
3. modify the values in the file for your translation (you can use minecraft § formatting sequences)
4. set the language property in the config file to your language abbrevation
5. start the plugin

Now you should see your translation being used by the plugin for localized messages.

## License

This project is licensed under the GPLv3.0 License - see the 
[LICENSE](https://github.com/Trivernis/spigot-chunkmaster/blob/master/LICENSE) for details.

## bStats

[![Plugin statistics](https://bstats.org/signatures/bukkit/chunkmaster.svg)](https://bstats.org/plugin/bukkit/Chunkmaster/5639)

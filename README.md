# chunkmaster ![](https://abstruse.trivernis.net/badge/1)

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
- `/<command> setCenter [<world>] <chunkX> <chunkZ>` - sets the center chunk of the world
- `/<command> getCenter [<world>]` - returns the center chunk of the world

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
  max-loaded-chunks: 10

  # Paper Only
  # The maximum amount of requested chunks with the asynchronous paper chunk
  # loading method. Higher values mean faster generation but more memory usage
  # (and probably bigger performance impact).
  # The value should be a positive integer.
  max-pending-chunks: 10

  # The period (in ticks) in which a generation step is run.
  # Higher values mean less performance impact but slower generation.
  # The value should be a positive integer.
  period: 2

  # The max amount of chunks that should be generated per step.
  # Higher values mean higher generation speed but higher performance impact.
  # The value should be a positive integer.
  chunks-per-step: 4

  # Paper Only
  # The number of already generated chunks that will be skipped for each step.
  # Notice that these still have a performance impact because the server needs to check
  # if the chunk is generated.
  # Higher values mean faster generation but greater performance impact.
  # The value should be a positive integer.
  chunk-skips-per-step: 100

  # The maximum milliseconds per tick the server is allowed to have
  # during the cunk generation process.
  # If the mspt is greather than this, the chunk generation task pauses.
  # The value should be a positive integer greater than 50.  
  mspt-pause-threshold: 500

  # Pauses the generation if the number of players on the server is larger or equal
  # to the configured value
  # Notice that playing on a server that constantly generates chunks can be
  # very laggy and can cause it to crash.
  # The value should be a posivitve integer > 1.
  pause-on-player-count: 1
```

### Spigot and Paper

The plugin works on spigot and paper servers but is significantly faster on paper servers
(because it profits from asynchronous chunk loading an the better implementation of the
isChunkGenerated method).

## License

This project is licensed under the GPLv3.0 License - see the [License.md](https://github.com/Trivernis/spigot-chunkmaster/blob/master/LICENSE) for details.

## bStats

[![Plugin statistics](https://bstats.org/signatures/bukkit/chunkmaster.svg)](https://bstats.org/plugin/bukkit/Chunkmaster/5639)
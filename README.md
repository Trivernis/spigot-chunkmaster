# chunkmaster

This plugin can be used to pre-generate the region of a world around the spawn chunk.
The generation automatically pauses when a player joins the server (assuming the server was empty before)
and resumes when the server is empty again. The generation also auto-resumes after a server
restart. The plugin tracks the ticks per second and pauses the generation when the tps
is lower than 2.

## Commands

All features can be accessed with the command `/chunkmaster` or the aliases `/chm`, `chunkm`, `cmaster`.

- `/chunkmaster generate [world] [chunk count]` Starts the generation until the specified chunk count or the world border is reached.
- `/chunkmaster list` Lists all running generation tasks
- `/chunkmaster cancel <Task id>` Cancels the generation task with the specified id (if it is running).
- `/chunkmaster pause` Pauses all generation tasks until the resume command is executed.
- `/chunkmaster resume` Resumes all paused generation tasks.
- `/chunkmaster reload` Reloads the configuration file.

## Config

```yaml
generation:

  # The period (in ticks) in which a generation step is run.
  # Higher values mean less performance impact but slower generation.
  # The value should be a positive integer.
  period: 2

  # The number of already generated chunks that will be skipped for each step.
  # Notice that these still have a performance impact because the server needs to check
  # if the chunk is generated.
  # Higher values mean faster generation but greater performance impact.
  # The value should be a positive integer.
  chunks-skips-per-step: 5

  # The maximum milliseconds per tick the server is allowed to have
  # during the cunk generation process.
  # If the mspt is greather than this, the chunk generation task pauses.
  # The value should be a positive integer greater than 50.  
  mspt-pause-threshold: 500

  # If the chunk generation process should pause on player join.
  # Notice that playing on a server that constantly generates chunks can be
  # very laggy and can cause it to crash.
  # You could configure the values above so that the performance impact of the generation
  # process is minimal.
  # The value should be a boolean <true/false>
  pause-on-join: true
```
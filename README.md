# chunkmaster

This plugin can be used to pre-generate the region of a world around the spawn chunk. The plugin provides the commands

- /generate - Pre-generates chunks in the current world
- /listgentasks - Lists all running generation tasks (and their ids)
- /removegentask - Removes a generation task (stops it permanently)

The generation automatically pauses when a player joins the server (assuming the server was empty before)
and resumes when the server is empty again. The generation also auto-resumes after a server
restart. The plugin tracks the ticks per second and pauses the generation when the tps
is lower than 2.

## Future Features

- pause generation tasks until restarted by command
- configure the tps pause limit
- specify how many chunks to generate for a task
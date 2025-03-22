# DLDS Unofficial
This plugin is a fan-made recreation of the one used in a streaming event hosted by [PietSmiet](https://twitch.tv/PietSmiet).
The event is an advancement run in which teams of streamers are given points for each advancement they obtain. Their points will count towards a leaderboard.
The goal is to get the most amount of points within 12 hours without dying.
The first player in a team to complete an advancement is also given a reward, which differs for each advancement. The type of reward is determined by the difficulty of the advancement. You can learn more about DLDS [here](https://www.pietsmiet.live).

## Warning: Alpha
The plugin is in very early development. Expect bugs and/or crashes.

## Features
The plugin currently contains most of the core mechanics of the event but is very rough around the edges, as I currently lack the time to polish it. Right now, it supports:
- Automatic tracking of advancements and points
- Automatic rewarding of the first team member who gains an advancement
- Playing alone or with any number of friends (the point cap adjusts automatically)
- Custom day/night cycle (15m/5m)
- Automatically adjusting all required settings (gamemode, difficulty, world border, player spawn)
- Permadeath (can be turned off)
- Customization (see below)
- Time measurement (also the ability to change the time during a game)
- Automatic Ender Dragon respawn after 10 minutes

The following features are currently missing / need work:
- Polishing (Colorful text, Soundeffects, ...)
- Multiple teams on one server

## Customization
**Note:** By default, all settings are chosen such that they reflect the official rules. You might, however, want to change settings such as permadeath if you play with friends.

Once the server has been started, you can find multiple configuration files in the `plugins/DLDS` folder. The file `config.yml` is for general customization. You can change the difficulty (choose between `EASY`, `NORMAL`, and `HARD`), the world border size (choose `0` to disable), and whether permadeath should be enabled (choose between `true` and `false`).

The file `rewards.yml` contains all advancements and their rewards. Only change this file if you know what you are doing!

The file `gamestate.json` only exists when a game of DLDS is currently running (and the server is offline). Do not touch this file. It contains the current game state.

After modifying the files, restart the server to apply your changes.

## Usage
The plugin offers four commands to control the game:
- `/dlds enter`
- `/dlds start`
- `/dlds stop`
- `/dlds time set playername hours minutes seconds`

Every player who wants to participate in the game must execute the `/dlds enter` command to register for the event. **You are not able to add more players after the event has started**. Once every player has entered, use the `/dlds start` command to start the round. If you want to stop the current round, use `/dlds stop`. **Careful: This will immediately reset your progress and remove the scoreboard. You will not be able to restore the game state.**

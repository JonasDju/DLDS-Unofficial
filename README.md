# DLDS Unofficial
This plugin is a fan-made recreation of the one used in a streaming event hosted by [PietSmiet](https://twitch.tv/PietSmiet).
The event is an advancement run in which teams of streamers are given points for each advancement they obtain. Their points will count towards a leaderboard.
The goal is to get the most amount of points within 12 hours without dying.
The first player in a team to complete an advancement is also given a reward, which differs for each advancement. The type of reward is determined by the difficulty of the advancement. You can learn more about DLDS [here](https://www.pietsmiet.live) (in german).

## Features
The plugin currently contains all of the core mechanics of the event:
- Automatic tracking of advancements and points
- Automatic rewarding of the first team member who gains an advancement
- Playing alone or with any number of friends (the point cap adjusts automatically)
- Custom day/night cycle (15m/5m)
- Automatically adjusting all required settings (gamemode, difficulty, world border, player spawn)
- Permadeath (can be turned off)
- Customization (see below)
- Time measurement (also the ability to change the time during a game)
- Automatic Ender Dragon respawn after 10 minutes (can be configured)
- Multiple teams on one server

## Usage
The plugin offers five commands to control and interact with the game:
1. `/dlds team`: This command is used to manage teams and assign players to existing teams. 

    If this is your first time starting a game of DLDS, you want to use this command to create one or more teams. Use the following sub-commands:

    - `/dlds team create <teamname>`: Creates a team with the given name. The team name must be one word.
    - `/dlds team delete <teamname>`: Deletes the team with the given name.
    - `/dlds team addplayer <player> <teamname>`: Adds the given player to the given team. A player can only be in one team at a time.
    - `/dlds team removeplayer <player>`: Removes the given player from its current team.
    - `/dlds team list`: View a list of all existing teams and their assigned players.


2. `/dlds start <teamname>`: This command is used to start the game for the given team.

    Before you can use this command, you need to create a team and add players to it. Look at the `/dlds team` command above for instructions.


3. `/dlds stop <teamname>`: This command is used to stop the game for the given team.


4. `/dlds time set <player> <hours> <minutes> <seconds>`: This command is used to set the remaining time for a given player.


5. `/dlds leaderboard`: This command shows the current leaderboard.

    Note, that it only contains teams that have players assigned to them and that the teams are ordered by the percentage of achievable points.
    This is done to make sure that teams with fewer players don't have a disadvantage.

### Example
Peter and Dennis want to play a game of DLDS with their friend Jay, who is sure that he can win alone against both of them.
Peter starts by creating two teams, one called `PietSmiet` and one called `Salzmine` by using the following commands:

- `/dlds team create PietSmiet`
- `/dlds team create Salzmine`

He then assigns himself and Dennis to PietSmiet and Jay to Salzmine:

- `/dlds team addplayer Peter PietSmiet`
- `/dlds team addplayer Dennis PietSmiet`
- `/dlds team addplayer Jay Salzmine`

He now uses `/dlds team list` to confirm that he didn't make any mistakes. To start the game for his own team and Jay, he runs:

- `/dlds start PietSmiet`
- `/dlds start Salzmine`

During the game, all players can check the leaderboard by running `/dlds leaderboard` to see which team is in first place.
Of course, Jay dies immediately and wants to start over because his death was just bad luck. To this end, Peter executes:

- `/dlds stop Salzmine`: To stop Jay's running game of DLDS.
- `/dlds start Salzmine`: To start a new game for Jay. This will reset his score and advancements.


## Customization
**Note:** By default, all settings are chosen such that they reflect the official rules. You might, however, want to change settings such as permadeath if you play with friends.

Once the server has been started, you can find multiple configuration files in the `plugins/DLDS` folder. The file `config.yml` is for general customization.
It contains many settings you can change, along with an explanation for each of them.

The file `rewards.yml` contains all advancements and their rewards. Only change this file if you know what you are doing!

The file `gamestate.json` only exists when a game of DLDS is currently running (and the server is offline). Do not touch this file. It contains the current game state.

After modifying the files, restart the server to apply your changes.
A game/instance can be very complex. There has to be some generality depending on the slots of a such game or instance.
We distinguish between a player, a spectator and admin.

## Lists

A slotted instance or game holds has several lists of accounts (read Minecraft players) that are connected or connecting to that instance/game.
These lists are hierarchical:

#*reserved* >= #*online* >= #*players* >= #*playing*

Whenever one UUID is in a list, it is definitely also in the ones left of it.
Whenever there is a limitation on a list, this also limits the ones right of it.

- If the *online* list is not joinable, then the *players* and *playing* lists are also unjoinable.
- If the *players* list is not joinable, then the *playing* list is unjoinable.
- If the *online* list is full, so would the *players* and *playing* lists be.
- If the *players* list is full, so would the *playing* list be.

To the last two items of the last list, there is an exception:
Every list can set whether it allows kicking.
Whenever a player tries to join, they do so with a queue priority.
If one list is full and the queue system has resulted in joining that specific slottable.
Then it may kick a player with a lower priority if it is allowed.
- If the *online* list allows kicking, then it does not have to be full per se.
- If the *players* list allows kicking, then it does not have to be full per se.
- If the *playing* list allows kicking, then it does not have to be full per se.

### Reserved
The *reserved* list is the most comprehensible list as it contains all UUIDs that are currently connected or still connecting to that instance/game.
This therefore also includes UUIDs that are not on that instance. Its maximum is defined by the next mentioned *online* list,
as when everyone is connected, this is equal to the online list.

### Online
The *online* list is everyone from the *reserved* list excluding the UUIDs that are not yet connected.
This means for these UUIDs, they are on the instance/game.

### Players
The *players* list is everyone from the *online* list excluding the UUIDs whose presence should not be notified.
This sounds vague, but this basically means only the UUIDs that are involved in the instance/game.
For example, whenever an admin is vanished to do some investigation or other kind of works, 
they would not be within this list, as they are not "involved" in the game.

### Playing
The *playing* list is everyone from the *playing* list excluding the UUIDs that are not playing.
Well, that sounds odd, some instances/games might use spectators for example, these would not be playing.
This is therefore implementation dependent, whether this list is unequal to the *players* list.
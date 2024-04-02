In order to let players play, a game first has to be registered to the server.
This happens by announcing it to the Controller:

1. new LaneGame()
2. Send GameStatusUpdatePacket to Controller
3. Controller saves game in its data

Any status changes are done via:
1. Send GameStatusUpdatePacket to Controller

A player joins like the following:

1. Player clicks to join game
2. Controller receives game join
3. Controller sends InstanceJoinPacket to the instance.
4. Controller forwards the player to the instance.
5. If instance sees that player is going to be in join state, send to game.


A player has several quit reasons, to be caused by several systems:
1. A player quits the server
2. A player is sent to another server.

In both ways the following is supposed to be done:
1. Clear the player from the respective games/instances
2. Let controller send data to other server when needed
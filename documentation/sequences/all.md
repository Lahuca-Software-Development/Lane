
TODOOOOOOOOO Rename to ControllerProcessEvent/Instance... so that diff is clear

# Sequences
## Player network join
Whenever a player joins the network, it is registered at the controller (if not initialized, player is kicked).
Registering does the following steps:
1. The player is added to the Lane player list.
2. The current assigned MC username is saved to the UUID.
3. The effective locale of the player is set (if a locale is saved in the data manager).

After the player has joined, it issues an MC server to connect to:
1. A new QueueRequest with the NETWORK_JOIN reason and lobby parameters bundled in a QueueStageEvent are sent to the queue system.
2. The queue system computes a default result.
3. The modified QueueStageEvent (with the default result) is forwarded to the controller, so that any plugin can react to it.
4. The queue system checks whether the result is valid: i.e. when joining an instance/game, there should be spots left.
If it fails, it restarts from step 2 again, but with the additional stage that has the failed information.
5. The result is sent back to the original request, where the result is performed.
6. In the mean time, when the player is sent to the game/instance, the PlayerNetworkProcessEvent event is sent.
This event consists of the player and has a boolean that determines whether they are processed.
A plugin can set the boolean to false, when it still needs to do additional processing before other plugins can register the player.
This is useful for example for an offline player verification system, so that no other plugins hook into it.
7. If the player needs to be processed, the plugin processing it should tell the Controller when it is done.
There is a timeout and denied state, if any of these occur, the player is disconnected.
8. If the plugin is done processing it successfully, another PlayerNetworkProcessEvent is sent.
This will let other plugins also handle additional processing.
9. When all plugins are done processing, a PlayerNetworkProcessedEvent is sent, which implies that all plugins can register the player.

While the server is connecting to the MC server, the player instance/game join sequence is performed.

## Player instance/game join
When the player is sent to the MC server, the following steps happen:
1. When the queue system checks when a result from QueueStageEvent is valid, also sends the InstanceJoinPacket.
This packet contains information about the player and makes the reservation on that instance/game.
When it cannot make the reservation, another QueueStageEvent is called and everything happens again.
2. So the instance/game is found, the player is forwarded to the MC server.
If the player is switching instances/games then a InstanceQuitEvent/InstanceQuitGameEvent is called on the old instance/game.
3. When it connects to the server, the reserved slot of the player is checked.
If it cannot join, it is disconnected, and the QueueStageEvent is called again.
If it can join, a QueueFinishedPacket is sent to the Controller, marking the end of the queue.
This will call a QueueFinishedEvent as well, so that other plugins can monitor on it.
4. After it is checked the Instance/Game can handle the player.
If the player was joining an instance a InstanceJoinEvent is called.
If the player was joining a game, the game gets the player within its methods, and then an InstanceGameJoinEvent is called.

If the player is already online on the given MC server, the player does not need to be forwarded.
The rest behaves the same.

If during any of the it fails, the player is not able to join.
The player is disconnected, which is then being caught within the Controller.
The Controller will once again call the queuing system, which using the QueueStageEvent will handle it.

## Player server kicked
Whenever a player fails connection, or when the MC server closes down unexpectedly this sequence becomes active.
1. When the player is not currently queuing to somewhere, start a new queue with the data of the server kick.
2. Forward the current/new queue stage to the queue system, which will compute a default result.
3. Then a QueueStageEvent is called with this default result to any plugins.
4. The updated QueueStageEvent is used in the player instance/game join sequence.

## New queue
Any plugin can call a new queue for a given player.
1. A new queue is made with the queue parameters and send to the queue system for a default result.
2. Then a QueueStageEvent is called with this default result to any plugins.
3. The updated QueueStageEvent is used in the player instance/game join sequence.

## Disconnect
When a player fully disconnects multiple events on both the proxy and instance.
1. Both the proxy and instance separately flag their disconnect state to the proxy.
2. When both are fully flagged, then the player is unregistered: remove from party, etc.

# Platforms
The actual platforms are better explained here, with a focus on the events.
The events in italics are not recommended to use. In bold are the events from Lane.

## Velocity (Controller)
We are assuming that there is currently only one controller, namely on the Velocity proxy.
### Player network join
There are several events within Velocity that are related to joining.

1. *ConnectionHandshakeEvent*, this is called whenever a handshake is established.
Only useful when hooking into the packet handler.
2. PreLoginEvent, this is called when the connection to the proxy is initialized, but the player is not yet authenticated.
This allows modifying whether the player should be checked to the Mojang servers.
3. GameProfileRequestEvent, this is called after the player has been checked/or is offline.
This allows modifying the GameProfile: UUID, username, profile properties (skin, etc.).
4. LoginEvent, this is called after the player is authenticated, but before they connect to a server. 
This allows denying the login.
This is used in the player network join sequence to listen to a player join.
5. *PostLoginEvent*, this is called after the player is fully done logging in, and about to connect to their first server.
This is only for monitoring mainly.
6. *PlayerChooseInitialServerEvent*, this is called to find a new server for the player.
   1. The Lane controller will handle this and send it to the queue controller as mentioned in the player network join.
   2. **QueueStageEvent**, this will let any plugins allow for modification of the initial server.
   This may be called multiple times.
7. *PreTransferEvent*, this is called before the player is sent to an IP.
8. *ServerPreConnectEvent*, this is called before the player is sent to the server.
This allows changing the server or denying it.
9. *ServerConnectedEvent*, this is called when the connection to the new server has been made and when the previous has been de-established.
10. *ServerPostConnectEvent*, this is called when the connection is done and the server is now available from Player.getCurrentServer().
One might better want to listen to the **QueueFinishedEvent** as mentioned in the player instance/game join sequence.
11. During steps 7 to 10, **PlayerNetworkProcessEvent**, will let plugins flag whether they need to process the player.
This makes it so that there is no additional data writing, whenever the player fails processing.
    1. When a plugin has flagged to process, that plugin can go ahead and when it is done let the Controller know it.
    2. When the Controller does not receive a result within a certain time limit, or when the plugin marks it as failed.
    The player is disconnected.
    3. When is is successful, another **PlayerNetworkProcessEvent** is sent to let other plugins handle it as well.
    When no plugins are wanting to process anymore, the **PlayerNetworkProcessedEvent** is sent.

### Player instance/game join
There is not really anything related the instance/game joining, 
only when a player is successfully connected it retrieves the QueueFinishedPacket, on which the QueueFinishedEvent is called.

### Player server kicked

1. *KickedFromServerEvent*, this is fired when a player is kicked from the server.
This will automatically run the queue system with a new queue request.
This works exactly the same by computing a default result and calling the **QueueStageEvent**.
This may be called multiple times.
2. *PreTransferEvent*/*ServerPreConnectEvent*/*ServerConnectedEvent*/*ServerPostConnectEvent*,
these are called exactly the same as steps 7 to 10 from the player network join sequence from Velocity.
3. **QueueFinishedEvent**, this is called when the player found a new instance/game

### New queue
This behaves the same as from the player server kicked sequence of Velocity,
just that it has different queue parameters.

### Disconnect
1. DisconnectEvent, this is ran when the player disconnects, this will flag it to the Controller.
It is undefined what happens when doing any more work than needed on the ControllerPlayer object itself.
Data object identified by their IDs work as usual.

## Paper (Instance)

### Player network join
The user does not enter the network via an instance, therefore there is nothing.

### Player server join
A server join happens the same when the first joined the network or when it has switched to the server it is active on.
The following events that are related to the profiles, should most likely not be used during join when using a proxy.

1. *AsyncPlayerPreLoginEvent*, this is ran when a player is attempting to log in. This is asynchronous.
2. *PlayerConnectionValidateLoginEvent*, this validates whether a connection is able to log in.
3. *PreLookupProfileEvent*, which lets plugins intercept the profile lookup by name.
4. *PreFillProfileEvent*, this is ran when the server is requesting additional profile properties.
5. *FillProfileEvent*/*LookupProfileEvent*, these are called when the profile has been filled/looked up.
4. *PlayerServerFullCheckEvent*, fires when a server is full by default.
A server should have enough slots so that the internal Lane systems can handle this.
5. PlayerJoinEvent, this is called when the connection and profile is fully complete.
The instance will use this to make sure the player is actually allowed to join.
If it is, it sends a QueueFinishedPacket back to the controller.
6. **InstanceJoinEvent**/**InstanceGameJoinEvent**,
the first is called when the player is supposed to join the instance only.
The other is called when the player is joining a game, and the game has already handled it.

### Player server kicked
The proxy already catches kicks by the instance.

## New queue
This works on the controller, as the queue request is sent to the controller.

## Disconnect
1. PlayerQuitEvent, this is ran when the player disconnects, this will flag it to the Controller.
   It is undefined what happens when doing any more work than needed on the InstancePlayer object itself.
   Data object identified by their IDs work as usual.
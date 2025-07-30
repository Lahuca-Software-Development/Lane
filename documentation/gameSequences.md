# Sequences
These sequences are showing how a game is sequenced

## Creation
A game creation is initiated by external events.
That can happen when a Minecraft server has started, an admin runs a command, etc..
The following steps happen between the Instance and the Controller.
1. To create a game, it is registered by doing `<T extends InstanceGame> CompletableFuture<T> LaneInstance.registerGame(Function<Long, T> gameConstructor)`.
The game constructor is a function that gives an ID and gets an InstanceGame.
This is used to properly create a new game ID while still allowing to still use the game object's constructor.
The ID is retrieved by using the `RequestIdPacket`, so that the game ID is unique on the whole network.
The retrieved `InstanceGame` is supposed to contain the same ID when doing `getGameId()`.
It should also contain the correct instance ID.
Do not do any data creation or other important algorithms, as it is not sure whether the game is actually being used.
Do those functions in `onStartup()` as written below.
2. After the game is constructed, the `registerGame` function will add the game to the Instance and announce it to the Controller.
That is done by sending the `GameStatusUpdatePacket` to the Controller.
If an error occurs, the game is not registered and cannot be used, this is returned as exception in the CompletableFuture.
3. When no error has occured, it is registered in the instance, followed by running `onStartup()` in the game itself.
`onStartup()` therefore allows the game itself to handle the startup.
4. The **InstanceStartupGameEvent** is called.
5. The game is returned in the `CompletableFuture` of `registerGame`

To showcase this better, here is an example:

```java
// We first create a simple InstanceGame

import java.util.concurrent.CompletableFuture;

public class MyGame extends InstanceGame {

    public MyGame(long gameId, String instanceId) {
        super(gameId, instanceId);
    }

    public abstract void onStartup() {
        Bukkit.broadcastMessage("Game " + gameId + " started!");
    }


    // We do not care about these below now
    public abstract void onShutdown() {
        throw new Exception("Not implemented");
    }

    public abstract void onJoin(InstancePlayer instancePlayer, QueueType queueType) {
        throw new Exception("Not implemented");
    }

    public abstract void onQuit(InstancePlayer instancePlayer) {
        throw new Exception("Not implemented");
    }

    public abstract void onSwitchQueueType(InstancePlayer instancePlayer, InstancePlayerListType oldPlayerListType, QueueType queueType) {
        throw new Exception("Not implemented");
    }

}

static {
    CompletableFuture<MyGame> future = LaneInstance.getInstance().registerGame(id -> new MyGame(id, LaneInstance.getInstance().getId()));
    future.whenComplete((data, ex) -> {
       if(ex != null) {
           // We got error
           return;
       }
       // We can use data, game is registered
    });
}
```

## Join/Leave/Switch Queue
Read more about these in the player sequences documentation.
The game does not have to handle anything, Lane itself makes sure that the player lists are properly updated.
The state is also automatically synced with the Controller.
The following functions can be used to listen to the events, these are only to be used to do handle it for the game itself.

For joining, `onJoin(InstancePlayer instancePlayer, QueueType queueType)` is called. The `instancePlayer` is already part of the player lists.
It is followed by calling the **InstanceJoinGameEvent**.

For leaving, `void onQuit(InstancePlayer instancePlayer)` is called. The `instancePlayer` is still part of the player lists.
It is removed after execution of `onQuit()`. It is followed by calling the **InstanceQuitGameEvent**.

For switching queue type, `void onSwitchQueueType(InstancePlayer instancePlayer, InstancePlayerListType oldPlayerListType, QueueType queueType);` is called.
The `instancePlayer` is already switched to the new queue type when the function is called.
It is followed by calling the **InstanceSwitchGameQueueTypeEvent**.

Be aware that some platforms use different events, for example for paper: **Paper**Instance...Event

## Deletion
There are two events how a game issues a deletion. Either manually or due to a server disabling.
In the case of a server shutdown, all games are deleted at once.

1. In any case, `LaneInstance.getInstance().unregisterGame(long gameId)` is called.
This will first run `onShutdown()` on the game.
2. Then an **InstanceShutdownGameEvent** is called.
3. This is then followed by sending a `GameShutdownPacket` to the controller.
The controller will then unregister the game in the controller as well.
For all online players on the game, their game is set to null.
It will also queue them with a queue type of `GAME_SHUTDOWN`.
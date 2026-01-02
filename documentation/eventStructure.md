The following is an example to show the structure of events.

# LaneEvent
The base class for all Lane events.

## LanePlayerEvent\<T extends LanePlayer>
A class for a player event, while easily providing the player.

### LaneJoinPlayerEvent\<T extends LanePlayer>
A class for a player joining Lane.

#### JoinPlayerEvent (Instance Java module)
This implements it with the correct LanePlayer.

##### PaperJoinPlayerEvent
This implements it with platform dependent classes.

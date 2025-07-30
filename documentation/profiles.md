Another important concept is profiles.
These are used to easily transfer data over from one player to another.
They can also be used to copy data or to make backups.

# Concept
A profile by its concept is nothing more than an identifier. This will be a UUID.
Instead of storing information using a player's UUID, a profile's UUID can be used.
Now, the information can easily be used by anyone.

## Network profile
By default, players are identified using their player UUID. This is typically still the case.
However, a player also has a network profile. 
One network profile can only have one player UUID attached to it.

## Sub profiles
A sub profile is a profile identified by a different UUID than a network profile.
This can be useful in games where every game creates their own sub profile for one network profile.
For example, this allows for like resetting a game just by deleting that sub profile.
A sub profile has a name per super profile.

It also has a value to determine whether it is "active" or not.
An active profile is a profile that is currently in used/to be used.
This is purely useful when a player can have multiple profiles for the same concept, while making sure that it can switch between them.

## Shared profiles
There are games that allow for sharing profiles.
In SkyBlock multiple people can share an island so that they can manage it together.
Here we demonstrate how that could be set up the best, we assume we have players A and B.
- Player A has a network profile: npA
- Player B has a network profile: npB
- Player A has SkyBlock profile: sbA
- Player B has SkyBlock profile: sbB
- The island uses profile: island

Of course, every player has their own network data, shared network-wide: e.g., coins.
Alongside that, every player can have their own information about SkyBlock using their profiles.
This can, for example, store their personal balance, etc.
Any shared data can be used in the island profile.

Notice here that we never limited the number of SkyBlock profiles.
Maybe it is allowed to have access to multiple islands, in that case, there could be a sbA2 and sbB2.

# Implementation
A UUID identifies a profile.
A profile has a type: network, sub, shared.
- Network: this profile holds the sub and shared profiles of a single player UUID.
- Sub: this profile holds information for a specific game/module of a player
- Shared: this profile holds information about some elements, can be shared by multiple players

One should be cautious when using a shared profile, as transactional functions can only be guaranteed on database systems.

The key "data" (the data object with relational ID of a specific profile and the ID of data) is reserved for profile information.
The profile information is stored as a JSON with at least the following three attributes:

The following attributes are reserved for the data objects of the profiles:

- type
- super
- sub.X (where X stands for the name)

Read more about these in the defaultData.md
These values are reserved by the controller.
# Players
## Username (username)
This value saved the last used Minecraft username of the player.
Only use this value when the username is to be found of offline players, as nicking might change the displayed username of online players.


## Network Profile (networkProfile)
This value determines the network profile of the player.
This is stored as the UUID of the profile.
Any network profile data that is stored at the profile itself are listed here.

### Locale (locale)
This value defines a custom set locale by the player.
By default the locale is taken by the Minecraft client, this does not update this value.
Only whenever an explicit locale change is issued by the player on the server itself.
Then this value is changed. Changing the locale in the client when the data is present, does not change it.
This gives following scenarios:

| Client Locale | Saved Locale | Effective Locale |
|---------------|--------------|------------------|
| A             | NULL         | A                |
| A             | B            | B                |

The saved locale is only explicitly set using methods.

### Nickname (nickname)
This value is the nicknamee of the player.
If it is not set, the player has no nickname.


# Profiles
Every profile has these same data objects. Network profile specific information can be read at "Players » Network Profile" in this document.

## Data (data)
This value is a JSON with the important information that the profile holds. The following attributes are part of the JSON.

### ID (id)
This is basically just the ID in the JSON. This is so that when parsing, the ID does not have to be appended.

### Type (type)
This value determines the profile type, which is one of the following:
- NETWORK
- SUB
- SHARED

### Super profiles (super)
This value determines who use the profile currently.
If the profile is a network profile, this is the player's UUID.
Otherwise these are the UUIDs of the super profiles using it.
Only in the case it is a shared type, this is not limited to only one id.
For every super profile, this profile's UUID is in their sub profiles.

### Sub profiles (sub.X)
This value is a path that has an array list with X as their name.
As an example:
- profiles.PROFILEUUID.subProfiles.SkyBlock = ["UUID1", "UUID2"]
- profiles.PROFILEUUID.subProfiles.Bedwars = ["UUID3"]

This allows nesting of profiles.

# Usernames
This relational table is solely in use to transfer the last seen usernames to UUIDs.
No data besides the UUID should be stored here.

## UUID (uuid)
This value is the UUID that was last used when accessing the given username.
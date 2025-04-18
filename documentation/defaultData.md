These values are reserved by the controller.
# Players
## Locale
This value defines a custom set locale by the player.
By default the locale is taken by the Minecraft client, this does not update this value.
Only whenever an explicit locale change is issued by the player on the server itself.
Then this value is changed. Changing the locale in the client when the data is present, does not change it.
This gives following scenarios:

| Client Locale | Saved Locale | Used Locale |
|---------------|--------------|-------------|
| A             | NULL         | A           |
| A             | B            | B           |

The saved locale is only explicitly set using methods.

## Username
This value saved the last used Minecraft username of the player.
Only use this value when the username is to be found of offline players, as nicking might change the displayed username of online players.

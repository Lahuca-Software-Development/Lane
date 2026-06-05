Data can be saved and managed by the controller.
An abstraction layer is implemented such that the data is can be stored in file systems, databases, etc.
Important properties of this system are:
- Saving of unstructured data
- Permission management
- Version management

<h3>Permission management</h3>
An important aspect is to only give certain instances, modules and plugins access to certain data.
This assures that privacy can be maintained, and that data does not go corrupt due to incorrect handling between modules.
Permissions are assigned by using a key, which remains to be set per plugin. This key has the following format "n-i", where:
- n := A name of the plugin/module, this is to be alphanumerical. Maximum length of 32. The "name" part.
- '-' is the character separating the two
- i := An alphanumerical 6 character long uppercase random ID. The "identifier" part.

An example for a key would be "LobbyStatistics-9NKZ33".

A permission can be given using a key <i>a = p-i</i> to the following types:
- To only the one with the key <i>a</i>, everyone with this key can access it.
- To everyone with the plugin part <i>p</i>, everyone with this name part in their key can access it.
Then the key is simply represented by <i>a=n</i>
- Controller, only the controller has access to this. This is for management features and to save the rights.
This can also be used when the controller has different methods handling some data objects: e.g. the locale of a player.
This can also be represented with the key <i>a=#-#</i>
- Everyone, this can also be represented with the key <i>a=\*-\*</i>

A data object has permissions, we seperate between these rights:
- Read: this allows to read the contents of the data object.
- Write: this allows to modify the contents of the data object, and to remove the data object.

<h3>Data object</h3>
A data object contains information about its permissions and its contents. 

Data objects can be temporarily or permanent.
Permanent data objects are only removed upon explicit call of removal. 
Temporarily data objects are removed either on controller stop, or after a given time frame.
This allows for caching for example.
This is to be defined by setting the time of removal of the data object:
0 = upon controller stop,
-1 = never.

Data objects can be tied to players, plugins, etc. this requires the need of assigning them by tables.
Therefore, a data object is either singular or relational.
A singular data object only has an ID.
A relational data object has along side its ID, another rID and the relation type.
Where the relation type can be of any sort, the rID of the rType (relation type) PLAYERS, would mean the player UUID, while the ID means the key of what data object is accessed.

It should be mentioned that an ID has to be formatted in such a way that different plugins/modules do not overwrite each other unwanted.
For any plugin/module specific values the intended ID is to be 'main.key'.
Here always the module/plugin is given 'main' and then split with a dot in between to mark the exact ID.
It is wise to split these keys up even more according to the usage: 'main.sub.key'.
For general data objects, the ID can also be 'key'.

Version management in data objects is simple, every data object has a integer attached that defines the version.
The default number is zero. This is purely useful for plugins that have updated their internal data.
This allows them to update the value of the data whenever the version has changed.

The time the data object has lastly been updated is also saved: when it was created or when it was lastly changed.

For example, the locale of me (Laurenshup) is stored in the data object with rType PLAYERS, rID e8cfb5f7-3505-4bd5-b9c0-5ca9a6967daa & ID locale.

<h2>Database structure</h2>
Basically everything could be put in one table:
- Relation type (null = singular)
- Relation ID (null = singular)
- ID
- Permissions
- Last updated time
- Removal date
- Version
- Contents

<h2>Data Manager</h2>
We have the following methods implemented:
- readDataObject: reading
- writeDataObject: writing
- removeDataObject: removing
- updateDataObject: read + optional write
- listDataObjectIds: reads all data object ids at some prefix
- listDataObjects: read all data objects at some prefix and version
- copyDataObject: read and copy at another location

<h3>Listing Multiple Data Objects</h3>
We want the following to be possible:

<h4>Relational table:</h4>
lane_relational_profiles (RelationalID, ID).
We want the following operations to be possible:
- List all IDs given some relational ID
- List the IDs with a shared prefix for some relational ID
- List the IDs with a shared prefix
- List all
- List all relational IDs given some ID

<h4>Singular table:</h4>
lane_profiles (ID).
We want the following operations to be possible:
- List all
- List with a shared prefix

<h4>Sorting</h4>
Sort on the value depending on the type.
If the type is a number, sort on the CAST(value AS DOUBLE) value.
If the type is a JSON, sort on the JSONPath value.
Otherwise do the default lexicographical sort.

<h4>Matching version</h4>
Filter for the version of the data object.
Either exact, min, max or minmax.

<h4>Filter</h4>
We can require certain values.
This can be very dynamic and therefore very exhaustive.
This would require adding AND, OR and NOT operators between the filters.
A filter could be an exact match on the value or anything else.
This could also work as JSONPath as well.
This will be added later.

# ReplicatedObject

Some objects require an update on an instance when it updates on the instance.
One good example is the LaneParty object, or more specifically the PartyRecord object.
This means that such instance holds the replica of the object.

## AuthoritativeObject

The authoritative object is the main object saved in the controller.

## ReplicaObject

The replica object is the object saved in the replicated object on the instance.

## Subscribers

The subscribers are the instances that hold a replica of the object.
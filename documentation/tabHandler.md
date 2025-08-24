Handle TAB management on Minecraft (instance only).
Features that we need:
- Sorting
- Prefix/suffixes (different to chat, multiple): customization of name display
  - Rank prefix
  - Personal prefix

# Solutions
## Sorting
Allow to set on instance to sort based on some Comparator.

Add like method: `setTabSorter(Comparator<InstancePlayer>)`

And some method: `sortTab()`

## Prefix/suffixes
`setPlayerListName()` is computed by the following methods.

<hr>

```java
interface Instance {
    void updatePlayerListNames();
}

interface Player {
    
    OrderedDataComponents getPlayerListNameData();
    OrderedDataComponents getChatName();
    
}

class OrderedDataSet<T> { // TODO Usee in the future for scoreboards
    Set<OrderedData<T>> data;

    void add(OrderedData<T> data);
    boolean remove(String id); // Some IDs should be default
    void setShouldInclude(Predicate<OrderedData<T>> shouldInclude);
    void setComparator(Comparator<OrderedData<T>> comparator);
}

class OrderedDataComponents extends OrderedDataSet<Component> implements ComponentLike {
    
    Component asComponent();
    
}

/**
 * 
 * - id = The ID of the prefix/suffix/chat tag/etc.
 * - data = The data of the prefix/suffix/chat tag/etc. = (String/Component)
 * - priority = The priority of the prefix/suffix/chat tag/etc.
 * This is used by the "include" predicate.
 * - ordering = The ordering of the prefix/suffix/chat tag/etc.
 * This is used in the comparator to determine the order.
 * - include = The predicate that decides if the prefix/suffix/chat tag/etc. should be included.
 */
class OrderedData<T>(String id, T data, int priority, int ordering, Predicate<Collection<OrderedData<T>>> include) {
    
}
```
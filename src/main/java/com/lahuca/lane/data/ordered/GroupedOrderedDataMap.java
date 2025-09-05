
/*
 * @author _Neko1
 * @date 3. 9. 2025
 */


package com.lahuca.lane.data.ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GroupedOrderedDataMap<T> extends OrderedDataMap<T> {

    private List<Predicate<OrderedData<T>>> groups;

    public GroupedOrderedDataMap() {}

    @SafeVarargs
    public GroupedOrderedDataMap(Predicate<OrderedData<T>>... groups) {
        this(List.of(groups));
    }

    public GroupedOrderedDataMap(List<Predicate<OrderedData<T>>> groups) {
        this.groups = groups;
    }

    public List<List<OrderedData<T>>> sortedGroups() {
        List<List<OrderedData<T>>> groupedData;
        if(groups == null) groupedData = List.of(sort());
        else {
            groupedData = new ArrayList<>();
            groups.forEach(group -> groupedData.add(sort(group)));
        }
        return groupedData;
    }

    public List<Predicate<OrderedData<T>>> getGroups() {
        return groups;
    }

    public void setGroups(Predicate<OrderedData<T>>... groups) {
        setGroups(List.of(groups));
    }

    public void setGroups(List<Predicate<OrderedData<T>>> groups) {
        this.groups = groups;
    }

}

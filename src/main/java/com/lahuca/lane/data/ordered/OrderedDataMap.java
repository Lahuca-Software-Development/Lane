/*
 * @author _Neko1
 * @date 3. 9. 2025
 */

/*
 * @author _Neko1
 * @date 2. 9. 2025
 */

package com.lahuca.lane.data.ordered;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author _Neko1
 * @date 24.08.2025
 **/

public class OrderedDataMap<T> {

    private final Map<String, OrderedData<T>> data = new HashMap<>();

    private Predicate<OrderedData<T>> shouldInclude = d -> false;
    private Comparator<OrderedData<T>> comparator = Comparator.comparingDouble(OrderedData::getPriority);

    public @NotNull @Unmodifiable List<OrderedData<T>> sort() {
        return sort(null);
    }

    public @NotNull @Unmodifiable List<OrderedData<T>> sort(@Nullable Predicate<OrderedData<T>> filter) {
        Collection<OrderedData<T>> all = getData().values();
        if(filter != null) all = all.stream().filter(filter).toList();
        double targetPriority = all.stream().mapToDouble(OrderedData::getPriority).max().orElse(0);
        Collection<OrderedData<T>> finalAll = all;

        return all.stream()
                .filter(d -> d.getPriority() == targetPriority || d.getForceInclude().test(finalAll))
                .sorted(Comparator.comparingInt(OrderedData::getOrdering))
                .toList();
    }

    public OrderedDataMap<T> add(OrderedData<T> data) {
        this.data.put(data.getId(), data);
        return this;
    }

    public OrderedDataMap<T> remove(String id) {
        this.data.remove(id);
        return this;
    }

    public OrderedData<T> get(String id) {
        return this.data.get(id);
    }

    public Map<String, OrderedData<T>> getData() {
        return data;
    }

    public void setShouldInclude(Predicate<OrderedData<T>> shouldInclude) {
        this.shouldInclude = shouldInclude;
    }

    public void setComparator(Comparator<OrderedData<T>> comparator) {
        this.comparator = comparator;
    }

    public Predicate<OrderedData<T>> getShouldInclude() {
        return shouldInclude;
    }

    public Comparator<OrderedData<T>> getComparator() {
        return comparator;
    }
}
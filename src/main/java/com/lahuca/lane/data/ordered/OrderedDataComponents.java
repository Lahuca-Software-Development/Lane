
/*
 * @author _Neko1
 * @date 3. 9. 2025
 */


package com.lahuca.lane.data.ordered;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public class OrderedDataComponents extends GroupedOrderedDataMap<Component> implements ComponentLike {

    public OrderedDataComponents() {
        super();
    }

    public OrderedDataComponents(Predicate<OrderedData<Component>>... groups) {
        super(groups);
    }

    public OrderedDataComponents(List<Predicate<OrderedData<Component>>> groups) {
        super(groups);
    }

    @Override
    public @NotNull Component asComponent() {
        Component result = Component.empty();
        for(List<OrderedData<Component>> group : sortedGroups()) {
            for(OrderedData<Component> data : group) {
                result = result.append(data.getData());
            }
        }
        return result;
    }

}

/*
 * @author _Neko1
 * @date 3. 9. 2025
 */


package com.lahuca.lane.data.ordered;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class OrderedDataComponents extends GroupedOrderedDataMap<Component> implements ComponentLike {

    // TODO Use vars within the components

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
        List<List<OrderedData<Component>>> groups = sortedGroups();

        Component result = Component.empty();
        boolean first = false;

        for(List<OrderedData<Component>> group : groups) {
            Component groupComponent = buildGroupComponent(group);

            if(groupComponent.equals(Component.empty())) continue;

            if(first) result = result.appendSpace();
            else first = true;

            result = result.append(groupComponent);
        }

        return result;
    }

    public Component getGroupComponent(Predicate<OrderedData<Component>> predicate) {
        List<List<OrderedData<Component>>> groups = sortedGroups();
        Component result = Component.empty();

        List<OrderedData<Component>> matching = groups.stream()
                .filter(group -> group.stream().anyMatch(predicate))
                .findFirst()
                .orElse(new ArrayList<>());

        if(matching.isEmpty()) return result;

        result = result.append(buildGroupComponent(matching));

        int index = groups.indexOf(matching);
        if(index >= 0 && index < groups.size() - 1) result = result.appendSpace();

        return result;
    }

    private Component buildGroupComponent(@NotNull List<OrderedData<Component>> group) {
        Component result = Component.empty();

        boolean first = true;
        for(OrderedData<Component> componentOrderedData : group) {
            Component append = componentOrderedData.getData();
            if(append != null && !append.equals(Component.empty())) {
                if(first) {
                    first = false;
                    result = result.append(append);
                } else {
                    result = result.appendSpace().append(append);
                }
            }
        }

        return result;
    }
}

/*
 * @author _Neko1
 * @date 3. 9. 2025
 */

/*
 * @author _Neko1
 * @date 2. 9. 2025
 */

/*
 * @author _Neko1
 * @date 1. 9. 2025
 */

/*
 * @author _Neko1
 * @date 1. 9. 2025
 */

/*
 * @author _Neko1
 * @date 1. 9. 2025
 */

package com.lahuca.lane.data.ordered;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author _Neko1
 * @date 24.08.2025
 **/


/**
 * Used for PlayerListName, create OrderedData<Component> where priorities 1+ are prefixes, 1- are suffixes & 0 is a self name.
 *
 */
public class OrderedDataComponents extends OrderedDataMap<Component> implements ComponentLike {

    @Override
    public @NotNull Component asComponent() {
        List<OrderedData<Component>> prefixes = sort(d -> d.getPriority() > 0);
        List<OrderedData<Component>> selfNames = sort(d -> d.getPriority() == 0);
        List<OrderedData<Component>> suffixes = sort(d -> d.getPriority() < 0);

        Component result = Component.empty();
        for(var p : prefixes) result = result.append(p.getData());
        for(var s : selfNames) result = result.append(s.getData());
        for(var s : suffixes) result = result.append(s.getData());

        return result;
    }
}
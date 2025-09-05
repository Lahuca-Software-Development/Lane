/*
 * @author _Neko1
 * @date 4. 9. 2025
 */

package com.lahuca.laneinstance.scoreboard;

import com.lahuca.lane.data.ordered.OrderedData;
import com.lahuca.lane.data.ordered.OrderedDataComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class PlayerScoreboard {

    protected ComponentLike title;
    private final LineBuilder rows = new LineBuilder();

    public PlayerScoreboard(ComponentLike title) {
        this.title = title;
    }

    public void setTitle(ComponentLike title) {
        this.title = title;
    }

    /**
     * Set rows via LineBuilder consumer.
     */
    public void setRows(@NotNull Consumer<LineBuilder> consumer) {
        consumer.accept(rows);
    }

    public LineBuilder getRows() {
        return rows;
    }

    /**
     * Render scoreboard according to implementation (Paper, etc.)
     */
    public abstract void render();

    public static final class LineBuilder extends OrderedDataComponents {
        // TODO For the future: move like some kind of OrderedDataComponentsBuilder into the base lane.

        private double nextPriority = 0;

        public LineBuilder add(String id, @NotNull ComponentLike component, Predicate<Collection<OrderedData<Component>>> forceInclude) {
            double priority = nextPriority--;
            add(new OrderedData<>(id, component.asComponent(), priority, (int) priority, forceInclude));
            return this;
        }

        public LineBuilder add(@NotNull ComponentLike component, Predicate<Collection<OrderedData<Component>>> forceInclude) {
            double priority = nextPriority--;
            add(new OrderedData<>("e" + priority, component.asComponent(), priority, (int) priority, forceInclude));
            return this;
        }

        public LineBuilder add(@NotNull ComponentLike component) {
            return add(component, collection -> true);
        }

        public LineBuilder add() {
            return add(Component::empty);
        }

    }

}
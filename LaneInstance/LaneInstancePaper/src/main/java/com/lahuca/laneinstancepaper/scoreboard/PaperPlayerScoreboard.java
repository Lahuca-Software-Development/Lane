/*
 * @author _Neko1
 * @date 4. 9. 2025
 */

package com.lahuca.laneinstancepaper.scoreboard;

import com.lahuca.lane.data.ordered.OrderedData;
import com.lahuca.laneinstance.scoreboard.PlayerScoreboard;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PaperPlayerScoreboard extends PlayerScoreboard {

    private final Player player;
    private Scoreboard scoreboard;
    private Objective sideBarObjective;

    public PaperPlayerScoreboard(@NotNull Player player, @NotNull ComponentLike title) {
        super(title);

        this.player = player;
        render();
    }

    @Override
    public void setTitle(ComponentLike title) {
        super.setTitle(title);
        sideBarObjective.displayName(title.asComponent());
    }

    /**
     * Render all rows to the scoreboard.
     */
    public void render() {
        if(!Objects.equals(player.getScoreboard(), scoreboard)) {
            // Scoreboard not even at player
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        if(!Objects.equals(player.getScoreboard().getObjective(DisplaySlot.SIDEBAR), sideBarObjective)) {
            // Objective not at sidebar
            this.sideBarObjective = scoreboard.registerNewObjective("lane", Criteria.DUMMY, title.asComponent());
            this.sideBarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            this.sideBarObjective.numberFormat(NumberFormat.blank());
        }

        scoreboard.getEntries().forEach(scoreboard::resetScores);
        for(OrderedData<Component> row : getRows().sort()) {
            Score s = sideBarObjective.getScore(row.getId());
            s.customName(row.getData());
            s.setScore(row.getOrdering());
        }
    }
}
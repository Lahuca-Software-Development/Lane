package com.lahuca.lanecontrollervelocity.commands;

import com.lahuca.lanecontroller.Controller;
import com.lahuca.lanecontroller.ControllerParty;
import com.lahuca.lanecontroller.ControllerPlayer;
import com.lahuca.lanecontrollervelocity.VelocityController;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author _Neko1
 * @date 17.03.2024
 **/
public class PartyCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player player)) {
            invocation.source().sendPlainMessage("You must be a requested to run this command!"); //TODO: replace
            return;
        }

        String[] args = invocation.arguments();

        Optional<ControllerPlayer> optionalPlayer = Controller.getInstance().getPlayer(player.getUniqueId());
        if(optionalPlayer.isEmpty()) {
            return;
        }

        ControllerPlayer controllerPlayer = optionalPlayer.get();
        Optional<ControllerParty> playerParty = getPartyOfPlayer(controllerPlayer);

        if(args.length == 0) {

            /*
             *
             * /party <Player> - Sends a request to the requested [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
             * /party info - Sends an information about the party
             * /party accept <Player> - Accepts the request from the given requested
             * /party deny <Player> - Denies the request from the given requested
             * /party disband - Disbands the party [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
             * /party kick <Player> - Kicks the requested from the party [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
             * /party warp - Sends all players to the leader's server [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
             * /party leader <Player> - Passes the leader to the given requested [ONLY OWNER OF THE PARTY CAN RUN THIS CMD]
             * /party leave
             *
             */

            //TODO: Send help message
            return;
        }

        if(args[0].equalsIgnoreCase("disband")) {
            runAsPartyLeader(controllerPlayer, party -> Controller.getInstance().disbandParty(party));
        } else if(args[0].equalsIgnoreCase("warp")) {
            runAsPartyLeader(controllerPlayer, party -> controllerPlayer.getGameId()
                    .flatMap(game -> Controller.getInstance().getGame(game))
                    .ifPresent(gameServer -> {
                        Controller.getInstance().joinInstance(party.getId(), gameServer.getServerId());//TODO check if its really serverId
                        //TODO: Send message that party was warped
                    }));
        } else if(args[0].equalsIgnoreCase("kick")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party kick <Player>
                return;
            }

            runAsPartyLeader(controllerPlayer, party -> {
                String name = args[1];
                Controller.getInstance().getPlayerByName(name).ifPresentOrElse(target -> {
                    if(!party.contains(target.getUuid())) {
                        //TODO: send message that given player is not in party
                        return;
                    }

                    party.removePlayer(target);
                    //TODO: send message that given player was kicked from party
                }, () -> {
                    //TODO: send message that given player is not online
                });
            });
        } else if(args[0].equalsIgnoreCase("info")) {
            //Displays the information about the party: members, ?
            //TODO: Edit the message
            playerParty.ifPresent(party -> player.sendPlainMessage(party.toString()));
        } else if(args[0].equalsIgnoreCase("accept")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party accept <Player>
                return;
            }

            String inviter = args[1];

            Controller.getInstance().getPlayerByName(inviter).ifPresentOrElse(controllerInviter -> {
                getPartyOfPlayer(controllerPlayer).ifPresentOrElse(party -> {
                    if(!party.getInvited().contains(player.getUniqueId())) {
                        //TODO send message that this player didnt invite him
                        return;
                    }

                    party.addPlayer(controllerPlayer);
                    //TODO: send message that he accepted it
                }, () -> {
                    //TODO send message that this player didnt invite him
                });
            }, () -> {
                //TODO: send msg to the player that player is offline
            });
        } else if(args[0].equalsIgnoreCase("deny")) {
            if(args.length < 2) {
                //TODO: send help message
                // /party deny <Player>
                return;
            }

            String inviter = args[1];

            Controller.getInstance().getPlayerByName(inviter).ifPresentOrElse(controllerInviter -> getPartyOfPlayer(controllerPlayer).ifPresentOrElse(party -> {
                if(!party.getInvited().contains(player.getUniqueId())) {
                    //TODO send message that this player didnt invite him
                    return;
                }

                party.getInvited().remove(player.getUniqueId());
                //TODO: send message that he denied it
            }, () -> {
                //TODO send message that this player didnt invite him
            }), () -> {
                //TODO: send msg to the player that player is offline
            });
        } else if(args[0].equalsIgnoreCase("leader")) {
            //Only party leader can run this command.

            if(args.length < 2) {
                //TODO: send help message
                // /party leader <Player>
                return;
            }

            runAsPartyLeader(controllerPlayer, party -> {

                String newLeader = args[1];
                Optional<Player> leader = VelocityController.getInstance().getServer().getPlayer(newLeader);

                leader.ifPresentOrElse(leaderPlayer -> {
                    if(!party.contains(leaderPlayer.getUniqueId())) {
                        //TODO: send message that requested is not in the party
                        return;
                    }

                    party.setOwner(leaderPlayer.getUniqueId());
                    //TODO: send message to the new party leader
                }, () -> {
                    //TODO send message that given player is offline.
                });
            });
        } else if(args[0].equalsIgnoreCase("leave")) {
            playerParty.ifPresentOrElse(party -> {
                Controller.getInstance().getPlayer(player.getUniqueId()).ifPresent(target -> {
                    if(!party.contains(target.getUuid())) {
                        //TODO: send message that player is no longer in the party
                        return;
                    }

                    party.removePlayer(target);
                    //TODO: send message that player left the party
                });
            }, () -> {
                //TODO: send message that player is not in the party
            });

        } else {
            // /party <Player> - Will send invitation
            String name = args[0];

            Controller.getInstance().getPlayerByName(name).ifPresentOrElse(target -> {
                if(playerParty.isEmpty()) {
                    Controller.getInstance().createParty(controllerPlayer, target);
                } else {
                    playerParty.get().sendRequest(target);
                }
            }, () -> {
                //TODO send message that player is offline.
            });


            //TODO: Send message that requested was invited
        }
    }


    private void runAsPartyLeader(ControllerPlayer controllerPlayer, Consumer<? super ControllerParty> consumer) {
        getPartyOfPlayer(controllerPlayer).ifPresentOrElse(party -> {
            if(!party.getOwner().equals(controllerPlayer.getUuid())) {
                //TODO: send message that only owner can do this
                return;
            }

            consumer.accept(party);
        }, () -> {
            //TODO send message that player isnt in party
        });
    }

    private Optional<ControllerParty> getPartyOfPlayer(ControllerPlayer controllerPlayer) {
        if(controllerPlayer.getPartyId().isEmpty()) return Optional.empty();
        else return Controller.getInstance().getParty(controllerPlayer.getPartyId().get());
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> possibilities = new ArrayList<>();
        if(!(invocation.source() instanceof Player player)) return possibilities;


        Optional<ControllerPlayer> optionalPlayer = Controller.getInstance().getPlayer(player.getUniqueId());
        if(optionalPlayer.isEmpty()) {
            return possibilities;
        }

        ControllerPlayer controllerPlayer = optionalPlayer.get();

        Optional<ControllerParty> controllerParty = getPartyOfPlayer(controllerPlayer);
        String[] args = invocation.arguments();

        if(args.length == 2 && (args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("leader"))) {
            List<String> partyMembers = new ArrayList<>();
            controllerParty.ifPresent(party -> party.getPlayers().forEach(partyPlayer -> partyMembers.add(partyPlayer.toString())));//TODO: fix the message its just UUID for now

            if(args[1].isEmpty()) {
                return partyMembers;
            }

            for(String current : partyMembers) {
                if(current.toLowerCase().startsWith(args[1].toLowerCase()) || args[1].toLowerCase().startsWith(current.toLowerCase())) {
                    possibilities.add(current);
                }
            }

            return possibilities;
        } else if(args.length == 1) {
            List<String> allNames = new ArrayList<>();
            VelocityController.getInstance().getServer().getAllPlayers().forEach(online -> controllerParty.ifPresent(party -> {
                if(!party.contains(online.getUniqueId())) allNames.add(online.getUsername());
            }));

            if(args[0].isEmpty()) return allNames;


            for(String current : allNames) {
                if(current.toLowerCase().startsWith(args[0].toLowerCase()) || args[0].toLowerCase().startsWith(current.toLowerCase())) {
                    possibilities.add(current);
                }
            }

            return possibilities;
        }

        return possibilities;
    }
}

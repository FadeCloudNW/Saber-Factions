package com.massivecraft.factions.cmd;


import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.WarmUpUtil;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CmdFly extends FCommand {

    /**
     * @author FactionsUUID Team
     */


    public static ConcurrentHashMap<String, Boolean> flyMap = new ConcurrentHashMap<>();
    public static BukkitTask flyTask = null;


    public CmdFly() {
        super();
        this.aliases.addAll(Aliases.fly);
        this.optionalArgs.put("on/off", "flip");

        this.requirements = new CommandRequirements.Builder(Permission.FLY_FLY)
                .playerOnly()
                .memberOnly()
                .build();
    }

    public static void startFlyCheck() {
        flyTask = Bukkit.getScheduler().runTaskTimerAsynchronously(FactionsPlugin.instance, () -> {
            checkTaskState();
            if (flyMap.keySet().size() != 0) {

                for (String name : flyMap.keySet()) {
                    if (name == null) {
                        continue;
                    }
                    Player player = Bukkit.getPlayer(name);

                    if (player == null
                            || !player.isFlying()
                            || player.getGameMode() == GameMode.CREATIVE
                            || !FactionsPlugin.getInstance().mc17 && player.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }

                    FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                    Faction myFaction = fPlayer.getFaction();

                    if (!player.hasPermission("factions.fly.bypassnearbyenemycheck") && !fPlayer.isAdminBypassing()) {
                        if (fPlayer.hasEnemiesNearby()) disableFlightSync(fPlayer);
                        checkEnemiesSync(fPlayer);
                        continue;
                    }

                    FLocation myFloc = new FLocation(player.getLocation());

                    if (Board.getInstance().getFactionAt(myFloc) != myFaction) {
                        if (!checkFly(fPlayer, player, Board.getInstance().getFactionAt(myFloc))) {
                            disableFlightSync(fPlayer);
                        }
                    }

                }
            }

        }, 20L, 20L);
    }

    public static boolean checkFly(FPlayer fme, Player me, Faction toFac) {
        if (Conf.denyFlightIfInNoClaimingWorld && !Conf.worldsNoClaiming.isEmpty() && Conf.worldsNoClaiming.stream().anyMatch(me.getWorld().getName()::equalsIgnoreCase))
            return false;

        if (toFac.getAccess(fme, PermissableAction.FLY) == Access.ALLOW) return true;
        if (fme.getFaction().isWilderness()) return false;
        if (toFac.isSystemFaction())
            return me.hasPermission(toFac.isWilderness() ? Permission.FLY_WILDERNESS.node : toFac.isSafeZone() ? Permission.FLY_SAFEZONE.node : Permission.FLY_WARZONE.node);
        Relation relationTo = toFac.getRelationTo(fme.getFaction());

        if (!relationTo.isEnemy() && !relationTo.isMember())
            return me.hasPermission(Permission.valueOf("FLY_" + relationTo.name()).node);
        return false;
    }


    public static void checkTaskState() {
        if (flyMap.isEmpty()) {
            flyTask.cancel();
            flyTask = null;
        }
    }

    public static void disableFlight(final FPlayer fme) {
        fme.setFlying(false);
        flyMap.remove(fme.getPlayer().getName());
    }

    private static void disableFlightSync(FPlayer fme) {
        Bukkit.getScheduler().runTask(FactionsPlugin.instance, () -> fme.setFFlying(false, false));
        flyMap.remove(fme.getName());
    }

    private static void checkEnemiesSync(FPlayer fp) {
        Bukkit.getScheduler().runTask(FactionsPlugin.instance, fp::checkIfNearbyEnemies);
    }

    public boolean isInFlightChecker(Player player) {
        return flyMap.containsKey(player.getName());
    }

    @Override
    public void perform(CommandContext context) {
        if (!context.fPlayer.isAdminBypassing()) {
            List<Entity> entities = context.player.getNearbyEntities(16.0D, 256.0D, 16.0D);

            for (int i = 0; i <= entities.size() - 1; ++i) {
                if (entities.get(i) instanceof Player) {
                    Player eplayer = (Player) entities.get(i);
                    FPlayer efplayer = FPlayers.getInstance().getByPlayer(eplayer);

                    if (efplayer.getRelationTo(context.fPlayer) == Relation.ENEMY && !efplayer.isStealthEnabled()) {
                        context.msg(TL.COMMAND_FLY_CHECK_ENEMY);
                        return;
                    }

                    context.fPlayer.setEnemiesNearby(false);
                }
            }

            FLocation myfloc = new FLocation(context.player.getLocation());
            Faction toFac = Board.getInstance().getFactionAt(myfloc);

            if (!checkFly(context.fPlayer, context.player, toFac)) {
                context.fPlayer.sendMessage(TL.COMMAND_FLY_NO_ACCESS.format(toFac.getTag()));
                return;
            }
        }

        if (context.args.size() == 0) {
            toggleFlight(context.fPlayer.isFlying(), context.fPlayer, context);

        } else if (context.args.size() == 1) {
            toggleFlight(context.argAsBool(0), context.fPlayer, context);
        }
    }

    private void toggleFlight(final boolean toggle, final FPlayer fme, CommandContext context) {
        if (toggle) {
            fme.setFlying(false);
            flyMap.remove(fme.getPlayer().getName());
            return;
        }

        context.doWarmUp(WarmUpUtil.Warmup.FLIGHT, TL.WARMUPS_NOTIFY_FLIGHT, "Fly", () -> {
            fme.setFlying(true);
            flyMap.put(fme.getPlayer().getName(), true);

            if (flyTask == null) {
                startFlyCheck();
            }
        }, FactionsPlugin.getInstance().getConfig().getLong("warmups.f-fly", 0));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_FLY_DESCRIPTION;
    }

}

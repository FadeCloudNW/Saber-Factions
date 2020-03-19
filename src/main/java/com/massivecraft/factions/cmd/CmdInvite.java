package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.audit.FLogType;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.CC;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;

public class CmdInvite extends FCommand {

    /**
     * @author FactionsUUID Team
     */

    public CmdInvite() {
        super();
        this.aliases.addAll(Aliases.invite);
        this.requiredArgs.add("player name");
        this.requirements = new CommandRequirements.Builder(Permission.INVITE)
                .playerOnly()
                .withAction(PermissableAction.INVITE)
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        FPlayer target = context.argAsBestFPlayerMatch(0);
        if (target == null) {
            return;
        }

        if (target.getFaction() == context.faction) {
            context.msg(TL.COMMAND_INVITE_ALREADYMEMBER, target.getName(), context.faction.getTag());
            context.msg(TL.GENERIC_YOUMAYWANT.toString() + FactionsPlugin.getInstance().cmdBase.cmdKick.getUsageTemplate(context));
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!context.payForCommand(Conf.econCostInvite, TL.COMMAND_INVITE_TOINVITE.toString(), TL.COMMAND_INVITE_FORINVITE.toString())) {
            return;
        }

        System.out.println("----------------------- MEW");
        System.out.println("Faction: " + context.faction.getTag());
        System.out.println("Size:" + context.faction.getFPlayers().size() + " / " + Conf.factionMemberLimit);
        System.out.println("Alt Size:" + context.faction.getAltPlayers().size());
        System.out.println("Invited Size:" + context.faction.getInvites().size());

        for (String name : context.faction.getInvites())
            System.out.println("Invited - " + name);

        System.out.println("----------------------- MEW");

        if (context.faction.isInvited(target)) {
            context.msg(TL.COMMAND_INVITE_ALREADYINVITED, target.getName());
            return;
        }

        if (context.faction.isBanned(target)) {
            context.msg(TL.COMMAND_INVITE_BANNED, target.getName());
            return;
        }

        if (Conf.factionMemberLimit > 0 && context.faction.getFPlayers().size() >= getFactionMemberLimit()) {
            context.msg(TL.COMMAND_JOIN_ATLIMIT, context.faction.getTag(context.fPlayer),
                    getFactionMemberLimit(),
                    context.fPlayer.describeTo(context.fPlayer, false));
            return;
        }

        context.faction.invite(target);
        // Send the invitation to the target player when online, otherwise just ignore
        if (target.isOnline()) {
            // Tooltips, colors, and commands only apply to the string immediately before it.
            FancyMessage message = new FancyMessage(context.fPlayer.describeTo(target, true))
                    .tooltip(TL.COMMAND_INVITE_CLICKTOJOIN.toString())
                    .command("/" + Conf.baseCommandAliases.get(0) + " join " + context.faction.getTag())
                    .then(TL.COMMAND_INVITE_INVITEDYOU.toString())
                    .color(ChatColor.YELLOW)
                    .tooltip(TL.COMMAND_INVITE_CLICKTOJOIN.toString())
                    .command("/" + Conf.baseCommandAliases.get(0) + " join " + context.faction.getTag())
                    .then(context.faction.describeTo(target)).tooltip(TL.COMMAND_INVITE_CLICKTOJOIN.toString())
                    .command("/" + Conf.baseCommandAliases.get(0) + " join " + context.faction.getTag());
            message.send(target.getPlayer());
        }
        context.faction.msg(TL.COMMAND_INVITE_INVITED, context.fPlayer.describeTo(context.faction, true), target.describeTo(context.faction));
        FactionsPlugin.instance.logFactionEvent(context.faction, FLogType.INVITES, context.fPlayer.getName(), CC.Green + "invited", target.getName());
    }

    private int getFactionMemberLimit() {
        return Conf.factionMemberLimit;
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_INVITE_DESCRIPTION;
    }

}

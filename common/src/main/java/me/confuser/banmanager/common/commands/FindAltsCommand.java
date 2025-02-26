package me.confuser.banmanager.common.commands;


import me.confuser.banmanager.common.BanManagerPlugin;
import me.confuser.banmanager.common.CommonPlayer;
import me.confuser.banmanager.common.data.PlayerBanData;
import me.confuser.banmanager.common.data.PlayerData;
import me.confuser.banmanager.common.ipaddr.IPAddress;
import me.confuser.banmanager.common.kyori.text.Component;
import me.confuser.banmanager.common.kyori.text.TextComponent;
import me.confuser.banmanager.common.kyori.text.event.ClickEvent;
import me.confuser.banmanager.common.kyori.text.format.NamedTextColor;
import me.confuser.banmanager.common.kyori.text.format.TextColor;
import me.confuser.banmanager.common.util.IPUtils;
import me.confuser.banmanager.common.util.Message;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FindAltsCommand extends CommonCommand {

  public FindAltsCommand(BanManagerPlugin plugin) {
    super(plugin, "alts", true);
  }

  @Override
  public boolean onCommand(final CommonSender sender, CommandParser parser) {
    if (parser.args.length < 1) {
      return false;
    }

    final String ipStr = parser.args[0];
    final boolean isName = !IPUtils.isValid(ipStr);

    if (isName && ipStr.length() > 16) {
      Message message = Message.get("sender.error.invalidIp");
      message.set("ip", ipStr);

      sender.sendMessage(message.toString());
      return true;
    }

    getPlugin().getScheduler().runAsync(() -> {
      final IPAddress ip = getIp(ipStr);

      if (ip == null) {
        sender.sendMessage(Message.get("alts.header"));
        sender.sendMessage(Message.get("none").toString());
        return;
      }

      List<PlayerData> players = getPlugin().getPlayerStorage().getDuplicatesInTime(ip, getPlugin().getConfig().getTimeAssociatedAlts());

      if (!sender.isConsole()) {
        sender.sendMessage(Message.get("alts.header").set("ip", ipStr).toString());

        if (players.isEmpty()) {
          sender.sendMessage(Message.get("none").toString());
          return;
        }

        ((CommonPlayer) sender).sendJSONMessage(alts(players));
      } else {
        ArrayList<String> names = new ArrayList<>(players.size());

        for (PlayerData player : players) {
          names.add(player.getName());
        }

        sender.sendMessage(Message.get("alts.header").set("ip", ipStr).toString());

        if (names.isEmpty()) {
          sender.sendMessage(Message.get("none").toString());
          return;
        }

        sender.sendMessage(String.join(", ", names));
      }
    });

    return true;
  }

  public static TextComponent alts(List<PlayerData> players) {
    TextComponent.Builder message = Component.text();
    int index = 0;

    for (PlayerData player : players) {
      TextColor colour = NamedTextColor.GREEN;

      if (BanManagerPlugin.getInstance().getPlayerBanStorage().isBanned(player.getUUID())) {
        PlayerBanData ban = BanManagerPlugin.getInstance().getPlayerBanStorage().getBan(player.getUUID());

        if (ban.getExpires() == 0) {
          colour = NamedTextColor.RED;
        } else {
          colour = NamedTextColor.GOLD;
        }
      } else {
        try {
          if (BanManagerPlugin.getInstance().getPlayerBanRecordStorage().getCount(player) != 0) {
            colour = NamedTextColor.YELLOW;
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }

      message
          .append(
                  Component.text(player.getName())
                  .color(colour)
                  .clickEvent(ClickEvent.runCommand("/bminfo " + player.getName())));

      if (index != players.size() - 1) {
        message.append(Component.text(", "));
      }

      index++;
    }

    return message.build();
  }
}

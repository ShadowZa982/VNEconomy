package org.fox.vneconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.fox.vneconomy.tst.Card;
import org.fox.vneconomy.tst.CardPrice;
import org.fox.vneconomy.tst.CardType;
import org.fox.vneconomy.tst.TheSieuTocAPI;

public class NapTheCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cLệnh này chỉ dùng trong game!");
            return true;
        }
        Player p = (Player) sender;

        if (args.length != 4) {
            p.sendMessage("§eSử dụng: /napthe <loai> <menhgia> <seri> <pin>");
            p.sendMessage("§7Ví dụ: /napthe VIETTEL 10000 123456789 987654321");
            return true;
        }

        String type = args[0].toUpperCase();

        if (!CardType.isSupported(type)) {
            p.sendMessage("§cThẻ cào không hỗ trợ!");
            return true;
        }

        int price;
        try {
            price = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            p.sendMessage("§cMệnh giá không hợp lệ!");
            return true;
        }

        // kiểm tra mệnh giá trong config.yml
        int apiPrice = TheSieuTocAPI.getMappedPrice(price);
        if (apiPrice == -1) {
            p.sendMessage("§cMệnh giá thẻ không được hỗ trợ!");
            return true;
        }



        String seri = args[2];
        String pin = args[3];

        CardType cardType = CardType.valueOf(type);
        Card card = new Card(p, cardType, apiPrice, seri, pin);

        p.sendMessage("§aĐang xử lý thẻ " + cardType.getApiName() + " mệnh giá " + price + "...");
        TheSieuTocAPI.processCard(p, card);
        return true;
    }
}

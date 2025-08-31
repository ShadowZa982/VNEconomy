package org.fox.vneconomy.qr;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class QrMapRenderer extends MapRenderer {

    private final BufferedImage qrImage;

    public QrMapRenderer(BufferedImage qrImage) {
        super(true); // ghi đè renderer mặc định
        this.qrImage = resizeImage(qrImage, 128, 128);
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (qrImage != null) {
            canvas.drawImage(0, 0, qrImage);
        }
    }

    public static ItemStack createQrMap(Player player, BufferedImage qrImage) {
        ItemStack map = new ItemStack(Material.FILLED_MAP, 1);
        MapView view = Bukkit.createMap(player.getWorld());
        view.getRenderers().clear();
        view.addRenderer(new QrMapRenderer(qrImage));

        MapMeta meta = (MapMeta) map.getItemMeta();
        if (meta != null) {
            meta.setMapView(view);
            meta.setDisplayName("QR Nạp Tiền");
            map.setItemMeta(meta);
        }
        return map;
    }

    public static BufferedImage fromUrl(String url) {
        try {
            return resizeImage(ImageIO.read(new URL(url)), 128, 128);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BufferedImage fromBase64(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            return resizeImage(ImageIO.read(new ByteArrayInputStream(data)), 128, 128);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BufferedImage createPlaceholderImage(int amount) {
        int w = 128, h = 128;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, w - 1, h - 1);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("SePay QR", 20, 50);
        g.drawString(amount + " VND", 20, 70);
        g.drawString("(placeholder)", 10, 90);
        g.dispose();
        return img;
    }

    public static BufferedImage fromSePay(String account, String bank, int amount, String description, String template) {
        try {
            String url = "https://qr.sepay.vn/img"
                    + "?acc=" + URLEncoder.encode(account, StandardCharsets.UTF_8)
                    + "&bank=" + URLEncoder.encode(bank, StandardCharsets.UTF_8)
                    + "&amount=" + amount
                    + "&des=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                    + "&template=" + (template == null ? "compact" : URLEncoder.encode(template, StandardCharsets.UTF_8));

            return fromUrl(url);
        } catch (Exception e) {
            e.printStackTrace();
            return createPlaceholderImage(amount);
        }
    }

    private static BufferedImage resizeImage(BufferedImage original, int width, int height) {
        if (original == null) return null;
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }
}

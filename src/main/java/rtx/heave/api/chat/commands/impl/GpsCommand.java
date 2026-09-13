package rtx.heave.api.chat.commands.impl;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import rtx.heave.api.chat.commands.Command;
import rtx.heave.api.modules.impl.Utils.EventMarkers;

public final class GpsCommand extends Command {
    public GpsCommand() {
        super("gps", "Устанавливает или сбрасывает GPS метку", new String[]{"waypoint", "point", "wp"});
    }

    @Override
    public void execute(String label, String[] args) {
        if (args.length == 0) {
            EventMarkers.Marker current = EventMarkers.getActiveMarker();
            if (current != null) {
                MinecraftClient mc = MinecraftClient.getInstance();
                double dist = mc.player != null ? mc.player.getEntityPos().distanceTo(current.pos()) : 0.0;
                this.logDirect(String.format(Locale.ROOT, "§6[GPS] §fАктивная метка: §e%s §7на §b%d %d %d §7(§a%.0fм§7). Для сброса: §c.gps off",
                    current.name(), (int) current.pos().x, (int) current.pos().y, (int) current.pos().z, dist));
            } else {
                this.logDirect("§6[GPS] §fНет активной метки.");
                this.usage();
            }
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("off") || sub.equals("clear") || sub.equals("reset") || sub.equals("stop") || sub.equals("del")) {
            EventMarkers.clearAll();
            this.logDirect("§a[GPS] §fМетка успешно сброшена.", Formatting.GREEN);
            return;
        }

        // Quick shortcut: .gps zamok / .gps замок -> 0 63 0
        if (sub.equals("zamok") || sub.equals("замок") || sub.equals("castle")) {
            EventMarkers.setGps("Замок", new Vec3d(0.0, 63.0, 0.0), new Color(255, 60, 60), 2 * 60 * 60 * 1000L, false);
            MinecraftClient mc = MinecraftClient.getInstance();
            double dist = mc.player != null ? mc.player.getEntityPos().distanceTo(new Vec3d(0, 63, 0)) : 0.0;
            this.logDirect(String.format(Locale.ROOT, "§6[GPS] §fМетка §cЗамок §fустановлена на координаты §b0 63 0 §7(дистанция: §a%.0fм§7)", dist), Formatting.GREEN);
            return;
        }


        // 1. Try parsing when the last 3 arguments are numbers: [optional name...] <x> <y> <z>
        if (args.length >= 3) {
            try {
                double x = Double.parseDouble(args[args.length - 3]);
                double y = Double.parseDouble(args[args.length - 2]);
                double z = Double.parseDouble(args[args.length - 1]);
                String name = (args.length > 3) ? String.join(" ", Arrays.copyOfRange(args, 0, args.length - 3)) : "GPS";
                EventMarkers.setGps(name, new Vec3d(x, y, z), new Color(255, 215, 0), 2 * 60 * 60 * 1000L, false);
                MinecraftClient mc = MinecraftClient.getInstance();
                double dist = mc.player != null ? mc.player.getEntityPos().distanceTo(new Vec3d(x, y, z)) : 0.0;
                this.logDirect(String.format(Locale.ROOT, "§6[GPS] §fМетка §e%s §fустановлена на координаты §b%.0f %.0f %.0f §7(дистанция: §a%.0fм§7)", name, x, y, z, dist), Formatting.GREEN);
                return;
            } catch (NumberFormatException ignored) {}
        }

        // 2. Try parsing when the last 2 arguments are numbers: [optional name...] <x> <z> (Y resolved automatically)
        if (args.length >= 2) {
            try {
                double x = Double.parseDouble(args[args.length - 2]);
                double z = Double.parseDouble(args[args.length - 1]);
                double y = EventMarkers.resolveY(x, z);
                String name = (args.length > 2) ? String.join(" ", Arrays.copyOfRange(args, 0, args.length - 2)) : "GPS";
                EventMarkers.setGps(name, new Vec3d(x, y, z), new Color(0, 229, 255), 2 * 60 * 60 * 1000L, false);
                MinecraftClient mc = MinecraftClient.getInstance();
                double dist = mc.player != null ? mc.player.getEntityPos().distanceTo(new Vec3d(x, y, z)) : 0.0;
                this.logDirect(String.format(Locale.ROOT, "§6[GPS] §fМетка §e%s §fустановлена на координаты §b%.0f %.0f %.0f §7(дистанция: §a%.0fм§7)", name, x, y, z, dist), Formatting.GREEN);
                return;
            } catch (NumberFormatException ignored) {}
        }

        this.usage();
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
            "Управление GPS метками ивентов и точек.",
            "Использование:",
            "> .gps zamok             - поставить метку на Замок (0 63 0)",
            "> .gps <x> <z>           - поставить метку на координаты (Y авто)",
            "> .gps <x> <y> <z>       - поставить метку с высотой",
            "> .gps <имя> <x> <z>     - поставить именованную метку",
            "> .gps off / clear       - выключить и сбросить метку",
            "> .gps                   - показать текущую метку и дистанцию"
        );
    }

    @Override
    public Stream<String> tabComplete(String prefix, String[] args) {
        if (args.length == 1) {
            return Stream.of("zamok", "off", "clear")
                .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)));
        }
        return Stream.empty();
    }
}

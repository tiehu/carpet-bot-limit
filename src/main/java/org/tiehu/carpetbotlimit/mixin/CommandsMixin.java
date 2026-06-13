package org.tiehu.carpetbotlimit.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(Commands.class)
public class CommandsMixin {


    @Inject(
        method = "performCommand",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onPerformCommand(
        ParseResults<CommandSourceStack> parse,
        String command,
        CallbackInfo ci
    ) {
        CommandSourceStack source = parse.getContext().getSource();

        if (!source.isPlayer()) return;
        if (Commands.LEVEL_GAMEMASTERS.check(source.permissions())) return;

        String[] parts = command.split("\\s+");
        if (parts.length < 3 || !parts[0].equals("player")) return;

        String targetName = parts[1];
        String action     = parts[2];
        String playerName = source.getPlayer().getScoreboardName();
        String allowedBot = "bot_" + playerName;

        boolean allowed = switch (action) {
            case "spawn"  -> targetName.equals(allowedBot);
            case "shadow" -> targetName.equals(playerName);
            default       -> targetName.equals(playerName) || targetName.equals(allowedBot);
        };

        if (!allowed) {
            String msg = switch (action) {
                case "spawn"  -> "You can only spawn a fake player named " + allowedBot + ".";
                case "shadow" -> "The shadow command can only target yourself (" + playerName + ").";
                default       -> "You can only control yourself or " + allowedBot + ".";
            };
            source.sendFailure(Component.literal(msg));
            ci.cancel();
        }
    }
}
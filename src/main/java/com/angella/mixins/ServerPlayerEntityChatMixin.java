package com.angella.mixins;

import com.angella.AngellaMod;
import com.angella.discord.EmbedBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityChatMixin {
    
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void onSendMessage(Text message, boolean overlay, CallbackInfo ci) {
        // Check if message is about advancement
        String messageText = message.getString().toLowerCase();
        boolean isAdvancementMessage = messageText.contains("получил достижение") || 
                                       messageText.contains("has made the advancement") || 
                                       messageText.contains("completed the challenge") || 
                                       messageText.contains("завершил вызов") ||
                                       messageText.contains("advancement") ||
                                       messageText.contains("достижение");
        
        if (isAdvancementMessage) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            
            if (AngellaMod.getDiscordBot() != null && AngellaMod.getDiscordBot().isReady()) {
                if (AngellaMod.getConfig().sendAdvancements) {
                    // Send advancement message from chat
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            var advancementTracker = player.getAdvancementTracker();
                            var server = player.getServer();
                            if (server != null) {
                                var advancementManager = server.getAdvancementLoader();
                                net.minecraft.advancement.AdvancementEntry foundAdvancement = null;
                                
                                // Find the most recently completed advancement
                                for (var advancement : advancementManager.getAdvancements()) {
                                    if (advancement.value().display().isPresent()) {
                                        var progress = advancementTracker.getProgress(advancement);
                                        if (progress.isDone()) {
                                            String advancementName = advancement.value().display().get().getTitle().getString().toLowerCase();
                                            // Check if message contains advancement name or player name
                                            if (messageText.contains(advancementName) || 
                                                messageText.contains(player.getName().getString().toLowerCase())) {
                                                foundAdvancement = advancement;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                // If we found an advancement, send it
                                if (foundAdvancement != null) {
                                    AngellaMod.LOGGER.info("Sending advancement to Discord: {}", foundAdvancement.id());
                                    EmbedBuilder.createAdvancementEmbed(player, foundAdvancement, message, AngellaMod.getConfig())
                                            .sendToGame();
                                } else {
                                    // Fallback: send message anyway if we can't find specific advancement
                                    AngellaMod.LOGGER.debug("Could not find specific advancement, sending generic message");
                                    // Create a simple embed with the chat message
                                    var builder = EmbedBuilder.create(AngellaMod.getConfig());
                                    builder.setTitle("🏆 Достижение получено!");
                                    builder.setDescription("**" + message.getString() + "**");
                                    builder.setColor(java.awt.Color.decode("#FFD700"));
                                    // Get avatar URL and set as thumbnail
                                    String avatarUrl = com.angella.discord.SkinRestorerIntegration.getPlayerAvatarUrl(player, AngellaMod.getConfig());
                                    builder.setThumbnail(avatarUrl);
                                    builder.sendToGame();
                                }
                            }
                        } catch (Exception e) {
                            AngellaMod.LOGGER.error("Failed to process advancement from chat", e);
                        }
                    });
                }
            }
        }
    }
}


package com.angella.mixins;

import com.angella.AngellaMod;
import com.angella.discord.EmbedBuilder;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.network.ServerAdvancementTracker")
public class ServerAdvancementTrackerMixin {
    
    @Shadow @Final private ServerPlayerEntity owner;
    
    @Inject(method = "grantCriterion", at = @At("TAIL"))
    private void onAdvancementGranted(AdvancementEntry advancement, String criterionName, CallbackInfo ci) {
        // Only send if advancement has a display (visible advancements)
        if (advancement.value().display().isPresent()) {
            // Check if advancement is actually completed (all criteria met)
            try {
                var tracker = (Object)this;
                java.lang.reflect.Method getProgressMethod = tracker.getClass().getMethod("getProgress", net.minecraft.advancement.AdvancementEntry.class);
                var progress = getProgressMethod.invoke(tracker, advancement);
                var isDoneMethod = progress.getClass().getMethod("isDone");
                boolean isDone = (Boolean)isDoneMethod.invoke(progress);
                
                if (isDone) {
                    AngellaMod.LOGGER.info("Advancement completed via tracker: {}", advancement.id());
                    // The chat message will be sent separately, so we'll let ServerPlayerEntityChatMixin handle it
                    // This ensures we get the actual chat message text
                }
            } catch (Exception e) {
                AngellaMod.LOGGER.debug("Failed to check advancement completion: {}", e.getMessage());
            }
        }
    }
}


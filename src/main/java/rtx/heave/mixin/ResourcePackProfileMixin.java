package rtx.heave.mixin;

import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourcePackProfile.class)
public abstract class ResourcePackProfileMixin {

    @Inject(method = "isRequired", at = @At("HEAD"), cancellable = true)
    private void heave_requireCustomSwords(CallbackInfoReturnable<Boolean> cir) {
        ResourcePackProfile self = (ResourcePackProfile) (Object) this;
        if (self.getId() != null && self.getId().contains("heave_custom_swords")) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPinned", at = @At("HEAD"), cancellable = true)
    private void heave_pinCustomSwords(CallbackInfoReturnable<Boolean> cir) {
        ResourcePackProfile self = (ResourcePackProfile) (Object) this;
        if (self.getId() != null && self.getId().contains("heave_custom_swords")) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getCompatibility", at = @At("HEAD"), cancellable = true)
    private void heave_customSwordsCompatible(CallbackInfoReturnable<ResourcePackCompatibility> cir) {
        ResourcePackProfile self = (ResourcePackProfile) (Object) this;
        if (self.getId() != null && self.getId().contains("heave_custom_swords")) {
            cir.setReturnValue(ResourcePackCompatibility.COMPATIBLE);
        }
    }

    @Inject(method = "getInitialPosition", at = @At("HEAD"), cancellable = true)
    private void heave_customSwordsPosition(CallbackInfoReturnable<ResourcePackProfile.InsertionPosition> cir) {
        ResourcePackProfile self = (ResourcePackProfile) (Object) this;
        if (self.getId() != null && self.getId().contains("heave_custom_swords")) {
            cir.setReturnValue(ResourcePackProfile.InsertionPosition.TOP);
        }
    }
}

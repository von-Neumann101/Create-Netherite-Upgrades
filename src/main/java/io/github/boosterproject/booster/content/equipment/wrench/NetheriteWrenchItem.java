package io.github.boosterproject.booster.content.equipment.wrench;

import com.simibubi.create.AllItems;
import io.github.boosterproject.booster.Booster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;

public final class NetheriteWrenchItem extends Item {
    public NetheriteWrenchItem(Properties properties) {
        super(properties.fireResistant().stacksTo(1).attributes(ItemAttributeModifiers.builder()
            .add(Attributes.BLOCK_INTERACTION_RANGE,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Booster.MOD_ID, "netherite_wrench_reach"),
                    2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return AllItems.WRENCH.get().useOn(context);
    }
}

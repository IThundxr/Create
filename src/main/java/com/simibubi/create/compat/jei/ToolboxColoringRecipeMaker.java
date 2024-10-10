package com.simibubi.create.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;

public final class ToolboxColoringRecipeMaker {

	// From JEI's ShulkerBoxColoringRecipeMaker
	public static List<CraftingRecipe> createRecipes() {
		String group = "create.toolbox.color";
		ItemStack baseShulkerStack = AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
			.asStack();
		Ingredient baseShulkerIngredient = Ingredient.of(baseShulkerStack);

		List<CraftingRecipe> recipes = new ArrayList<>();

		for (DyeColor color : DyeColor.values()) {
			if (color != DyeColor.BROWN) {
				DyeItem dye = DyeItem.byColor(color);
				ItemStack dyeStack = new ItemStack(dye);
				TagKey<Item> colorTag = color.getTag();
				Ingredient.Value dyeList = new Ingredient.ItemValue(dyeStack);
				Ingredient.Value colorList = new Ingredient.TagValue(colorTag);
				Stream<Ingredient.Value> colorIngredientStream = Stream.of(dyeList, colorList);
				Ingredient colorIngredient = Ingredient.fromValues(colorIngredientStream);
				NonNullList<Ingredient> inputs =
						NonNullList.of(Ingredient.EMPTY, baseShulkerIngredient, colorIngredient);
				Block coloredShulkerBox = AllBlocks.TOOLBOXES.get(color)
						.get();
				ItemStack output = new ItemStack(coloredShulkerBox);
				ResourceLocation id = Create.asResource(group + "." + output.getDescriptionId());
				recipes.add(new ShapelessRecipe(id, group, CraftingBookCategory.MISC, output, inputs));
			}
		}

		return recipes;
	}

	private ToolboxColoringRecipeMaker() {}

}

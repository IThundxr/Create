package com.simibubi.create.content.processing.basin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BasinOperatingBlockEntity extends KineticBlockEntity {

	public DeferralBehaviour basinChecker;
	public boolean basinRemoved;
	protected Recipe<?> currentRecipe;

	public BasinOperatingBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		basinChecker = new DeferralBehaviour(this, this::updateBasin);
		behaviours.add(basinChecker);
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		if (getSpeed() == 0)
			basinRemoved = true;
		basinRemoved = false;
		basinChecker.scheduleUpdate();
	}

	@Override
	public void tick() {
		if (basinRemoved) {
			basinRemoved = false;
			onBasinRemoved();
			sendData();
			return;
		}

		super.tick();
	}

	protected boolean updateBasin() {
		if (!isSpeedRequirementFulfilled())
			return true;
		if (getSpeed() == 0)
			return true;
		if (isRunning())
			return true;
		if (level == null || level.isClientSide)
			return true;
		Optional<BasinBlockEntity> basin = getBasin();
		if (!basin.filter(BasinBlockEntity::canContinueProcessing)
			.isPresent())
			return true;

		List<Recipe<?>> recipes = getMatchingRecipes();
		if (recipes.isEmpty())
			return true;
		currentRecipe = recipes.get(0);
		startProcessingBasin();
		sendData();
		return true;
	}

	protected abstract boolean isRunning();

	public void startProcessingBasin() {}

	public boolean continueWithPreviousRecipe() {
		return true;
	}

	protected <C extends Container> boolean matchBasinRecipe(Recipe<C> recipe) {
		if (recipe == null)
			return false;
		Optional<BasinBlockEntity> basin = getBasin();
		return basin.filter(basinBlockEntity -> BasinRecipe.match(basinBlockEntity, recipe)).isPresent();
	}

	protected void applyBasinRecipe() {
		if (currentRecipe == null)
			return;

		Optional<BasinBlockEntity> optionalBasin = getBasin();
		if (optionalBasin.isEmpty())
			return;
		BasinBlockEntity basin = optionalBasin.get();
		boolean wasEmpty = basin.canContinueProcessing();
		if (!BasinRecipe.apply(basin, currentRecipe))
			return;
		getProcessedRecipeTrigger().ifPresent(this::award);
		basin.inputTank.sendDataImmediately();

		// Continue mixing
		if (wasEmpty && matchBasinRecipe(currentRecipe)) {
			continueWithPreviousRecipe();
			sendData();
		}

		basin.notifyChangeOfContents();
	}

	protected List<Recipe<?>> getMatchingRecipes() {
		if (getBasin().map(BasinBlockEntity::isEmpty)
			.orElse(true))
			return new ArrayList<>();

		List<Recipe<?>> filteredRecipes = new ArrayList<>();

		for (Recipe<?> recipe : RecipeFinder.get(getRecipeCacheKey(), level, this::matchStaticFilters)) {
			if (matchBasinRecipe(recipe)) {
				filteredRecipes.add(recipe);
			}
		}

		filteredRecipes.sort(Comparator.comparingInt(recipe -> recipe.getIngredients().size()));

		return filteredRecipes;
	}

	protected abstract void onBasinRemoved();

	protected Optional<BasinBlockEntity> getBasin() {
		if (level == null)
			return Optional.empty();
		BlockEntity basinBE = level.getBlockEntity(worldPosition.below(2));
		if (!(basinBE instanceof BasinBlockEntity))
			return Optional.empty();
		return Optional.of((BasinBlockEntity) basinBE);
	}

	protected Optional<CreateAdvancement> getProcessedRecipeTrigger() {
		return Optional.empty();
	}

	protected abstract <C extends Container> boolean matchStaticFilters(Recipe<C> recipe);

	protected abstract Object getRecipeCacheKey();
}

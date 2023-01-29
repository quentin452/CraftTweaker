package minetweaker.api.vanilla;

import java.util.List;

import minetweaker.api.item.*;
import stanhebben.zenscript.annotations.*;

/**
 * Created by Jared on 5/9/2016.
 */
public interface IVillagerRegistry {

    @ZenMethod
    void addSeed(WeightedItemStack item);

    @ZenMethod
    void removeSeed(IIngredient item);

    @ZenGetter("seeds")
    List<WeightedItemStack> getSeeds();
}

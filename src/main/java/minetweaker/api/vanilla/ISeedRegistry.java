package minetweaker.api.vanilla;

import java.util.List;

import minetweaker.api.item.*;
import stanhebben.zenscript.annotations.*;

/**
 * @author Stan
 */
@ZenClass("vanilla.ISeedRegistry")
public interface ISeedRegistry {

    @ZenMethod
    void addSeed(WeightedItemStack item);

    @ZenMethod
    void removeSeed(IIngredient item);

    @ZenGetter("seeds")
    List<WeightedItemStack> getSeeds();
}

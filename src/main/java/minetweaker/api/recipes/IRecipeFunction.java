package minetweaker.api.recipes;

import java.util.Map;

import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

/**
 * @author Stan
 */
@ZenClass("minetweaker.recipes.IRecipeFunction")
public interface IRecipeFunction {

    IItemStack process(IItemStack output, Map<String, IItemStack> inputs, ICraftingInfo craftingInfo);
}

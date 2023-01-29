package minetweaker.api.block;

import java.util.List;

import stanhebben.zenscript.annotations.*;

/**
 * @author Stan
 */
@ZenClass("minetweaker.block.IBlockPattern")
public interface IBlockPattern {

    @ZenMethod("blocks")
    List<IBlock> getBlocks();

    @ZenOperator(OperatorType.CONTAINS)
    boolean matches(IBlock block);

    @ZenOperator(OperatorType.OR)
    IBlockPattern or(IBlockPattern pattern);

    @ZenGetter("displayName")
    String getDisplayName();

}

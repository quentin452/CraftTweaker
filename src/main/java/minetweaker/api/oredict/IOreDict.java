package minetweaker.api.oredict;

import java.util.List;

import stanhebben.zenscript.annotations.*;

/**
 * @author Stan
 */
@ZenClass("minetweaker.oredict.IOreDict")
public interface IOreDict {

    @ZenMemberGetter
    IOreDictEntry get(String name);

    @ZenGetter("entries")
    List<IOreDictEntry> getEntries();
}

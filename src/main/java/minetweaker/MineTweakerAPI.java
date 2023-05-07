package minetweaker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.common.registry.GameRegistry;
import minetweaker.annotations.BracketHandler;
import minetweaker.annotations.ModOnly;
import minetweaker.api.client.IClient;
import minetweaker.api.compat.*;
import minetweaker.api.event.IEventManager;
import minetweaker.api.formatting.IFormatter;
import minetweaker.api.game.IGame;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IngredientItem;
import minetweaker.api.mods.ILoadedMods;
import minetweaker.api.oredict.IOreDict;
import minetweaker.api.oredict.IOreDictEntry;
import minetweaker.api.oredict.IngredientOreDict;
import minetweaker.api.recipes.IFurnaceManager;
import minetweaker.api.recipes.IRecipeManager;
import minetweaker.api.server.IServer;
import minetweaker.api.vanilla.IVanilla;
import minetweaker.runtime.GlobalRegistry;
import minetweaker.runtime.ILogger;
import minetweaker.runtime.ITweaker;
import minetweaker.runtime.MTTweaker;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.symbols.IZenSymbol;
import stanhebben.zenscript.symbols.SymbolJavaStaticField;
import stanhebben.zenscript.symbols.SymbolJavaStaticGetter;
import stanhebben.zenscript.symbols.SymbolJavaStaticMethod;
import stanhebben.zenscript.type.natives.IJavaMethod;
import stanhebben.zenscript.type.natives.JavaMethod;

/**
 * Provides access to the MineTweaker API.
 *
 * An implementing platform needs to do the following: - Set a logger - Set the ore dictionary - Set the recipe manager
 * - Set the furnace manager - Set event manager - Set resource manager
 *
 * - Register additional global symbols to the GlobalRegistry (recipes, minetweaker, oreDict, logger, as well as the
 * official set of functions) - Register native classes using the GlobalRegistry - Register bracket handlers to resolve
 * block/item/... references using the bracket syntax
 *
 * @author Stan Hebben
 */
public class MineTweakerAPI {

    private static final org.apache.logging.log4j.Logger TRANSLATORLOGGER = LogManager
            .getLogger("[SCRIPTS TO CODE TRANSLATOR]");

    public static void info(String log) {
        TRANSLATORLOGGER.info(log);
    }

    public static String convertStack(ItemStack stack) {
        if (stack == null) return "null";
        GameRegistry.UniqueIdentifier itemIdentifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        int meta = stack.getItemDamage();
        int size = stack.stackSize;
        NBTTagCompound tagCompound = stack.stackTagCompound;
        if (tagCompound == null || tagCompound.hasNoTags()) {
            return "getModItem(\"" + itemIdentifier.modId
                    + "\", \""
                    + itemIdentifier.name
                    + "\", "
                    + size
                    + ", "
                    + meta
                    + ", missing)";
        } else {
            return "createItemStack(\"" + itemIdentifier.modId
                    + "\", \""
                    + itemIdentifier.name
                    + "\", "
                    + size
                    + ", "
                    + meta
                    + ", "
                    + "\""
                    + tagCompound.toString().replace("\"", "\\\"")
                    + "\""
                    + ", missing)";
        }
    }

    public static String convertStack(IIngredient ingredient) {
        Object internal = ingredient.getInternal();
        if (internal instanceof ItemStack) return convertStack((ItemStack) internal);
        else return "ERRORSTACK";
    }

    public static String convertStackOrOre(IIngredient ingredient) {
        Object internal = ingredient.getInternal();
        if (internal instanceof ItemStack) return convertStack((ItemStack) internal);
        else if (internal instanceof String) return (String) internal;
        else return "ERRORSTACK";
    }

    public static String convertArrayInLine(Object[] arr) {
        StringBuilder arrayString = new StringBuilder();
        for (int i = 0, arrLength = arr.length; i < arrLength; i++) {
            Object o = arr[i];
            if (o instanceof String) arrayString.append("\"").append((String) o).append("\"");
            else if (o instanceof Character) arrayString.append("'").append((char) o).append("'");
            else if (o instanceof ItemStack) arrayString.append(convertStack((ItemStack) o));
            else if (o instanceof IItemStack || o instanceof IngredientItem)
                arrayString.append(convertStack((IIngredient) o));
            else if (o instanceof IOreDictEntry)
                arrayString.append("\"").append((String) ((IOreDictEntry) o).getInternal()).append("\"");
            else if (o instanceof IngredientOreDict)
                arrayString.append("\"").append((String) ((IngredientOreDict) o).getInternal()).append("\"");
            else if (o == null) arrayString.append("null");
            else arrayString.append(o);
            if (i + 1 < arrLength) arrayString.append(", ");
        }
        return arrayString.toString();
    }

    public static String convert2DArrayInLine(Object[][] arr) {
        StringBuilder arrayString = new StringBuilder();
        for (int i = 0, arrLength = arr.length; i < arrLength; i++) {
            for (int j = 0, jarrLength = arr[i].length; j < jarrLength; j++) {
                Object o = arr[i][j];
                if (o instanceof String) arrayString.append("\"").append((String) o).append("\"");
                else if (o instanceof Character) arrayString.append("'").append((char) o).append("'");
                else if (o instanceof ItemStack) arrayString.append(convertStack((ItemStack) o));
                else if (o instanceof IItemStack || o instanceof IngredientItem)
                    arrayString.append(convertStack((IIngredient) o));
                else if (o instanceof IOreDictEntry)
                    arrayString.append("\"").append((String) ((IOreDictEntry) o).getInternal()).append("\"");
                else if (o instanceof IngredientOreDict)
                    arrayString.append("\"").append((String) ((IngredientOreDict) o).getInternal()).append("\"");
                else if (o == null) arrayString.append("null");
                else arrayString.append(o);
                if (i + 1 < arrLength || j + 1 < jarrLength) arrayString.append(", ");
            }
        }
        return arrayString.toString();
    }

    public static void logGTRecipe(ItemStack[] itemInputs, ItemStack[] itemOutputs, int[] chances,
            FluidStack[] fluidInputs, FluidStack[] fluidOutputs, int duration, int eut, Integer specialValue,
            String recipeMapVariable) {
        StringBuilder builder = new StringBuilder("GT_Values.RA.stdBuilder()");
        Function<ItemStack[], ItemStack[]> isAllObjectsNullItemStack = arr -> {
            if (arr == null || arr.length == 0 || Arrays.stream(arr).allMatch(Objects::isNull)) return null;
            return Arrays.stream(arr).filter(Objects::nonNull).toArray(ItemStack[]::new);
        };
        Function<FluidStack[], FluidStack[]> isAllObjectsNullFluidStack = arr -> {
            if (arr == null || arr.length == 0 || Arrays.stream(arr).allMatch(Objects::isNull)) return null;
            return Arrays.stream(arr).filter(Objects::nonNull).toArray(FluidStack[]::new);
        };
        itemInputs = isAllObjectsNullItemStack.apply(itemInputs);
        itemOutputs = isAllObjectsNullItemStack.apply(itemOutputs);
        fluidInputs = isAllObjectsNullFluidStack.apply(fluidInputs);
        fluidOutputs = isAllObjectsNullFluidStack.apply(fluidOutputs);
        if (itemInputs == null || itemInputs.length == 0) builder.append(".noItemInputs()");
        else builder.append(".itemInputs(").append(convertArrayInLine(itemInputs)).append(")");
        if (itemOutputs == null || itemOutputs.length == 0) builder.append(".noItemOutputs()");
        else builder.append(".itemOutputs(").append(convertArrayInLine(itemOutputs)).append(")");
        if (chances != null && chances.length > 0) builder.append(".outputChances(")
                .append(convertArrayInLine(Arrays.stream(chances).boxed().toArray(Integer[]::new))).append(")");
        if (fluidInputs == null || fluidInputs.length == 0) builder.append(".noFluidInputs()");
        else builder.append(".fluidInputs(").append(convertArrayInLine(fluidInputs)).append(")");
        if (fluidOutputs == null || fluidOutputs.length == 0) builder.append(".noFluidOutputs()");
        else builder.append(".fluidOutputs(").append(convertArrayInLine(fluidOutputs)).append(")");
        builder.append(".duration(").append(duration).append(")");
        builder.append(".eut(").append(eut).append(")");
        if (specialValue != null) builder.append(".specialValue(").append(specialValue.intValue()).append(")");
        builder.append(".addTo(").append(recipeMapVariable).append(");");

        info(builder.toString());
    }

    public static void logGTRecipe(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs,
            FluidStack[] fluidOutputs, int duration, int eut, String recipeMapVariable) {
        logGTRecipe(itemInputs, itemOutputs, null, fluidInputs, fluidOutputs, duration, eut, null, recipeMapVariable);
    }

    public static void logGTRecipe(ItemStack[] itemInputs, ItemStack[] itemOutputs, int[] chances,
            FluidStack[] fluidInputs, FluidStack[] fluidOutputs, int duration, int eut, String recipeMapVariable) {
        logGTRecipe(
                itemInputs,
                itemOutputs,
                chances,
                fluidInputs,
                fluidOutputs,
                duration,
                eut,
                null,
                recipeMapVariable);
    }

    public static void logGTRecipe(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs,
            FluidStack[] fluidOutputs, int duration, int eut, int specialValue, String recipeMapVariable) {
        logGTRecipe(
                itemInputs,
                itemOutputs,
                null,
                fluidInputs,
                fluidOutputs,
                duration,
                eut,
                specialValue,
                recipeMapVariable);
    }

    public static final String[] COLOR_NAMES = { "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink",
            "Gray", "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black" };
    private static IJEIRecipeRegistry ijeiRecipeRegistry = new DummyJEIRecipeRegistry();

    static {
        registerClassRegistry(ClassRegistry.class, "MT-API");

        registerGlobalSymbol("logger", getJavaStaticGetterSymbol(MineTweakerAPI.class, "getLogger"));
        registerGlobalSymbol("recipes", getJavaStaticFieldSymbol(MineTweakerAPI.class, "recipes"));
        // registerGlobalSymbol("brewing", getJavaStaticFieldSymbol(MineTweakerAPI.class, "brewing"));
        registerGlobalSymbol("furnace", getJavaStaticFieldSymbol(MineTweakerAPI.class, "furnace"));
        registerGlobalSymbol("oreDict", getJavaStaticFieldSymbol(MineTweakerAPI.class, "oreDict"));
        registerGlobalSymbol("events", getJavaStaticFieldSymbol(MineTweakerAPI.class, "events"));
        registerGlobalSymbol("server", getJavaStaticFieldSymbol(MineTweakerAPI.class, "server"));
        registerGlobalSymbol("client", getJavaStaticFieldSymbol(MineTweakerAPI.class, "client"));
        registerGlobalSymbol("game", getJavaStaticFieldSymbol(MineTweakerAPI.class, "game"));
        registerGlobalSymbol("loadedMods", getJavaStaticFieldSymbol(MineTweakerAPI.class, "loadedMods"));
        registerGlobalSymbol("format", getJavaStaticFieldSymbol(MineTweakerAPI.class, "format"));
        registerGlobalSymbol("vanilla", getJavaStaticFieldSymbol(MineTweakerAPI.class, "vanilla"));
    }

    private MineTweakerAPI() {

    }

    /**
     * The Tweaker is where you apply undoable actions. Any kind of action that reloads with the scripts should always
     * be submitted to the tweaker.
     */
    @Deprecated
    public static final ITweaker tweaker = new MTTweaker();

    /**
     * The logger can be used to write logging messages to the client. Error and warning messages should be relayed to
     * admins for further handling.
     *
     * @return
     */
    public static final ILogger getLogger() {
        return MineTweakerImplementationAPI.logger;
    }

    /**
     * Access point to the ore dictionary.
     */
    public static IOreDict oreDict = null;

    /**
     * Access point to the recipe manager.
     */
    public static IRecipeManager recipes = null;

    // /**
    // * Access point to the brewing manager.
    // */
    // public static IBrewingManager brewing = null;

    /**
     * Access point to the furnace manager.
     */
    public static IFurnaceManager furnace = null;

    /**
     * Access point to the events manager.
     */
    public static final IEventManager events = MineTweakerImplementationAPI.events;

    /**
     * Access point to the server, if any.
     */
    public static IServer server = null;

    /**
     * Access point to the client, if any.
     */
    public static IClient client = null;

    /**
     * Access point to general game data, such as items.
     */
    public static IGame game = null;

    /**
     * Access point to mods list.
     */
    public static ILoadedMods loadedMods = null;

    /**
     * Access point to the text formatter.
     */
    public static IFormatter format = null;

    /**
     * Access point to the vanilla functions and data.
     */
    public static IVanilla vanilla = null;

    /**
     * Applies this given action.
     *
     * @param action action object
     */
    public static void apply(IUndoableAction action) {
        tweaker.apply(action);
    }

    /**
     * Logs a command message. Commands messages are those generated as output in response to a command.
     *
     * @param message command message
     */
    public static void logCommand(String message) {
        getLogger().logCommand(message);
    }

    /**
     * Logs an info message. Info messages have low priority and will only be displayed in the log files, but not
     * directly to players in-game.
     *
     * @param message info message
     */
    public static void logInfo(String message) {
        getLogger().logInfo(message);
    }

    /**
     * Logs a warning message. Warning messages are displayed to admins and indicate that there is an issue. However,
     * the issue is not a large problem, and everything should run fine - besides perhaps a few things not entirely
     * working as expected.
     *
     * @param message warning message
     */
    public static void logWarning(String message) {
        getLogger().logWarning(message);
    }

    /**
     * Logs an error message. Error messages indicate a real problem and indicate that things won't run properly. The
     * scripting system will still make a best-effort attempt at executing the rest of the scripts, but that might cause
     * additional errors and issues.
     *
     * @param message error message
     */
    public static void logError(String message) {
        getLogger().logError(message);
    }

    /**
     * Logs an error message. Error messages indicate a real problem and indicate that things won't run properly. The
     * scripting system will still make a best-effort attempt at executing the rest of the scripts, but that might cause
     * additional errors and issues.
     *
     * @param message   error message
     * @param exception exception that was caught related to the error
     */
    public static void logError(String message, Throwable exception) {
        getLogger().logError(message, exception);
    }

    // ###################################
    // ### Plugin registration methods ###
    // ###################################

    /**
     * Register a class registry class. Such class must have (at least) a public static method called "getClasses" with
     * accepts a List of classes and which stores its classes into that list.
     *
     * @param registryClass
     */
    public static void registerClassRegistry(Class registryClass) {
        registerClassRegistry(registryClass, null);
    }

    /**
     * Register a class registry class. Such class must have (at least) a public static method called "getClasses" with
     * accepts a List of classes and which stores its classes into that list.
     *
     * @param registryClass
     * @param description
     */
    public static void registerClassRegistry(Class registryClass, String description) {
        try {
            Method method = registryClass.getMethod("getClasses", List.class);
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                logError("ERROR: getClasses method in " + registryClass.getName() + " isn't static");
            } else {
                List<Class> classes = new ArrayList<Class>();
                method.invoke(null, classes);

                outer: for (Class cls : classes) {
                    for (Annotation annotation : cls.getAnnotations()) {
                        if (annotation instanceof ModOnly) {
                            String[] value = ((ModOnly) annotation).value();
                            String version = ((ModOnly) annotation).version();

                            for (String mod : value) {
                                if (!loadedMods.contains(mod)) continue outer;

                                if (!loadedMods.get(mod).getVersion().startsWith(version)) continue outer;
                            }
                        }
                    }

                    MineTweakerAPI.registerClass(cls);
                }

                if (description != null) logInfo("Loaded class registry: " + description);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            if (ex.getCause() instanceof NoClassDefFoundError) {
                logInfo("Classes for registry " + registryClass + " not found, skipping");
            } else {
                logError("Error registering class registry", ex);
            }
        }
    }

    /**
     * Registers a class registry. Will attempt to resolve the given class name. Does nothing if the class could not be
     * loaded.
     *
     * @param className class name to be loaded
     * @return true if registration was successful
     */
    public static boolean registerClassRegistry(String className) {
        return registerClassRegistry(className, null);
    }

    /**
     * Registers a class registry. Will attempt to resolve the given class name. Does nothing if the class could not be
     * loaded.
     *
     * @param className   class name to be loaded
     * @param description
     * @return true if registration was successful
     */
    public static boolean registerClassRegistry(String className, String description) {
        try {
            registerClassRegistry(Class.forName(className), description);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Registers an annotated class. A class is annotated with either @ZenClass or @ZenExpansion. Classes not annotated
     * with either of these will be ignored.
     *
     * @param annotatedClass
     */
    public static void registerClass(Class annotatedClass) {
        // System.out.println("Registering " + annotatedClass.getName());

        for (Annotation annotation : annotatedClass.getAnnotations()) {
            if (annotation instanceof ZenExpansion) {
                GlobalRegistry.registerExpansion(annotatedClass);
            }

            if (annotation instanceof ZenClass) {
                GlobalRegistry.registerNativeClass(annotatedClass);
            }
            if ((annotation instanceof BracketHandler) && IBracketHandler.class.isAssignableFrom(annotatedClass)) {
                try {
                    IBracketHandler bracketHandler = (IBracketHandler) annotatedClass.newInstance();
                    registerBracketHandler(bracketHandler);
                } catch (InstantiationException ex) {
                    Logger.getLogger(MineTweakerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(MineTweakerAPI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Registers a global symbol. Global symbols are immediately accessible from anywhere in the scripts.
     *
     * @param name   symbol name
     * @param symbol symbol
     */
    public static void registerGlobalSymbol(String name, IZenSymbol symbol) {
        GlobalRegistry.registerGlobal(name, symbol);
    }

    /**
     * Registers a recipe remover. Removers are called when the global minetweaker.remove() function is called.
     *
     * @param remover recipe remover
     */
    public static void registerRemover(IRecipeRemover remover) {
        GlobalRegistry.registerRemover(remover);
    }

    /**
     * Registers a bracket handler. Is capable of converting the bracket syntax to an actual value. This new handler
     * will be added last - it can thus not intercept values that are already handled by the system.
     *
     * @param handler bracket handler to be added
     */
    public static void registerBracketHandler(IBracketHandler handler) {
        GlobalRegistry.registerBracketHandler(handler);
    }

    /**
     * Creates a symbol that refers to a java method.
     *
     * @param cls       class that contains the method
     * @param name      method name
     * @param arguments method argument types
     * @return corresponding symbol
     */
    public static IZenSymbol getJavaStaticMethodSymbol(Class cls, String name, Class... arguments) {
        IJavaMethod method = JavaMethod.get(GlobalRegistry.getTypeRegistry(), cls, name, arguments);
        return new SymbolJavaStaticMethod(method);
    }

    /**
     * Creates a symbol that refers to a java getter. The getter must be a method with no arguments. The given symbol
     * will act as a variable of which the value can be retrieved but not set.
     *
     * @param cls  class that contains the getter method
     * @param name name of the method
     * @return corresponding symbol
     */
    public static IZenSymbol getJavaStaticGetterSymbol(Class cls, String name) {
        IJavaMethod method = JavaMethod.get(GlobalRegistry.getTypeRegistry(), cls, name);
        return new SymbolJavaStaticGetter(method);
    }

    /**
     * Creates a symbol that refers to a static field. The field must be an existing public field in the given class.
     * The field will act as a variable that can be retrieved but not set.
     *
     * @param cls  class that contains the field
     * @param name field name (must be public)
     * @return corresponding symbol
     */
    public static IZenSymbol getJavaStaticFieldSymbol(Class cls, String name) {
        try {
            Field field = cls.getField(name);
            return new SymbolJavaStaticField(cls, field, GlobalRegistry.getTypeRegistry());
        } catch (NoSuchFieldException ex) {
            return null;
        } catch (SecurityException ex) {
            return null;
        }
    }

    /**
     * Loads a Java method from an existing class.
     *
     * @param cls       method class
     * @param name      method name
     * @param arguments argument types
     * @return java method
     */
    public static IJavaMethod getJavaMethod(Class cls, String name, Class... arguments) {
        return JavaMethod.get(GlobalRegistry.getTypeRegistry(), cls, name, arguments);
    }

    public static IJEIRecipeRegistry getIjeiRecipeRegistry() {
        return ijeiRecipeRegistry;
    }

    public static void setIjeiRecipeRegistry(IJEIRecipeRegistry ijeiRecipeRegistry) {
        MineTweakerAPI.ijeiRecipeRegistry = ijeiRecipeRegistry;
    }
}

package me.wonk2.Utilities;

//import eu.endercentral.crazy_advancements.JSONMessage;
//import eu.endercentral.crazy_advancements.NameKey;
//import eu.endercentral.crazy_advancements.advancement.Advancement;
//import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
//import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
//import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
//import eu.endercentral.crazy_advancements.manager.AdvancementManager;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.wonk2.DFPlugin;
import me.wonk2.Utilities.Internals.Enums.DFType;
import me.wonk2.Utilities.Internals.FileManager;
import me.wonk2.Utilities.Internals.PlayerData;
import me.wonk2.Utilities.Internals.Values.DFValue;
import me.wonk2.Utilities.Internals.Values.DFSound;
import me.wonk2.Utilities.Internals.Values.DFVar;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DFUtilities implements Listener {
    public static FileManager managerClass;
    public static void invokeAction(Object[] inputArray, String action, LivingEntity[] targets, HashMap<String, DFValue> localStorage){

        //TODO: Surround with try/catch, figure out which error to catch first (NullPointerException/NullDFValueException/etc)
        HashMap<String, DFValue> args = (HashMap<String, DFValue>) inputArray[0];
        HashMap<String, String> tags = (HashMap<String, String>) inputArray[1];

        HashMap<String, SoundCategory> categories = new HashMap<>(){{
            put("Master", SoundCategory.MASTER);
            put("Music", SoundCategory.MUSIC);
            put("Jukebox/Note Blocks", SoundCategory.RECORDS);
            put("Weather", SoundCategory.WEATHER);
            put("Blocks", SoundCategory.BLOCKS);
            put("Hostile Creatures", SoundCategory.HOSTILE);
            put("Friendly Creatures", SoundCategory.NEUTRAL);
            put("Players", SoundCategory.PLAYERS);
            put("Ambient/Environment", SoundCategory.AMBIENT);
            put("Voice/Speech", SoundCategory.VOICE);
        }};

        for(LivingEntity target : targets)
            switch(action){
                case "SendMessage": {
                    String[] txtArray = DFValue.castTxt((DFValue[]) args.get("msg").getVal());

                    String msg = tags.get("Text Value Merging").equals("Add spaces") ?
                        String.join(" ", txtArray) :
                        String.join("", txtArray);

                    target.sendMessage(msg);
                    break;
                }
                case "PlaySound": {
                    DFSound[] soundArr = DFValue.castSound((DFValue[]) args.get("sounds").getVal());

                    for(DFSound sound : soundArr){
                        if(!args.containsKey("playbackLoc"))
                            ((Player) target).playSound(target.getLocation(), sound.sound, SoundCategory.MASTER, sound.volume, sound.pitch);
                        else ((Player) target).playSound((Location) args.get("playbackLoc").getVal(), sound.sound, categories.get(tags.get("Sound Source")), sound.volume, sound.pitch);
                    }
                    break;
                }

                case "SendTitle":{
                    ((Player) target).sendTitle((String) args.get("title").getVal(), (String) args.get("subtitle").getVal(), args.get("fadeIn").getInt(), args.get("duration").getInt(), args.get("fadeOut").getInt());
                    break;
                }

                case "SetBossBar":{
                    HashMap<String, BarStyle> barStyles = new HashMap<>(){{
                        put("Solid", BarStyle.SOLID);
                        put("6 segments", BarStyle.SEGMENTED_6);
                        put("10 segments", BarStyle.SEGMENTED_10);
                        put("12 segments", BarStyle.SEGMENTED_12);
                        put("20 segments", BarStyle.SEGMENTED_20);
                    }};

                    BarColor color = BarColor.valueOf(tags.get("Bar Color").toUpperCase());
                    double barHealth = (Float) args.get("health").getVal() / (Float) args.get("maxHealth").getVal();;
                    ArrayList<BarFlag> barFlags = new ArrayList<>();
                    if(tags.get("Sky Effect") == "Both" || tags.get("Sky Effect") == "Create fog")
                        barFlags.add(BarFlag.CREATE_FOG);
                    if(tags.get("Sky Effect") == "Both" || tags.get("Sky Effect") == "Darken sky")
                        barFlags.add(BarFlag.DARKEN_SKY);

                    setBossBar((Player) target, (String) args.get("title").getVal(), args.get("index").getInt(), barHealth, color, barStyles.get(tags.get("Bar Style")), barFlags.toArray(new BarFlag[0]));
                    break;
                }

                case "RemoveBossBar": {
                    removeBossbar((Player) target, args.get("index").getInt());
                    break;
                }

                case "ActionBar":{
                    String[] txtArray = DFValue.castTxt((DFValue[]) args.get("msg").getVal());

                    String msg = tags.get("Text Value Merging").equals("Add spaces") ?
                        String.join(" ", txtArray) :
                        String.join("", txtArray);

                    ((Player) target).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
                    break;
                }

                case "SendMessageSeq":{
                    sendMessageSeq((Player) target, DFValue.castTxt((DFValue[]) args.get("msgs").getVal()), (long) args.get("delay").getVal(), DFPlugin.plugin);
                    break;
                }

                case "PlaySoundSeq":{
                    Location loc = args.containsKey("loc") ?
                        (Location) args.get("loc").getVal() :
                        null;
                    playSoundSeq((Player[]) targets, DFValue.castSound((DFValue[]) args.get("sounds").getVal()), loc, (long) args.get("delay").getInt(), DFPlugin.plugin);
                    break;
                }

                case "SendHover": {
                    sendHover((Player) target, (String) args.get("msg").getVal(), (String) args.get("hover").getVal());
                    break;
                }

                case "StopSound": {
                    stopSounds((Player) target, DFValue.castSound((DFValue[]) args.get("sounds").getVal()), categories.get(tags.get("Sound Source")));
                    break;
                }

                case "SetTabListInfo": {
                    if(tags.get("Player List Field") == "Header") ((Player) target).setPlayerListHeader((String) args.get("tabInfo").getVal());
                    else ((Player) target).setPlayerListFooter((String) args.get("tabInfo").getVal());
                    break;
                }

                case "GiveItems": {
                    for(ItemStack item : DFValue.castItem((DFValue[]) args.get("items").getVal()))
                        for(int i = 0; i < args.get("stack").getInt(); i++)
                            ((Player) target).getInventory().addItem(item);
                    break;
                }

                case "SetHotbar": {
                    DFValue[] items = (DFValue[]) args.get("items").getVal();
                    for(int i = 0; i < 9; i++){
                        if(items[i].slot != i) ((Player) target).getInventory().clear(i);
                        else ((Player) target).getInventory().setItem(i, (ItemStack) items[i].getVal());
                    }
                    break;
                }

                case "SetInventory": {
                    DFValue[] items = (DFValue[]) args.get("items").getVal();
                    for(int i = 9; i < 36; i++)
                        if(items[i].slot == i)
                            ((Player) target).getInventory().setItem(items[i].slot, (ItemStack) items[i].getVal());
                        else ((Player) target).getInventory().clear(i);
                    break;
                }

                case "SetSlotItem": {
                    ((Player) target).getInventory().setItem(args.get("slot").getInt() - 1, (ItemStack) args.get("item").getVal());
                    break;
                }

                case "SetEquipment": {
                    HashMap<String, EquipmentSlot> equipmentSlots = new HashMap<>(){{
                        put("Main hand", EquipmentSlot.HAND);
                        put("Off hand", EquipmentSlot.OFF_HAND);
                        put("Head", EquipmentSlot.HEAD);
                        put("Chest", EquipmentSlot.CHEST);
                        put("Legs", EquipmentSlot.LEGS);
                        put("Feet", EquipmentSlot.FEET);
                    }};

                    ((Player) target).getInventory().setItem(equipmentSlots.get(tags.get("Equipment Slot")), (ItemStack) args.get("item").getVal());
                    break;
                }

                case "SetArmor": {
                    Player player = (Player) target;

                    player.getInventory().setArmorContents(
                        new ItemStack[]{(ItemStack) args.get("head").getVal(), (ItemStack) args.get("chest").getVal(), (ItemStack) args.get("leggings").getVal(), (ItemStack) args.get("feet").getVal()}
                    );
                    break;
                }

                case "ReplaceItems": {
                    replaceItems((Player) target, DFValue.castItem((DFValue[]) args.get("replaceables").getVal()), (ItemStack) args.get("replacement").getVal(), (byte) args.get("amount").getVal());
                    break;
                }

                case "RemoveItems": {
                    for(ItemStack item : DFValue.castItem((DFValue[]) args.get("removals").getVal()))
                        ((Player) target).getInventory().removeItem(item);
                    break;
                }

                case "ClearItems": {
                    clearItems((Player) target, DFValue.castItem((DFValue[]) args.get("items").getVal()));
                    break;
                }

                case "SetCursorItem": {
                    ((Player) target).setItemOnCursor((ItemStack) args.get("item").getVal());
                    break;
                }

                case "ClearInv": {
                    HashMap<String, Integer[]> clearTypes = new HashMap<>(){{
                       put("Entire inventory", new Integer[]{0, 41});
                       put("Main inventory", new Integer[]{0, 35});
                       put("Upper inventory", new Integer[]{8, 35});
                       put("Hotbar", new Integer[]{0, 8});
                       put("Armor", new Integer[]{36,39});
                    }};

                    Integer[] clearIndices = clearTypes.get(tags.get("Clear Mode"));

                    clearInv((Player) target, clearIndices[1], clearIndices[2], tags.get("Clear Crafting and Cursor") == "True");
                    break;
                }

                case "SetItemCooldown": {
                    Material material = ((ItemStack) args.get("item").getVal()).getType();
                    ((Player) target).setCooldown(material, args.get("ticks").getInt());
                    break;
                }

                case "SaveInv": {
                    saveInv((Player) target);
                    break;
                }

                case "LoadInv": {
                    loadInv((Player) target);
                    break;
                }

                case "ShowInv": {
                    ((Player) target).openInventory(createInventory((Player) target,(DFValue[]) args.get("items").getVal(), 27));
                    break;
                }

                case "ExpandInv": {
                    expandInv((Player) target, (DFValue[]) args.get("items").getVal(), 27);
                    break;
                }

                case "SetMenuItem": {
                    ((Player) target).getOpenInventory().setItem( args.get("slot").getInt() - 1, (ItemStack) args.get("item").getVal());
                    break;
                }

                case "SetInvName": {
                    setInvName((Player) target, (String) args.get("invName").getVal());
                    break;
                }

                case "CloseInv": {
                    ((Player) target).closeInventory();
                    break;
                }

                case "RemoveInvRow": {
                    removeInvRow((Player) target, args.get("rows").getInt());
                    break;
                }

                case "AddInvRow": {
                    expandInv((Player) target, (DFValue[]) args.get("items").getVal(), 9);
                    break;
                }

                case "OpenBlockInv": {
                    openContainerInv((Player) target, (Location) args.get("loc").getVal());
                    break;
                }

                case "Damage": {
                    if(args.get("source").getVal() == null) target.damage((double) args.get("amount").getVal());
                    else target.damage((double) args.get("amount").getVal(), Bukkit.getEntity(UUID.fromString((String) args.get("source").getVal())));
                    break;
                }

                case "Heal": {
                    Integer amount = args.get("amount").getInt();
                    if(amount == null) target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    else target.setHealth(target.getHealth() + amount);
                    break;
                }

                case "SetHealth": {
                    target.setHealth(args.get("amount").getInt());
                    break;
                }

                case "SetMaxHealth": {
                    target.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue((double) args.get("amount").getVal());
                    if(tags.get("Heal Player to Max Health") == "True")
                        target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    break;
                }

                case "SetAbsorption": {
                    target.setAbsorptionAmount((double) args.get("amount").getVal());
                    break;
                }

                case "SetFoodLevel": {
                    ((Player) target).setFoodLevel(args.get("amount").getInt());
                    break;
                }

                case "SetSaturation": {
                    ((Player) target).setSaturation((Float) args.get("amount").getVal());
                    break;
                }

                case "GiveExp": {
                    Player player = (Player) target;
                    switch(tags.get("Give Experience")){
                        case "Points":
                            player.giveExp(args.get("amount").getInt());
                            break;
                        case "Level":
                            player.giveExpLevels(args.get("amount").getInt());
                            break;
                        case "Level Percentage":
                            player.setExp(player.getExp() + Math.min((Float) args.get("amount").getVal()/100f, 1f));
                            break;
                    }
                    break;
                }

                case "SetExp": {
                    Player player = (Player) target;
                    switch(tags.get("Set Experience")){
                        case "Points":
                            player.setTotalExperience(args.get("amount").getInt());
                            break;
                        case "Level":
                            player.setLevel(args.get("amount").getInt());
                            break;
                        case "Level Percentage":
                            player.setExp(Math.min((Float) args.get("amount").getVal()/100, 1));
                            break;
                    }
                    break;
                }

                case "GivePotion": {
                    target.addPotionEffects(Arrays.asList(DFValue.castPotion((DFValue[]) args.get("effects").getVal())));
                    break;
                }

                case "RemovePotion": {
                    removePotions((Player) target, DFValue.castPotion((DFValue[]) args.get("effects").getVal()));
                    break;
                }

                case "ClearPotions": {
                    removePotions((Player) target, target.getActivePotionEffects().toArray(new PotionEffect[0]));
                    break;
                }

                case "SetSlot": {
                    ((Player) target).getInventory().setHeldItemSlot(args.get("slot").getInt());
                    break;
                }

                case "SetAtkSpeed": {
                    target.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue((double) args.get("amount").getVal());
                    break;
                }

                case "SetFireTicks": {
                    target.setFireTicks(args.get("ticks").getInt());
                    break;
                }

                case "SetFreezeTicks": {
                    target.setFreezeTicks(args.get("ticks").getInt());
                    break;
                }

                case "SetAirTicks": {
                    target.setRemainingAir(args.get("ticks").getInt());
                    break;
                }

                case "SetInvulTicks": {
                    target.setNoDamageTicks(args.get("ticks").getInt());
                    break;
                }

                case "SetFallDistance": {
                    target.setFallDistance((float) args.get("distance").getVal());
                    break;
                }

                case "SetSpeed": {
                    String speedTag = tags.get("Speed Type");
                    Float speedAmount = (Float) args.get("speed").getVal();
                    if(speedTag == "Ground speed" || speedTag == "Both") ((Player) target).setWalkSpeed(speedAmount/1000f);
                    if(speedTag == "Flight speed" || speedTag == "Both") ((Player) target).setFlySpeed(speedAmount/1000f);
                    break;
                }

                case "SetAllowFlight": {
                    ((Player) target).setAllowFlight(tags.get("Allow Flight") == "Enable");
                    break;
                }

                case "SetAllowPvP": {
                    PlayerData.getPlayerData(target.getUniqueId()).canPvP = tags.get("PVP") == "Enable";
                    break;
                }

                case "SetDropsEnabled": {
                    PlayerData.getPlayerData(target.getUniqueId()).deathDrops = tags.get("Spawn Death Drops") == "Enable";
                    break;
                }

                case "SetInventoryKept": {
                    PlayerData.getPlayerData(target.getUniqueId()).keepInv = tags.get("Inventory Kept") == "Enable";
                    break;
                }

                case "SetCollidable": {
                    target.setCollidable(tags.get("Collision") == "Enable");
                    break;
                }

                case "InstantRespawn": {
                    PlayerData.getPlayerData(target.getUniqueId()).instantRespawn = tags.get("Instant Respawn") == "Enable";
                    break;
                }

                case "EnableBlocks": {
                    PlayerData playerData = PlayerData.getPlayerData(target.getUniqueId());

                    if(args.get("blocks").getVal() == null) playerData.allowedBlocks = (ArrayList<Material>) Arrays.asList(Material.values());
                    else playerData.allowBlocks(getStackTypes(DFValue.castItem( (DFValue[]) args.get("blocks").getVal())));
                    break;
                }

                case "DisableBlocks": {

                    PlayerData playerData = PlayerData.getPlayerData(target.getUniqueId());

                    if(args.get("blocks").getVal() == null) playerData.allowedBlocks = new ArrayList<>();
                    else playerData.allowedBlocks.removeAll(Arrays.asList(getStackTypes(DFValue.castItem((DFValue[]) args.get("blocks").getVal()))));
                    break;
                }

                // ↑ Player Action ////////////////////////////////////////////////////////////////////////
                // ↓ Set Variable  ////////////////////////////////////////////////////////////////////////

                case "=": {
                    DFVar.setVar((DFVar) args.get("var").getVal(), args.get("value"), localStorage);
                    break;
                }

                case "RandomValue": {
                    DFValue[] values = (DFValue[]) args.get("values").getVal();
                    DFValue value = values[new Random().nextInt(values.length)];
                    DFVar.setVar((DFVar) args.get("var").getVal(), value, localStorage);
                    break;
                }

                case "PurgeVars": {
                    String[] varNames = DFValue.castTxt((DFValue[]) args.get("varNames").getVal());
                    String matchReq = tags.get("Match Requirement");
                    boolean ignoreCase = tags.get("Ignore Case") == "True";

                    purgeKeys(varNames, DFVar.globalVars, matchReq, ignoreCase);
                    purgeKeys(varNames, localStorage, matchReq, ignoreCase);
                    /*TODO: Purge save vars once implemented*/
                    break;
                }

                case "+": {
                    Float[] nums = DFValue.castNum((DFValue[]) args.get("nums").getVal());
                    float result = nums[0];

                    for(int i = 1; i < nums.length; i++) result += nums[i];
                    DFVar.setVar((DFVar) args.get("var").getVal(), new DFValue(result, null, DFType.NUM),localStorage);
                    break;
                }

                case "-": {
                    Float[] nums = DFValue.castNum((DFValue[]) args.get("nums").getVal());
                    float result = nums[0];

                    for(int i = 1; i < nums.length; i++) result -= nums[i];
                    DFVar.setVar((DFVar) args.get("var").getVal(), new DFValue(result, null, DFType.NUM),localStorage);
                    break;
                }

                case "x": {
                    Float[] nums = DFValue.castNum((DFValue[]) args.get("nums").getVal());
                    float result = nums[0];

                    for(int i = 1; i < nums.length; i++) result *= nums[i];
                    DFVar.setVar((DFVar) args.get("var").getVal(), new DFValue(result, null, DFType.NUM),localStorage);
                    break;
                }

                case "/": {
                    Float[] nums = DFValue.castNum((DFValue[]) args.get("nums").getVal());
                    float result = nums[0];

                    for(int i = 1; i < nums.length; i++) result /= nums[i];
                    DFVar.setVar((DFVar) args.get("var").getVal(), new DFValue(result, null, DFType.NUM),localStorage);
                    break;
                }

                case "%": {
                    DFValue val = new DFValue((Float) args.get("dividend").getVal() % (Float) args.get("divisor").getVal(), null, DFType.NUM);
                    DFVar.setVar((DFVar) args.get("var").getVal(), val, localStorage);
                    break;
                }

                case "+=": {
                    DFVar var = (DFVar) args.get("var").getVal();

                    Float[] nums = DFValue.castNum((DFValue[]) args.get("nums").getVal());
                    Float num = (Float) DFVar.getVar(var, localStorage).getVal();

                    for(float increment : nums) num += increment;
                    DFVar.setVar(var, new DFValue(num, null, DFType.NUM), localStorage);
                    break;
                }

                case "-=": {
                    DFVar var = (DFVar) args.get("var").getVal();

                    Float[] nums = DFValue.castNum((DFValue[]) args.get("nums").getVal());
                    Float num = (Float) DFVar.getVar(var, localStorage).getVal();

                    for(float decrement : nums) num -= decrement;
                    DFVar.setVar(var, new DFValue(num, null, DFType.NUM), localStorage);
                    break;
                }

                case "Exponent": {
                    DFVar var = (DFVar) args.get("var").getVal();
                    DFValue val = new DFValue(Math.pow((Float) args.get("num").getVal(), (Float) args.get("exponent").getVal()), null, DFType.NUM);

                    DFVar.setVar(var, val, localStorage);
                }

                case "Root": {
                    DFVar var = (DFVar) args.get("var").getVal();
                    DFValue val = new DFValue(Math.pow((Float) args.get("num").getVal(), 1/((Float) args.get("rootIndex").getVal())), null, DFType.NUM);

                    DFVar.setVar(var, val, localStorage);
                }

                case "Logarithm": {
                    DFVar var = (DFVar) args.get("var").getVal();
                    Float num = (Float) args.get("num").getVal();
                    Float base = (Float) args.get("base").getVal();

                    DFValue val = new DFValue(Math.log(num) / Math.log(base), null, DFType.NUM);

                    DFVar.setVar(var, val, localStorage);
                }

                case "ParseNumber": {
                    DFVar var = (DFVar) args.get("var").getVal();
                    String txt = (String) (args.get("txt").getVal() == null ? DFVar.getVar(var, localStorage).getVal() : args.get("txt").getVal());

                    DFVar.setVar(var, new DFValue(Float.parseFloat(txt), null, DFType.NUM), localStorage);
                }

                case "AbsoluteValue": {
                    DFVar var = (DFVar) args.get("var").getVal();
                    Float num = (Float) (args.get("num").getVal() == null ? DFVar.getVar(var, localStorage).getVal() : args.get("num").getVal());

                    DFVar.setVar(var, new DFValue(Math.abs(num), null, DFType.NUM), localStorage);
                }
            }
        // The indentation looks sus here because the for loop above has no brackets :)
    }

    public static void setBossBar(Player barPlayer, String title, Integer id, double barHealth, BarColor color, BarStyle style, BarFlag[] flags){
        HashMap<String, TreeMap<Integer, BossBar>> handler = DFPlugin.bossbarHandler;

        BossBar bar = Bukkit.createBossBar(title, color, style);
        for(BarFlag flag : flags) bar.addFlag(flag);
        bar.setProgress(barHealth);
        TreeMap<Integer, BossBar> currentBars = new TreeMap<>();
        if(handler.containsKey(barPlayer.getName())) currentBars = handler.get(barPlayer.getName());
        else bar.addPlayer(barPlayer);


        for(Integer key : currentBars.keySet().toArray(new Integer[0])) currentBars.get(key).removePlayer(barPlayer);
        currentBars.put(id, bar);
        for(Integer key : currentBars.keySet().toArray(new Integer[0])) currentBars.get(key).addPlayer(barPlayer);

        handler.put(barPlayer.getName(), currentBars);
    }

    public static void removeBossbar(Player p, Integer id){
        HashMap<String, TreeMap<Integer, BossBar>> handler = DFPlugin.bossbarHandler;

        TreeMap<Integer, BossBar> bars = new TreeMap<>();
        if(handler.containsKey(p.getName())) bars = handler.get(p.getName());
        else{
            handler.put(p.getName(), bars);
            return;
        }

        if(!bars.containsKey(id)) return;

        for(Integer key : bars.keySet().toArray(new Integer[0])) bars.get(key).removePlayer(p);

        if(id != null) bars.remove(id);
        else{
            handler.put(p.getName(), new TreeMap<>());
            return;
        }

        for(Integer key : bars.keySet().toArray(new Integer[0])) bars.get(key).addPlayer(p);
        handler.put(p.getName(), bars);
    }

    public static void sendMessageSeq(Player player, String[] msgs, long delay, JavaPlugin plugin){
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run(){
                player.sendMessage(msgs[index++]);
                if(index >= msgs.length) this.cancel();
            }
        }.runTaskTimerAsynchronously(plugin, 0, delay);
    }

    public static void playSoundSeq(Player[] players, DFSound[] sounds, Location loc, long delay, JavaPlugin plugin){
        new BukkitRunnable(){
            int index = 0;
            @Override
            public void run(){
                for(Player player : players){
                    if(loc != null) player.playSound(loc, sounds[index].sound, sounds[index].pitch, sounds[index].volume);
                    else player.playSound(player, sounds[index].sound, sounds[index].pitch, sounds[index].volume);
                }

                index++;
                if(index >= sounds.length) cancel();
            }
        }.runTaskTimer(plugin, 0, delay);
    }

    public static void sendHover(Player p, String msg, String hoverMsg){
        TextComponent component = new TextComponent(msg);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverMsg)));
        p.spigot().sendMessage(component);
    }

    public static void stopSounds(Player p, DFSound[] sounds, SoundCategory category){
        if(category == SoundCategory.MASTER && sounds.length == 0) {
            p.stopAllSounds();
            return;
        }

        for (DFSound sound : sounds) p.stopSound(sound.sound, category);
    }

//    public static void createAdvManager(ItemStack[] icons, JSONMessage[] titles, AdvancementDisplay.AdvancementFrame[] frames, boolean save){
//        AdvancementManager manager = new AdvancementManager(new NameKey("DF", "DFManager"));
//        for(int i = 0; i < icons.length; i++){
//            AdvancementDisplay display = new AdvancementDisplay(icons[i], titles[i], new JSONMessage(new TextComponent("")), frames[i], AdvancementVisibility.ALWAYS);
//            int id = (int) Math.floor(Math.random() * 99999999);
//            Advancement adv = new Advancement(new NameKey("DF", "DF" + id), display);
//            adv.setCriteria(new Criteria(1));
//            manager.addAdvancement(adv);
//        }
//
//        Bukkit.getOnlinePlayers().forEach(manager::addPlayer);
//    }

    public static ItemStack parseItemNBT(String rawNBT){
        if(rawNBT == "null") return null;
        CompoundTag nbt = null;
        try{nbt = TagParser.parseTag(rawNBT);}
        catch(CommandSyntaxException e){}

        net.minecraft.world.item.ItemStack nmsItem = net.minecraft.world.item.ItemStack.of(nbt);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    public static Material[] getStackTypes(ItemStack[] items){
        Material[] result = new Material[items.length];
        for(int i = 0; i < items.length; i++) result[i] = items[i].getType();
        return result;
    }

    public static void replaceItems(Player p, ItemStack[] replaceables, ItemStack replaceItem, byte amount){
        ItemStack[] items = p.getInventory().getContents();
        ItemStack removalItem;

        short itemsReplaced = 0;
        for(int i = 0; i < items.length; i++){
            for(int k = 0; k < replaceables.length; k++){
                if(items[i].isSimilar(replaceables[k])){
                    byte stack = (byte) (items[i].getAmount()/replaceables[k].getAmount());
                    if(stack > (amount - itemsReplaced)) stack = (byte) (amount - itemsReplaced);
                    if(stack > 0){
                        itemsReplaced += stack;
                        replaceItem.setAmount(replaceItem.getAmount() * stack);
                        removalItem = items[i].clone();
                        removalItem.setAmount(replaceables[k].getAmount() * stack);
                        p.getInventory().addItem(replaceItem);
                        p.getInventory().removeItem(removalItem);
                        if(itemsReplaced >= amount) return;
                    }
                    break;
                }
            }

        }
    }


    public static void clearItems(Player p, ItemStack[] items){
        ItemStack[] invContents = p.getInventory().getContents();
        for(int i = 0; i < invContents.length; i++){
            for(int j = 0; j < items.length; j++){
                if(invContents[i].isSimilar(items[j])) p.getInventory().clear(i);
            }
        }
    }

    public static void clearInv(Player p, int min, int max, boolean clearCrafting){
        for(int i = min; i < max; i++){
            p.getInventory().clear(i);
        }
        if(clearCrafting){
            p.setItemOnCursor(null);
            Inventory topInv = p.getOpenInventory().getTopInventory();
            if(topInv.getType() == InventoryType.CRAFTING)
                topInv.clear();
        }
    }

    public static void saveInv(Player p){
        ItemStack[] inv = p.getInventory().getContents();
        String[] result = new String[inv.length];
        for(int i = 0; i < inv.length; i++){
            if(inv[i] == null){
                result[i] = "null";
                continue;
            }

            if(!CraftItemStack.asNMSCopy(inv[i]).hasTag()){
                result[i] = "{Count:" + inv[i].getAmount() + "b,id:\"minecraft:" + inv[i].getType().toString().toLowerCase() + "\"}";
                continue;
            }
            result[i] = formatCompoundTags(inv[i], CraftItemStack.asNMSCopy(inv[i]).getTag().toString());
        }
        p.sendMessage(String.join("|", result));
        managerClass.getConfig().set("players." + p.getUniqueId() + ".inventory", String.join("|", result));
        managerClass.saveConfig();
    }

    public static void loadInv(Player p){
        if(!managerClass.getConfig().contains("players." + p.getUniqueId() + ".inventory")) return;
        String[] inv = managerClass.getConfig().getString("players." + p.getUniqueId() + ".inventory").split("\\|");
        for(int i = 0; i < inv.length; i++){
            if(inv[i] != null) p.getInventory().setItem(i, parseItemNBT(inv[i]));
            else p.getInventory().setItem(i, null);
        }
    }

    public static String formatCompoundTags(ItemStack item, String tags){
        String result = "{Count:" + item.getAmount() + "b, id:\"minecraft:" + item.getType().toString().toLowerCase() + "\",";
        result += "tag:{" + tags.substring(1, tags.length() - 1) + "}}"; // Remove opening and ending brackets from the CompoundTag, then add closing bracket of main nbt.
        return result;
    }

    public static Inventory createInventory(Player p, DFValue[] items, Integer length){
        Inventory inv = Bukkit.createInventory(p, length, "Menu");

        for (DFValue item : items){
            if(item.slot > length) break;
            inv.setItem(item.slot, (ItemStack) item.getVal());
        }

        return inv;
    }

    public static void expandInv(Player p, DFValue[] items, Integer expandLength){
        if(p.getOpenInventory().getType() == InventoryType.PLAYER) return; // Cannot expand player inventory!
        ItemStack[] invItems = (ItemStack[]) ArrayUtils.addAll(p.getOpenInventory().getTopInventory().getContents(), createInventory(p, items, expandLength).getContents());
        byte length = (byte) Math.min(invItems.length, 54);
        Inventory newInv = Bukkit.createInventory(p, length, p.getOpenInventory().getTitle());
        for(int i = 0; i < length; i++){
            newInv.setItem(i, invItems[i]);
        }
        p.openInventory(newInv);
    }

    public static void setInvName(Player p, String name){
        if(p.getOpenInventory().getType() == InventoryType.PLAYER) return;
        ItemStack[] currentInvItems = p.getOpenInventory().getTopInventory().getContents();
        Inventory newInv = Bukkit.createInventory(p, currentInvItems.length, name);
        newInv.setContents(currentInvItems);
        p.openInventory(newInv);
    }

    public static void removeInvRow(Player p, Integer rows){
        if(!inCustomInv(p)) return;

        InventoryView inv = p.getOpenInventory();
        List<ItemStack> invItems = Arrays.asList(inv.getTopInventory().getContents());
        Integer invSize = invItems.size() - rows * 9;
        if(invSize < 9) return;
        Inventory newInv = Bukkit.createInventory(p, invSize, inv.getTitle());
        newInv.setContents(invItems.subList(0, invSize).toArray(new ItemStack[0]));
        p.openInventory(newInv);
    }

    public static void openContainerInv(Player p, Location loc){
        if(loc == null) return;
        Block block = p.getWorld().getBlockAt(loc);
        switch(block.getType()){
            case CRAFTING_TABLE:
                p.openWorkbench(loc, true);
                break;
            case ENCHANTING_TABLE:
                p.openEnchanting(loc, true);
                break;
            case OAK_SIGN:
            case OAK_WALL_SIGN:
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
            case BIRCH_SIGN:
            case BIRCH_WALL_SIGN:
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case ACACIA_SIGN:
            case ACACIA_WALL_SIGN:
            case JUNGLE_SIGN:
            case JUNGLE_WALL_SIGN:
            case CRIMSON_SIGN:
            case CRIMSON_WALL_SIGN:
            case WARPED_SIGN:
            case WARPED_WALL_SIGN:
                p.openSign((Sign) block);
                break;
            case GRINDSTONE:
                p.openInventory(Bukkit.createInventory(p, InventoryType.GRINDSTONE));
                break;
            case ENDER_CHEST:
                p.openInventory(p.getEnderChest());
                break;
            case BEACON:
                p.openInventory(Bukkit.createInventory(p, InventoryType.BEACON));
                break;
            case CARTOGRAPHY_TABLE:
                p.openInventory(Bukkit.createInventory(p, InventoryType.CARTOGRAPHY));
                break;
            case SMITHING_TABLE:
                p.openInventory(Bukkit.createInventory(p, InventoryType.SMITHING));
                break;
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
                p.openInventory(Bukkit.createInventory(p, InventoryType.ANVIL));
                //TODO: Using this menu will not damage the actual anvil at the location! Figure out how to use HumanEntity#openAnvil() instead!
                break;
            default:
                Inventory inv = ((InventoryHolder) block.getState()).getInventory();
                p.openInventory(inv);
                break;
        }
    }

    public static void removePotions(Player p, PotionEffect[] effects){
        for(PotionEffect effect : effects) p.removePotionEffect(effect.getType());
    }

    public static HashMap<String, DFValue> purgeKeys(String[] varNames, HashMap<String, DFValue> storage, String matchReq, boolean ignoreCase){
        String[] storageKeys = storage.keySet().toArray(new String[0]);
        String[] matchedKeys = new String[0];

        for(String name : varNames)
            switch(matchReq) {
                case "Entire name":
                    if (!ignoreCase)
                        matchedKeys = (String[]) Arrays.stream(storageKeys).filter(val -> val.equalsIgnoreCase(name)).toArray();
                    else matchedKeys = (String[]) Arrays.stream(storageKeys).filter(val -> val.equals(name)).toArray();
                    break;
                case "Full word(s) in name":
                    String regex = name.replaceAll("[$^.+*?{}()|\\[\\]\\\\]", "\\\\$0") + "($| )";
                    Pattern pattern = !ignoreCase ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

                    matchedKeys = (String[]) Arrays.stream(storageKeys).filter(val -> pattern.matcher(val).find()).toArray();
                    break;
                case "Any part of name":
                    if(!ignoreCase) matchedKeys = (String[]) Arrays.stream(storageKeys).filter(val -> val.contains(name)).toArray();
                    else matchedKeys = (String[]) Arrays.stream(storageKeys).filter(val -> val.toLowerCase().contains(name.toLowerCase())).toArray();
                    break;
            }

        for(String key : matchedKeys) storage.remove(key);

        return storage;
    }


    public static boolean inCustomInv(Player p){
        Inventory inv = p.getOpenInventory().getTopInventory();
        return inv.getType() != InventoryType.PLAYER
                && inv.getType() != InventoryType.CRAFTING
                && inv.getLocation() == null;
    }



    @EventHandler
    public static void ClickMenuSlot(InventoryClickEvent event){
        if(inCustomInv((Player) event.getView().getPlayer())) event.setCancelled(true); // ClickSlot event triggered from inside custom GUI
    }

    @EventHandler
    public static void PlayerDmgPlayer(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player && event.getEntity() instanceof Player){
            if(!PlayerData.getPlayerData(((Player) event.getEntity()).getUniqueId()).canPvP)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public static void PlaceBlock(BlockPlaceEvent event){
        PlayerData playerData = PlayerData.getPlayerData(event.getPlayer().getUniqueId());
        if(!playerData.allowedBlocks.contains(event.getBlockPlaced().getType())) event.setCancelled(true);
    }

    @EventHandler
    public static void BreakBlock(BlockBreakEvent event){
        PlayerData playerData = PlayerData.getPlayerData(event.getPlayer().getUniqueId());
        if(!playerData.allowedBlocks.contains(event.getBlock().getType())) event.setCancelled(true);
    }

    @EventHandler
    public static void Death(PlayerDeathEvent event){
        PlayerData playerData = PlayerData.getPlayerData(((Player) event.getEntity()).getUniqueId());
        if(!playerData.deathDrops) event.getDrops().clear();
        if(playerData.keepInv) event.setKeepInventory(true);
        if(playerData.instantRespawn) ((Player) event.getEntity()).spigot().respawn();
    }

    public static void getManager(JavaPlugin plugin){
        managerClass = new FileManager(plugin, "playerData.yml");
    }

}
package dispenserduel;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DispenserDuelFeature {

    public enum Side { NONE, LEFT, RIGHT }

    /**
     * Rangliste von Stufe 1 (schwach) bis Stufe 9 (stark). Hier kannst du die Items aendern.
     * Alle 9 Items sollten verschieden sein.
     */
    private static final List<Item> RANKS = List.of(
            Items.DIRT,            // 1
            Items.COBBLESTONE,     // 2
            Items.IRON_INGOT,      // 3
            Items.GOLD_INGOT,      // 4
            Items.DIAMOND,         // 5
            Items.EMERALD,         // 6
            Items.NETHERITE_INGOT, // 7
            Items.ELYTRA,          // 8
            Items.MACE             // 9
    );

    // --- Durchgang (Taste gedrueckt, wartet auf die herausfallenden Items) ---
    private static Side armed = Side.NONE;
    private static int armedTicks = 0;
    private static int assignedCount = 0;
    private static int winnerRank = 9;
    private static int loserRank = 1;
    private static final Set<Integer> ignored = new HashSet<>();
    private static final Set<Integer> done = new HashSet<>();

    // --- Sitzung (Fake-Anzeige aktiv, bis die Items wieder im Dispenser liegen) ---
    private static boolean sessionActive = false;
    private static boolean pickedUp = false;
    private static boolean wasDispenserOpen = false;
    /** echtes Item -> Item, das bei dir angezeigt wird */
    private static final Map<Item, Item> mapping = new HashMap<>();
    /** So viele davon hattest du beim Herausfallen im Inventar (zum Erkennen des Aufsammelns). */
    private static final Map<Item, Integer> baseline = new HashMap<>();

    private static boolean busy = false;
    private static final Random RNG = new Random();

    private static KeyBinding leftKey;
    private static KeyBinding rightKey;
    private static KeyBinding resetKey;

    public static void init() {
        // Standard: Numpad 1/2 (die normale Taste 1 waehlt sonst auch den Hotbar-Slot).
        // Numpad 0 beendet die Fake-Anzeige sofort. Aenderbar unter Optionen > Steuerung.
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("dispenserduel", "main"));
        leftKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dispenserduel.left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_1, category));
        rightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dispenserduel.right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_2, category));
        resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dispenserduel.reset", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_0, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (leftKey.wasPressed()) arm(mc, Side.LEFT);
            while (rightKey.wasPressed()) arm(mc, Side.RIGHT);
            while (resetKey.wasPressed()) endSession();

            if (armed != Side.NONE && mc.world != null && mc.player != null) tickAssign(mc);
            tickSession(mc);
        });
    }

    private static void arm(MinecraftClient mc, Side side) {
        endSession();
        armed = side;
        // Zufaelliges Ergebnis wie 8:3, 7:2, 5:4 ... (Gewinner 2-9, Verlierer immer kleiner)
        winnerRank = 2 + RNG.nextInt(8);               // 2..9
        loserRank = 1 + RNG.nextInt(winnerRank - 1);   // 1..winnerRank-1
        armedTicks = 0;
        assignedCount = 0;
        ignored.clear();
        done.clear();
        if (mc.world != null) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof ItemEntity) ignored.add(e.getId());
            }
        }
    }

    private static void endSession() {
        sessionActive = false;
        pickedUp = false;
        mapping.clear();
        baseline.clear();
    }

    /** Sucht frisch herausgefallene Items und legt fest, als was sie bei dir aussehen. */
    private static void tickAssign(MinecraftClient mc) {
        armedTicks++;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity item)) continue;
            int id = item.getId();
            if (ignored.contains(id) || done.contains(id)) continue;

            ItemStack real = item.getStack();
            if (real.isEmpty()) continue; // Daten sind evtl. noch nicht angekommen
            Item realItem = real.getItem();
            if (!RANKS.contains(realItem)) continue;

            int rank = (sideOf(mc, item) == armed) ? winnerRank : loserRank;
            Item fake = RANKS.get(rank - 1);

            if (!mapping.containsKey(realItem)) {
                mapping.put(realItem, fake);
                // Tauschen, damit im Dispenser nicht zwei gleich aussehende Items liegen.
                if (fake != realItem && !mapping.containsKey(fake)) mapping.put(fake, realItem);
                baseline.put(realItem, mc.player.getInventory().count(realItem));
            }
            sessionActive = true;
            done.add(id);
            assignedCount++;
        }
        // Nach zwei Items (links + rechts) oder nach ca. 10 Sekunden ist die Suche beendet.
        if (assignedCount >= 2 || armedTicks > 200) armed = Side.NONE;
    }

    /** Beendet die Fake-Anzeige, sobald du die Items aufgesammelt und das Dispenser-Menue wieder geschlossen hast. */
    private static void tickSession(MinecraftClient mc) {
        boolean open = mc.currentScreen instanceof Generic3x3ContainerScreen;
        if (sessionActive && mc.player != null) {
            if (!pickedUp) {
                for (Map.Entry<Item, Integer> en : baseline.entrySet()) {
                    if (mc.player.getInventory().count(en.getKey()) > en.getValue()) pickedUp = true;
                }
            }
            if (wasDispenserOpen && !open && pickedUp) endSession();
        }
        wasDispenserOpen = open;
    }

    /** Links/rechts aus deiner Blickrichtung. */
    private static Side sideOf(MinecraftClient mc, ItemEntity item) {
        Vec3d eye = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d to = new Vec3d(item.getX(), item.getY(), item.getZ()).subtract(eye);
        double crossY = look.x * to.z - look.z * to.x;
        return crossY < 0 ? Side.LEFT : Side.RIGHT;
    }

    // ---------- Aufrufe aus den Mixins (nur Anzeige auf deinem Bildschirm) ----------

    /** Wird beim Zeichnen jedes Items aufgerufen (Hotbar, Inventar, Dispenser-Menue, Hand, Boden). */
    public static ItemStack mapStack(ItemStack stack) {
        if (!sessionActive || stack == null || stack.isEmpty()) return stack;
        Item fake = mapping.get(stack.getItem());
        if (fake == null || fake == stack.getItem()) return stack;
        return new ItemStack(fake, stack.getCount());
    }

    /** Wird fuer den Namen im Tooltip aufgerufen. */
    public static Text mapName(ItemStack stack, Text original) {
        if (!sessionActive || busy || stack.isEmpty()) return original;
        Item fake = mapping.get(stack.getItem());
        if (fake == null || fake == stack.getItem()) return original;
        if (stack.get(DataComponentTypes.CUSTOM_NAME) != null) return original;
        busy = true;
        try {
            return new ItemStack(fake).getName();
        } finally {
            busy = false;
        }
    }
}

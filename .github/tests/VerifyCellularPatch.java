import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/** Verifies emitted DEX, including the two callbacks that originally collided. */
public final class VerifyCellularPatch {
    private static final String SAVER = "Lapp/morphe/extension/twitter/patches/CellularDataSaver;";
    private static final String CHAT = "Lapp/morphe/extension/twitter/patches/clientversion/SpoofClientVersionPatch;";

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static boolean calls(Method method, String owner, String name) {
        if (method.getImplementation() == null) return false;
        for (var instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof MethodReference call
                    && call.getDefiningClass().equals(owner) && call.getName().equals(name)) return true;
        }
        return false;
    }

    private static boolean hasString(Method method, String value) {
        if (method.getImplementation() == null) return false;
        for (var instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof StringReference string
                    && string.getString().contains(value)) return true;
        }
        return false;
    }

    private static Method named(ClassDef type, String name) {
        for (var method : type.getMethods()) if (method.getName().equals(name)) return method;
        throw new AssertionError("Missing bridge: " + name);
    }

    public static void main(String[] args) throws Exception {
        ClassDef saver = null;
        ClassDef chat = null;
        List<ClassDef> callbacks = new ArrayList<>();
        int startupHooks = 0;
        try (ZipFile zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                var dex = DexBackedDexFile.fromInputStream(Opcodes.getDefault(),
                        new BufferedInputStream(zip.getInputStream(entry)));
                for (var type : dex.getClasses()) {
                    if (type.getType().equals(SAVER)) saver = type;
                    if (type.getType().equals(CHAT)) chat = type;
                    if ("Landroid/net/ConnectivityManager$NetworkCallback;".equals(type.getSuperclass())) {
                        for (var method : type.getMethods()) {
                            if (method.getName().equals("onCapabilitiesChanged")
                                    && hasString(method, "data_saved_toggled_by_sat_mode")) callbacks.add(type);
                        }
                    }
                    if ("Landroid/app/Application;".equals(type.getSuperclass())) {
                        for (var method : type.getMethods()) {
                            if (method.getName().equals("onCreate") && calls(method, SAVER, "start")) startupHooks++;
                        }
                    }
                }
            }
        }
        require(saver != null, "Cellular saver extension missing");
        String manager = null;
        for (var instruction : named(saver, "createManager").getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof MethodReference call
                    && call.getName().equals("<init>")
                    && call.getParameterTypes().toString().equals("[Landroid/content/Context;]")) manager = call.getDefiningClass();
        }
        require(manager != null, "Native manager constructor bridge missing");
        require(calls(named(saver, "writeEnabled"), manager, "a"), "Native toggle bridge missing for 12.19.1 fixture");
        for (String name : List.of("createManager", "readEnabled", "writeEnabled")) {
            for (var instruction : named(saver, name).getImplementation().getInstructions()) {
                require(instruction.getOpcode() != Opcode.THROW, "Unpatched extension bridge: " + name);
            }
        }
        require(hasString(named(saver, "readEnabled"), "pref_data_saver"), "Preference read bridge missing");
        require(hasString(named(saver, "writeEnabled"), "pref_data_saver"), "Preference write bridge missing");
        require(startupHooks == 1, "Expected one application startup hook, found " + startupHooks);
        require(callbacks.size() == 2, "Fixture must retain both colliding callbacks");
        int guarded = 0;
        for (var callback : callbacks) {
            boolean ownsManager = false;
            for (var field : callback.getFields()) if (field.getType().equals(manager)) ownsManager = true;
            boolean hooked = calls(named(callback, "onCapabilitiesChanged"), SAVER, "isEnabled");
            require(hooked == ownsManager, "Wrong satellite callback hooked: " + callback.getType());
            if (hooked) guarded++;
        }
        require(guarded == 1, "Expected only the native manager callback to be guarded");
        require(chat != null && hasString(named(chat, "xchatOperations"), "SendMessageCreateMutation"),
                "Chat send-operation list was not populated");
        System.out.println("Emitted DEX verified: native bridges, correct satellite callback, startup hook, chat operation list");
    }
}

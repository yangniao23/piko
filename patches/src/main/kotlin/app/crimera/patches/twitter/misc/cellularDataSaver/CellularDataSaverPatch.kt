package app.crimera.patches.twitter.misc.cellularDataSaver

import app.crimera.patches.twitter.misc.extension.twitterInitHook
import app.crimera.patches.twitter.misc.settings.settingsPatch
import app.crimera.patches.twitter.utils.enableSettings
import app.crimera.patches.twitter.utils.Constants.COMPATIBILITY_X
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.string
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION = "Lapp/morphe/extension/twitter/patches/CellularDataSaver;"

private object ManagerConstructor : Fingerprint(
    name = "<init>", parameters = listOf("Landroid/content/Context;"),
    filters = listOf(string("android_sat_mode_support_enabled")),
)
private object NativeToggle : Fingerprint(
    classFingerprint = ManagerConstructor,
    parameters = listOf("Z"), returnType = "V",
    filters = listOf(string("prev_data_sync"), string("prev_image_quality_upload")),
)
private object SatelliteCallback : Fingerprint(
    superclass = "Landroid/net/ConnectivityManager\$NetworkCallback;",
    name = "onCapabilitiesChanged",
    parameters = listOf("Landroid/net/Network;", "Landroid/net/NetworkCapabilities;"),
    filters = listOf(string("data_saved_toggled_by_sat_mode")),
    // X ships a second callback with the same string for its newer media stack.
    // Only the callback holding this manager controls the legacy data saver.
    custom = { _, classDef ->
        classDef.fields.any { it.type == ManagerConstructor.originalMethod.definingClass }
    },
)

private fun <T> Iterable<T>.singleNative(description: String, predicate: (T) -> Boolean): T {
    val matches = filter(predicate)
    if (matches.size != 1) {
        throw PatchException("Cellular data saver: expected one $description, found ${matches.size}")
    }
    return matches[0]
}

@Suppress("unused")
val cellularDataSaverPatch = patch(
    name = "Cellular data saver",
    description = "Adds an opt-in setting to switch native data saver with the active network.",
) {
    // Broaden only after inspecting and testing other app versions.
    compatibleWith(COMPATIBILITY_X)
    dependsOn(settingsPatch)
    execute {
        val ctor = ManagerConstructor.method
        val toggle = NativeToggle.method
        val satellite = SatelliteCallback.method
        if (ctor.definingClass != toggle.definingClass) throw PatchException("Data saver manager mismatch")
        val manager = ctor.definingClass
        val prefs = ctor.implementation!!.instructions.mapNotNull {
            if (it.opcode == Opcode.IPUT_OBJECT) it.getReference<FieldReference>() else null
        }.singleNative("manager preferences field") { it.definingClass == manager }
        // Resolve the preference API from the native toggle that uses this field,
        // rather than assuming a network callback has the same preference API.
        val references = toggle.implementation!!.instructions.mapNotNull { it.getReference<MethodReference>() }.distinct()
        val read = references.singleNative("getBoolean(String, boolean) call") {
            it.definingClass == prefs.type && it.name == "getBoolean" && it.returnType == "Z" &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;", "Z")
        }
        val edit = references.singleNative("preferences edit call") {
            it.definingClass == prefs.type && it.name == "edit" && it.parameterTypes.isEmpty()
        }
        val put = references.singleNative("boolean editor call") {
            it.definingClass == edit.returnType && it.returnType == edit.returnType &&
                it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/String;", "Z")
        }
        val apply = references.singleNative("editor apply call") {
            it.definingClass == edit.returnType && it.parameterTypes.isEmpty() && it.returnType == "V"
        }
        val extension = mutableClassDefBy(EXTENSION)
        fun bridge(name: String, registers: Int, code: String) {
            val original = extension.methods.singleNative("$name extension bridge") { it.name == name }
            val replacement = ImmutableMethod(
                original.definingClass,
                original.name,
                original.parameters,
                original.returnType,
                original.accessFlags,
                original.annotations,
                original.hiddenApiRestrictions,
                MutableMethodImplementation(registers),
            ).toMutable().apply {
                addInstructions(0, code)
            }
            extension.methods.remove(original)
            extension.methods.add(replacement)
        }
        bridge("createManager", 2, """
            new-instance v0, $manager
            invoke-direct {v0, p0}, $ctor
            return-object v0
        """)
        bridge("readEnabled", 4, """
            check-cast p0, $manager
            iget-object v0, p0, $prefs
            const-string v1, "pref_data_saver"
            const/4 v2, 0x0
            invoke-interface {v0, v1, v2}, $read
            move-result v0
            return v0
        """)
        bridge("writeEnabled", 4, """
            check-cast p0, $manager
            invoke-virtual {p0, p1}, $toggle
            iget-object v0, p0, $prefs
            invoke-interface {v0}, $edit
            move-result-object v0
            const-string v1, "pref_data_saver"
            invoke-interface {v0, v1, p1}, $put
            move-result-object v0
            invoke-interface {v0}, $apply
            return-void
        """)
        // Keep native satellite support intact when automation is disabled.
        if (satellite.implementation!!.registerCount < 4) throw PatchException("No satellite scratch register")
        satellite.addInstructionsWithLabels(0, """
            invoke-static {}, $EXTENSION->isEnabled()Z
            move-result v0
            if-eqz v0, :native_policy
            return-void
            :native_policy
            nop
        """)
        val startup = twitterInitHook.fingerprint.method
        val returns = startup.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        if (returns.isEmpty()) throw PatchException("No application startup return")
        returns.reversed().forEach {
            startup.addInstruction(it, "invoke-static/range {p0 .. p0}, $EXTENSION->start(Landroid/content/Context;)V")
        }
        enableSettings("cellularDataSaver")
    }
}

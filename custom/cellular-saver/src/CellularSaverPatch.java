package mobile.saver.patches;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import app.morphe.patcher.extensions.InstructionExtensions;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import kotlin.Unit;
import kotlin.Pair;
import java.util.Set;

public final class CellularSaverPatch {
 private static final String MANAGER="Lcom/twitter/app/settings/s1;";
 public static final Patch<?> PATCH = create();
 private static Patch<?> create() {
  BytecodePatchBuilder b=new BytecodePatchBuilder("Cellular-only data saver", "Automatically enables native data saver on cellular and restores settings on Wi-Fi. Only X 12.19.1-release.0.",true);
  b.compatibleWith(new Pair<>("com.twitter.android", Set.of("12.19.1-release.0")));
  b.extendWith(() -> CellularSaverPatch.class.getResourceAsStream("/extensions/cellular-saver.dex"));
  b.execute(ctx -> {
   // Fail closed on a different app structure; never guess obfuscated classes.
   MutableClass manager=ctx.mutableClassDefBy(MANAGER);
   MutableMethod ctor=manager.getMethods().stream().filter(m -> m.getName().equals("<init>") && m.getParameterTypes().toString().equals("[Landroid/content/Context;]")).findFirst().orElseThrow();
   int insert=-1;
   for(int i=0;i<ctor.getImplementation().getInstructions().size();i++) {
    var ins=ctor.getImplementation().getInstructions().get(i);
    if(ins.getOpcode()==Opcode.IPUT_OBJECT && ins instanceof ReferenceInstruction) {
     var ref=((ReferenceInstruction)ins).getReference();
     if(ref instanceof FieldReference && ((FieldReference)ref).getType().equals("Lcom/twitter/util/prefs/o;")) insert=i+1;
    }
   }
   if(insert<0) throw new IllegalStateException("Native data saver constructor changed");
   // The constructor's original satellite network request would fight this policy.
   InstructionExtensions.INSTANCE.addInstruction(ctor,insert,"return-void");
   MutableClass app=ctx.mutableClassDefBy("Lcom/snap/stuffing/api/exopackage/b;");
   MutableMethod onCreate=app.getMethods().stream().filter(m -> m.getName().equals("onCreate") && m.getParameterTypes().isEmpty()).findFirst().orElseThrow();
   int end=onCreate.getImplementation().getInstructions().size()-1;
   if(onCreate.getImplementation().getInstructions().get(end).getOpcode()!=Opcode.RETURN_VOID) throw new IllegalStateException("Application startup changed");
   InstructionExtensions.INSTANCE.addInstruction(onCreate,end,"invoke-static/range {p0 .. p0}, Lmobile/saver/CellularSaver;->start(Landroid/content/Context;)V");
   return Unit.INSTANCE;
  });
  return b.build$morphe_patcher();
 }
}

import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.io.*;
import java.util.*;
public class VerifyDex {
 public static void main(String[] args) throws Exception {
  var c=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.forApi(36));
  var targets=Set.of("Lmobile/saver/CellularSaver;","Lcom/twitter/app/settings/s1;","Lcom/snap/stuffing/api/exopackage/b;");
  int seen=0;
  for(var name:c.getDexEntryNames()) for(var cls:c.getEntry(name).getDexFile().getClasses()) {
   if(!targets.contains(cls.getType())) continue;
   seen++;
   for(var m:cls.getMethods()) {
    if(m.getImplementation()==null)continue;
    if(cls.getType().equals("Lmobile/saver/CellularSaver;") || m.getName().equals("<init>") || m.getName().equals("onCreate")) {
     System.out.println(cls.getType()+"->"+m.getName());
     for(var i:m.getImplementation().getInstructions()) {
      System.out.println("  "+i.getOpcode()+(i instanceof ReferenceInstruction?" "+((ReferenceInstruction)i).getReference():""));
     }
    }
   }
  }
  if(seen!=3) throw new IllegalStateException("Missing or duplicate target classes: "+seen);
 }
}

import com.android.tools.smali.smali.*;
public class Assemble {
 public static void main(String[] args) throws Exception {
  SmaliOptions o = new SmaliOptions(); o.apiLevel=28; o.outputDexFile=args[1]; o.jobs=4;
  if (!Smali.assemble(o,args[0])) throw new IllegalStateException("Smali failed");
 }
}

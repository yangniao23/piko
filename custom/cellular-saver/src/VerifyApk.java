import com.android.apksig.ApkVerifier;
import java.io.File;
import java.security.MessageDigest;
import java.util.HexFormat;
public class VerifyApk {
 public static void main(String[] args) throws Exception {
  var r=new ApkVerifier.Builder(new File(args[0])).setMinCheckedPlatformVersion(36).setMaxCheckedPlatformVersion(36).build().verify();
  System.out.println("verified="+r.isVerified()+" v2="+r.isVerifiedUsingV2Scheme()+" v3="+r.isVerifiedUsingV3Scheme());
  for(var c:r.getSignerCertificates()) System.out.println("sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(c.getEncoded())));
  for(var e:r.getErrors()) System.out.println("ERROR "+e);
  if(!r.isVerified()) throw new IllegalStateException("APK signature verification failed");
 }
}

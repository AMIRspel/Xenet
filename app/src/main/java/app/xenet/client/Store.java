package app.xenet.client;
import android.content.Context;import android.os.Build;import android.security.KeyPairGeneratorSpec;import android.security.keystore.KeyGenParameterSpec;import android.security.keystore.KeyProperties;import android.util.AtomicFile;
import javax.crypto.*;import javax.crypto.spec.*;import javax.security.auth.x500.X500Principal;import java.math.BigInteger;import java.security.*;import java.io.*;import java.nio.channels.*;import java.util.*;import org.json.*;
/** Encrypted atomic store, at most100 profiles; all methods run on IO workers. */
public final class Store {
 public static final int LIMIT=100;private final Context context;private final File file,lock;private final AtomicFile atomic;
 public Store(Context c){context=c.getApplicationContext();file=new File(c.getFilesDir(),"profiles.enc");lock=new File(c.getFilesDir(),"profiles.lock");atomic=new AtomicFile(file);}
 private SecretKey key()throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(Build.VERSION.SDK_INT>=23){String alias="xenet-data-aes-v1";if(!ks.containsAlias(alias)){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());g.generateKey();}return (SecretKey)ks.getKey(alias,null);}
  String alias="xenet-wrap-rsa-v1";if(!ks.containsAlias(alias)){Calendar from=Calendar.getInstance(),to=Calendar.getInstance();to.add(Calendar.YEAR,30);KeyPairGenerator g=KeyPairGenerator.getInstance("RSA","AndroidKeyStore");g.initialize(new KeyPairGeneratorSpec.Builder(context).setAlias(alias).setSubject(new X500Principal("CN=Xenet local key")).setSerialNumber(BigInteger.ONE).setStartDate(from.getTime()).setEndDate(to.getTime()).setKeySize(2048).build());g.generateKeyPair();}
  File wrapped=new File(context.getFilesDir(),"wrapped-aes");Cipher rsa=Cipher.getInstance("RSA/ECB/PKCS1Padding");byte[] raw;
  if(wrapped.exists()){rsa.init(Cipher.DECRYPT_MODE,ks.getKey(alias,null));raw=rsa.doFinal(read(wrapped,4096));}else{raw=new byte[32];new SecureRandom().nextBytes(raw);rsa.init(Cipher.ENCRYPT_MODE,ks.getCertificate(alias).getPublicKey());try(FileOutputStream o=new FileOutputStream(wrapped)){o.write(rsa.doFinal(raw));}}
  return new SecretKeySpec(raw,"AES");
 }
 private JSONObject load()throws Exception {
  if(!file.exists()&&!new java.io.File(file.getPath()+".bak").exists())return new JSONObject().put("profiles",new JSONArray()).put("groups",new JSONArray()).put("selected","");
  byte[] data;try(FileInputStream in=atomic.openRead()){data=read(in,4*1024*1024);}
  if(data.length<29)throw new IOException("STORE_CORRUPT");byte[] nonce=Arrays.copyOfRange(data,0,12);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,nonce));return new JSONObject(new String(c.doFinal(Arrays.copyOfRange(data,12,data.length)),"UTF-8"));
 }
 private void save(JSONObject data)throws Exception {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] iv=c.getIV();if(iv.length!=12)throw new IOException("KEY_IV");byte[] cipher=c.doFinal(data.toString().getBytes("UTF-8"));FileOutputStream out=null;try{out=atomic.startWrite();out.write(iv);out.write(cipher);atomic.finishWrite(out);}catch(Exception e){if(out!=null)atomic.failWrite(out);throw e;}}
 public interface Change {void apply(JSONObject d)throws Exception;}
 public JSONObject transaction(Change change)throws Exception {synchronized(Store.class){try(RandomAccessFile f=new RandomAccessFile(lock,"rw");FileLock l=f.getChannel().lock()){JSONObject d=load();if(change!=null){change.apply(d);validate(d);save(d);}return d;}}}
 static void validate(JSONObject d)throws Exception {new ConnectionOptions(d.optJSONObject("options"));JSONArray a=d.getJSONArray("profiles");if(a.length()>LIMIT)throw new IllegalArgumentException("LIMIT_100");String selected=d.optString("selected");boolean found=false;for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(selected))found=true;if(!found)d.put("selected",a.length()==0?"":a.getJSONObject(0).getString("id"));if(d.toString().getBytes("UTF-8").length>4*1024*1024-28)throw new IllegalArgumentException("STORE_TOO_LARGE");}
 public static void append(JSONObject d,List<Profile> rows)throws Exception {JSONArray a=d.getJSONArray("profiles");if(a.length()+rows.size()>LIMIT)throw new IllegalArgumentException("LIMIT_100");for(Profile p:rows)a.put(p.json());}
 public static void replaceGroup(JSONObject d,String id,String name,String url,List<Profile> rows)throws Exception {
  JSONArray kept=new JSONArray(),old=d.getJSONArray("profiles");for(int i=0;i<old.length();i++)if(!id.equals(old.getJSONObject(i).optString("group")))kept.put(old.getJSONObject(i));if(kept.length()+rows.size()>LIMIT)throw new IllegalArgumentException("LIMIT_100");for(Profile p:Catalog.retainIds(old,id,rows))kept.put(p.json());
  JSONArray groups=new JSONArray(),g=d.getJSONArray("groups");JSONObject replacement=new JSONObject().put("id",id).put("name",name).put("url",url);boolean replaced=false;for(int i=0;i<g.length();i++){if(id.equals(g.getJSONObject(i).getString("id"))){groups.put(replacement);replaced=true;}else groups.put(g.getJSONObject(i));}if(!replaced)groups.put(replacement);d.put("profiles",kept).put("groups",groups);
 }
 public static List<Profile> profiles(JSONObject d)throws Exception {List<Profile> out=new ArrayList<>();JSONArray a=d.getJSONArray("profiles");for(int i=0;i<a.length();i++)out.add(Profile.from(a.getJSONObject(i)));return out;}
 static byte[] read(File f,int max)throws Exception{try(FileInputStream in=new FileInputStream(f)){return read(in,max);}}
 static byte[] read(InputStream in,int max)throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("INPUT_TOO_LARGE");out.write(b,0,n);}return out.toByteArray();}
}

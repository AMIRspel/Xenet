package app.xenet.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** One process-local subscription operation that survives activity recreation. */
final class SubscriptionJob {
 interface Listener { void changed(Snapshot snapshot); }
 enum Phase { RUNNING, SUCCESS, FAILURE }
 static final class Snapshot {
  final Phase phase; final String id, name, error; final int count; final JSONObject data;
  Snapshot(Phase phase,String id,String name,int count,String error,JSONObject data){this.phase=phase;this.id=id;this.name=name;this.count=count;this.error=error;this.data=data;}
 }
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static final Handler main=new Handler(Looper.getMainLooper());
 private static Listener listener; private static Snapshot current;
 private SubscriptionJob(){}
 static boolean running(){return current!=null&&current.phase==Phase.RUNNING;}
 static Snapshot current(){return current;}
 static void attach(Listener l){listener=l;if(current!=null)l.changed(current);}
 static void detach(Listener l){if(listener==l)listener=null;}
 static void acknowledge(Snapshot s){if(current==s&&s.phase!=Phase.RUNNING)current=null;}
 private static void publish(Snapshot s){current=s;if(listener!=null)listener.changed(s);}
 static boolean start(Context context,String id,String name,String url,int proxyPort){
  if(running())return false;
  Context app=context.getApplicationContext();publish(new Snapshot(Phase.RUNNING,id,name,0,"",null));
  worker.execute(()->{
   try {
    SubscriptionOperation.Result result=SubscriptionOperation.execute(id,url,proxyPort,Net::subscription,incoming->new Store(app).transaction(x->{Store.replaceGroup(x,id,name,url,incoming);JSONArray gs=x.getJSONArray("groups");for(int i=0;i<gs.length();i++)if(gs.getJSONObject(i).getString("id").equals(id))gs.getJSONObject(i).put("updated",System.currentTimeMillis());}));
    Snapshot done=new Snapshot(Phase.SUCCESS,id,name,result.count,"",result.data);main.post(()->publish(done));
   } catch(Exception e){String code=Net.error(e);Snapshot failed=new Snapshot(Phase.FAILURE,id,name,0,code,null);main.post(()->publish(failed));}
  });return true;
 }
}

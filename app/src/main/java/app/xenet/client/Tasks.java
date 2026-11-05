package app.xenet.client;
import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Pure single-owner state machine. Stop never waits for network/JNI or worker completion. */
public final class Tasks {
 public static final class Job {public final String kind;public final AtomicBoolean cancelled=new AtomicBoolean();private final AtomicReference<Runnable> hook=new AtomicReference<>();Job(String kind){this.kind=kind;}public void check()throws InterruptedException{if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedException();}}
 private final AtomicReference<Job> current=new AtomicReference<>();private final Executor closer;
 public Tasks(Executor closer){this.closer=closer;}
 public Job begin(String kind){Job j=new Job(kind);return current.compareAndSet(null,j)?j:null;}
 public Job current(){return current.get();}
 public void hook(Job j,Runnable action){j.hook.set(action);if(j.cancelled.get()&&action!=null)closer.execute(action);}
 public void stop(){Job j=current.get();if(j==null)return;j.cancelled.set(true);Runnable h=j.hook.get();if(h!=null)closer.execute(h);}
 public boolean finish(Job j){return current.compareAndSet(j,null);}
 public boolean requiresReset(Job j){return current.get()==j&&j.cancelled.get();}
}

package app.xenet.client;
import java.util.concurrent.atomic.AtomicBoolean;
/** Explicit reconnect is coalesced, never starts an idle VPN or another native owner. */
public final class ReconnectGate {private final AtomicBoolean pending=new AtomicBoolean();public boolean request(String kind,String state){return kind.equals("vpn")&&state.equals("connected")&&pending.compareAndSet(false,true);}public boolean take(){return pending.getAndSet(false);}public void clear(){pending.set(false);}}

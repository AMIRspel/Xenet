package app.xenet.client;
/** Small deterministic policy: no background animation or pretend connection progress. */
final class MotionPolicy {
 static boolean animate(String state,boolean enabled,boolean visible){return enabled&&visible&&(state.equals("reconnecting")||state.equals("starting")||state.equals("connected")||state.equals("testing")||state.equals("stopping"));}
 static float phase(long elapsed){return (elapsed%2400L+2400L)%2400L/2400f;}
}

package app.xenet.client;
import org.junit.Test;import static org.junit.Assert.*;
public class MotionPolicyTest {
 @Test public void idleDoesNotAnimate(){assertFalse(MotionPolicy.animate("idle",true,true));}
 @Test public void connectingAnimates(){assertTrue(MotionPolicy.animate("starting",true,true));}
 @Test public void connectedPulse(){assertTrue(MotionPolicy.animate("connected",true,true));}
 @Test public void backgroundDoesNotAnimate(){assertFalse(MotionPolicy.animate("testing",true,false));}
 @Test public void reducedMotionHasNoFrames(){for(String s:new String[]{"starting","connected","testing","stopping"})assertFalse(MotionPolicy.animate(s,false,true));}
 @Test public void phaseBounded(){for(long n:new long[]{-2401,-1,0,1,2399,2400,Long.MAX_VALUE})assertTrue(MotionPolicy.phase(n)>=0&&MotionPolicy.phase(n)<1);assertEquals(0,MotionPolicy.phase(2400),0);}
}

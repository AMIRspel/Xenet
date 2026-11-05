package app.xenet.client;
import org.junit.*;import static org.junit.Assert.*;
public class FeedbackPolicyTest {
 @Test public void selectionIsSilent(){assertEquals("",FeedbackPolicy.message(FeedbackPolicy.Event.SELECTED));}
 @Test public void themeIsSilent(){assertEquals("",FeedbackPolicy.message(FeedbackPolicy.Event.THEME));}
 @Test public void collapseIsSilent(){assertEquals("",FeedbackPolicy.message(FeedbackPolicy.Event.COLLAPSED));}
 @Test public void importIsSilent(){assertEquals("",FeedbackPolicy.message(FeedbackPolicy.Event.IMPORTED));}
 @Test public void deleteIsSilent(){assertEquals("",FeedbackPolicy.message(FeedbackPolicy.Event.DELETED));}
 @Test public void subscriptionProgressIsExplicit(){assertTrue(SubscriptionFeedback.running().contains("Updating"));}
 @Test public void subscriptionSuccessHasCount(){assertEquals("Subscription updated successfully · 2 servers.",SubscriptionFeedback.success(2,false));}
 @Test public void subscriptionSuccessConnectedExplainsReconnect(){assertTrue(SubscriptionFeedback.success(1,true).contains("Stop, then Connect"));}
 @Test public void subscriptionFailureExplainsAndKeepsOld(){String s=SubscriptionFeedback.failure("Check your connection.","NETWORK");assertTrue(s.contains("failed"));assertTrue(s.contains("NETWORK"));assertTrue(s.contains("previous servers"));}
 @Test public void nullIsSilent(){assertEquals("",FeedbackPolicy.message(null));}
 @Test public void routingExplainsHowToApply(){String s=FeedbackPolicy.message(FeedbackPolicy.Event.ROUTING_SAVED);assertTrue(s.contains("Stop, then Connect"));assertTrue(s.contains("App choices saved"));}
 @Test public void connectionExplainsHowToApply(){assertTrue(FeedbackPolicy.message(FeedbackPolicy.Event.CONNECTION_SAVED).contains("Stop, then Connect"));}
 @Test public void restoringKeepsApps(){assertTrue(FeedbackPolicy.message(FeedbackPolicy.Event.DEFAULTS_RESTORED).contains("App choices kept"));}
 @Test public void copyingWarnsAboutPrivacy(){assertTrue(FeedbackPolicy.message(FeedbackPolicy.Event.PRIVATE_COPIED).contains("private"));}
}

package app.xenet.client;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.io.IOException;import java.util.concurrent.atomic.AtomicInteger;
public class SubscriptionOperationTest {
 private static final String VALID="vless://12345678-1234-1234-1234-123456789012@server.example:443#Server";
 @Test public void validResponseCommitsParsedGroupAndCount()throws Exception{
  AtomicInteger commits=new AtomicInteger();JSONObject data=new JSONObject().put("saved",true);
  SubscriptionOperation.Result r=SubscriptionOperation.execute("group-id","https://example.test/sub",0,(u,p)->VALID,profiles->{commits.incrementAndGet();assertEquals(1,profiles.size());assertEquals("group-id",profiles.get(0).group);return data;});
  assertEquals(1,commits.get());assertEquals(1,r.count);assertSame(data,r.data);
 }
 @Test public void fetchFailureNeverCommits()throws Exception{AtomicInteger commits=new AtomicInteger();try{SubscriptionOperation.execute("g","u",0,(u,p)->{throw new IOException("HTTP_403");},profiles->{commits.incrementAndGet();return new JSONObject();});fail();}catch(IOException e){assertEquals("HTTP_403",e.getMessage());}assertEquals(0,commits.get());}
 @Test public void malformedResponseNeverCommits()throws Exception{AtomicInteger commits=new AtomicInteger();try{SubscriptionOperation.execute("g","u",0,(u,p)->"unknown://broken",profiles->{commits.incrementAndGet();return new JSONObject();});fail();}catch(Exception expected){}assertEquals(0,commits.get());}
 @Test public void emptyResponseNeverCommits()throws Exception{AtomicInteger commits=new AtomicInteger();try{SubscriptionOperation.execute("g","u",0,(u,p)->"  ",profiles->{commits.incrementAndGet();return new JSONObject();});fail();}catch(IllegalArgumentException e){assertEquals("EMPTY_INPUT",e.getMessage());}assertEquals(0,commits.get());}
 @Test public void failedCommitIsNotReportedAsSuccess()throws Exception{try{SubscriptionOperation.execute("g","u",0,(u,p)->VALID,profiles->{throw new IOException("STORE_FAILURE");});fail();}catch(IOException e){assertEquals("STORE_FAILURE",e.getMessage());}}
 @Test public void chosenProxyIsPassedToFetcher()throws Exception{SubscriptionOperation.execute("g","url",2468,(u,p)->{assertEquals("url",u);assertEquals(2468,p);return VALID;},profiles->new JSONObject());}
}

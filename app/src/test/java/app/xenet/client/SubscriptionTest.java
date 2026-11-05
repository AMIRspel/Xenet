package app.xenet.client;
import org.junit.*;import static org.junit.Assert.*;import java.net.*;import java.io.*;import java.util.concurrent.*;import okhttp3.HttpUrl;
public class SubscriptionTest {
 ServerSocket server;ExecutorService worker;
 @Before public void init()throws Exception{server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));worker=Executors.newSingleThreadExecutor();}
 @After public void close()throws Exception{server.close();worker.shutdownNow();}
 String url(){return "http://127.0.0.1:"+server.getLocalPort()+"/start";}
 void replies(String... values){worker.submit(()->{for(String response:values)try(Socket s=server.accept()){s.setSoTimeout(3000);BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream()));String l;while((l=in.readLine())!=null&&!l.isEmpty()){}s.getOutputStream().write(response.getBytes("UTF-8"));s.getOutputStream().flush();}catch(Exception ignored){return;}});}
 String body(String b)throws Exception{return "HTTP/1.1 200 OK\r\nContent-Length: "+b.getBytes("UTF-8").length+"\r\nConnection: close\r\n\r\n"+b;}
 @Test public void followsRelativeRedirect()throws Exception{replies("HTTP/1.1 302 Found\r\nLocation: /final\r\nContent-Length: 0\r\nConnection: close\r\n\r\n",body("vless://id@server.test:443"));assertTrue(Net.subscription(url()).startsWith("vless://"));}
 @Test public void activeProxyCanFetchBlockedDirectHost()throws Exception{replies(body("vless://id@server.test:443"));assertTrue(Net.subscription("http://not-resolvable.invalid/sub",server.getLocalPort()).startsWith("vless://"));}
 @Test public void stripsBom()throws Exception{replies(body("\uFEFFvless://id@server.test:443"));assertTrue(Net.subscription(url()).startsWith("vless://"));}
 @Test public void loopRejected()throws Exception{replies("HTTP/1.1 302 Found\r\nLocation: /start\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");try{Net.subscription(url());fail();}catch(IOException e){assertEquals("REDIRECT_LOOP",e.getMessage());}}
 @Test public void htmlRejected()throws Exception{replies(body("<!doctype html><html>not a config</html>"));try{Net.subscription(url());fail();}catch(IOException e){assertEquals("SUBSCRIPTION_HTML",e.getMessage());}}
 @Test public void expiredLinkError()throws Exception{replies("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");try{Net.subscription(url());fail();}catch(IOException e){assertEquals("HTTP_403",e.getMessage());}}
 @Test public void downgradeRejected()throws Exception{try{Net.redirectTarget(HttpUrl.parse("https://example.test/start"),"http://example.test/end");fail();}catch(IOException e){assertEquals("HTTPS_DOWNGRADE",e.getMessage());}}
 @Test public void userinfoNotAccepted(){try{Net.subscriptionUrl("https://user:secret@example.test/path");fail();}catch(IllegalArgumentException e){assertEquals("BAD_URL",e.getMessage());}}
 @Test public void oversizeRejected()throws Exception{StringBuilder b=new StringBuilder();for(int i=0;i<2*1024*1024+1;i++)b.append('a');replies(body(b.toString()));try{Net.subscription(url());fail();}catch(IOException e){assertEquals("INPUT_TOO_LARGE",e.getMessage());}}
}

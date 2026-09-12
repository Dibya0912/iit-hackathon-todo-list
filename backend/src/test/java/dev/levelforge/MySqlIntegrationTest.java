package dev.levelforge;
import dev.levelforge.Dtos.*;
import dev.levelforge.Models.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=${TEST_DB_URL}","spring.datasource.username=${TEST_DB_USER}","spring.datasource.password=${TEST_DB_PASSWORD}"})
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches="jdbc:mysql:.*")
@Import(MySqlIntegrationTest.TimeConfig.class)
class MySqlIntegrationTest {
 @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
 @Autowired tools.jackson.databind.json.JsonMapper json;
 @Autowired AuthService auth; @Autowired GameService game; @Autowired JdbcTemplate jdbc; @Autowired TestClock clock;
 @TestConfiguration static class TimeConfig { @Bean @Primary TestClock testClock(){return new TestClock();} }
 static class TestClock extends Clock {
  volatile Instant now=Instant.parse("2026-03-08T10:00:00Z");
  public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return this;} public Instant instant(){return now;}
 }
 @BeforeEach void resetTime(){clock.now=Instant.parse("2026-03-08T10:00:00Z");}
 String password(){return "Aa9!"+UUID.randomUUID()+UUID.randomUUID();}
 long user(){return auth.signup(new Signup("Tester","test-"+UUID.randomUUID()+"@example.com",password(),"UTC")).id();}
 Quest quest(long u,Difficulty d,Category c){return game.create(u,new QuestInput("A meaningful task","Description",c,d,null));}
 <T> List<T> concurrent(Callable<T> action) throws Exception {
  try(var executor=Executors.newFixedThreadPool(8)) {
   CountDownLatch start=new CountDownLatch(1);List<Future<T>> futures=new ArrayList<>();
   for(int i=0;i<8;i++)futures.add(executor.submit(()->{start.await();return action.call();}));
   start.countDown();List<T> results=new ArrayList<>();for(var f:futures)results.add(f.get(20,TimeUnit.SECONDS));return results;
  }
 }
 @Test void completionExactlyOnceUnderConcurrency() throws Exception {
  long u=user();var q=quest(u,Difficulty.HARD,Category.CODING);
  var results=concurrent(()->game.complete(u,q.id()));
  assertEquals(1,results.stream().filter(CompletionResult::applied).count());
  var h=game.character(u);assertEquals(50,h.progress().total());assertEquals(20,h.gold());
  assertEquals(50,h.attributes().stream().filter(a->a.type().equals("INTELLECT")).findFirst().orElseThrow().progress().total());
  assertFalse(game.complete(u,q.id()).applied());
  assertEquals(1,jdbc.queryForObject("select count(*) from task_completions where task_id=?",Integer.class,q.id()));
  assertEquals(1,game.activity(u,0,50).totalElements());
  game.archive(u,q.id());assertFalse(game.complete(u,q.id()).applied());
  assertThrows(ApiError.class,()->game.edit(u,q.id(),new QuestInput("Changed","",Category.STUDY,Difficulty.EASY,null)));
 }
 @Test void ownershipAndEquipDenial() {
  long u=user(),other=user();var q=quest(u,Difficulty.EASY,Category.READING);
  assertEquals(404,assertThrows(ApiError.class,()->game.quest(other,q.id())).status);
  assertEquals(404,assertThrows(ApiError.class,()->game.complete(other,q.id())).status);
  assertEquals(404,assertThrows(ApiError.class,()->game.archive(other,q.id())).status);
  assertEquals(404,assertThrows(ApiError.class,()->game.equip(other,999999L,true)).status);
  for(int i=0;i<2;i++)game.complete(u,quest(u,Difficulty.HARD,Category.STUDY).id());
  game.purchase(u,4);var owned=game.inventory(u).getFirst();
  assertEquals(404,assertThrows(ApiError.class,()->game.equip(other,owned.inventoryId(),true)).status);
  assertEquals(0,game.character(other).gold());assertTrue(game.inventory(other).isEmpty());
 }
 @Test void purchaseConcurrencyAndInsufficientFunds() throws Exception {
  long u=user();assertEquals("INSUFFICIENT_GOLD",assertThrows(ApiError.class,()->game.purchase(u,4)).code);
  for(int i=0;i<3;i++)game.complete(u,quest(u,Difficulty.HARD,Category.EXERCISE).id());
  assertEquals(2,game.character(u).progress().level());
  var results=concurrent(()->game.purchase(u,4));
  assertEquals(1,results.stream().filter(PurchaseResult::applied).count());assertEquals(30,game.character(u).gold());
  assertEquals(1,game.inventory(u).size());assertFalse(game.purchase(u,4).applied());
  long id=game.inventory(u).getFirst().inventoryId();
  assertEquals(1,game.equip(u,id,true).cosmetics().size());
  assertTrue(game.equip(u,id,false).cosmetics().isEmpty());
  assertEquals(4,game.activity(u,0,50).totalElements());
 }
 @Test void simultaneousDifferentPurchasesCannotOverspend() throws Exception {
  long u=user();for(int i=0;i<3;i++)game.complete(u,quest(u,Difficulty.HARD,Category.MEDITATION).id());
  var counter=new java.util.concurrent.atomic.AtomicInteger();
  var outcomes=concurrent(()->{try{return game.purchase(u,counter.getAndIncrement()%2==0?5:6).applied();}catch(ApiError e){assertEquals("INSUFFICIENT_GOLD",e.code);return false;}});
  assertEquals(1,outcomes.stream().filter(Boolean::booleanValue).count());assertTrue(game.character(u).gold()>=0);
 }
 @Test void actualStreakAndTimezoneBoundaryPolicy() {
  long u=user();game.complete(u,quest(u,Difficulty.EASY,Category.READING).id());assertEquals(1,game.character(u).streak());
  game.complete(u,quest(u,Difficulty.EASY,Category.READING).id());assertEquals(1,game.character(u).streak());
  var profile=game.profile(u,new Dtos.Profile("Traveller","Pacific/Kiritimati","MAGE"));
  assertEquals("UTC",profile.timezone());assertEquals("Pacific/Kiritimati",profile.pendingTimezone());
  clock.now=Instant.parse("2026-03-08T23:59:59Z");game.complete(u,quest(u,Difficulty.EASY,Category.READING).id());
  assertEquals(1,game.character(u).streak());
  clock.now=Instant.parse("2026-03-09T00:00:01Z");game.complete(u,quest(u,Difficulty.EASY,Category.READING).id());
  assertNull(game.character(u).pendingTimezone());assertEquals("Pacific/Kiritimati",game.character(u).timezone());
  // The last event is remapped to the new zone; crossing zones cannot manufacture a new date.
  assertEquals(1,game.character(u).streak());
  clock.now=Instant.parse("2026-03-09T10:00:01Z");game.complete(u,quest(u,Difficulty.EASY,Category.READING).id());
  assertEquals(2,game.character(u).streak());
  clock.now=Instant.parse("2026-03-11T10:00:01Z");assertEquals(0,game.character(u).streak());
  game.complete(u,quest(u,Difficulty.EASY,Category.READING).id());assertEquals(1,game.character(u).streak());assertEquals(2,game.character(u).bestStreak());
 }
 @Test void allAttributesAndHistoryPagination() {
  long u=user();for(var c:Category.values())game.complete(u,quest(u,Difficulty.MEDIUM,c).id());
  var h=game.character(u);assertEquals(150,h.progress().total());assertEquals(50,h.progress().current());
  for(var a:h.attributes())assertEquals(a.type().equals("INTELLECT")?50:25,a.progress().total());
  assertEquals(2,game.activity(u,0,2).content().size());assertEquals(3,game.activity(u,0,2).totalPages());
  assertEquals(6,game.quests(u,"completed",null,"newest",0,50).totalElements());
 }
 @Test void httpSessionExpiryCsrfAndPersistence() throws Exception {
  var manager=new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL);
  var client=java.net.http.HttpClient.newBuilder().cookieHandler(manager).build();
  String base="http://localhost:"+port+"/api";
  var tokenRequest=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/auth/csrf")).GET().build();
  var tokenResponse=client.send(tokenRequest,java.net.http.HttpResponse.BodyHandlers.ofString());
  var token=json.readTree(tokenResponse.body());
  String oldCookie=manager.getCookieStore().getCookies().stream().filter(c->c.getName().equals("SESSION")).findFirst().orElseThrow().getValue();
  String email="http-"+UUID.randomUUID()+"@example.com";
  String body=json.writeValueAsString(new Signup("HTTP tester",email,password(),"UTC"));
  var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/auth/signup"))
    .header("Content-Type","application/json").header(token.get("headerName").asText(),token.get("token").asText())
    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
  var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
  assertEquals(201,response.statusCode(),response.body());
  String cookie=manager.getCookieStore().getCookies().stream().filter(c->c.getName().equals("SESSION")).findFirst().orElseThrow().getValue();
  assertNotEquals(oldCookie,cookie);
  String principal=json.readTree(response.body()).get("id").asText();
  assertEquals(1,jdbc.queryForObject("select count(*) from SPRING_SESSION where PRINCIPAL_NAME=?",Integer.class,principal));
  var me=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/auth/me")).GET().build();
  assertEquals(200,client.send(me,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
  var forbidden=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/tasks")).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}")).build();
  assertEquals(403,client.send(forbidden,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
  jdbc.update("update SPRING_SESSION set LAST_ACCESS_TIME=1,EXPIRY_TIME=1 where PRINCIPAL_NAME=?",principal);
  assertEquals(401,client.send(me,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
 }
}

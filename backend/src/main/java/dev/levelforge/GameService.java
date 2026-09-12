package dev.levelforge;
import dev.levelforge.Models.*;
import dev.levelforge.Repositories.*;
import dev.levelforge.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.*;
import java.util.*;
@Service
@Transactional
public class GameService {
    private final Clock clock;
    private final Users users; private final Heroes heroes; private final Attributes attributes; private final Tasks tasks;
    private final Completions completions; private final Days days; private final Items items; private final Inventory inventory; private final Ledgers ledgers;
    GameService(Users u,Heroes h,Attributes a,Tasks t,Completions c,Days d,Items i,Inventory v,Ledgers l,Clock clock){users=u;heroes=h;attributes=a;tasks=t;completions=c;days=d;items=i;inventory=v;ledgers=l;this.clock=clock;}
    private Hero lock(long user){return heroes.lock(user).orElseThrow(ApiError::missing);}
    private Task task(long user,long id){return tasks.findByIdAndUserId(id,user).orElseThrow(ApiError::missing);}
    private static PageRequest page(int page,int size,Sort sort){return PageRequest.of(Math.max(0,page),Math.clamp(size,1,50),sort);}
    private List<ItemView> catalog(Hero h) {
        Map<Long,Owned> owned=new HashMap<>();inventory.findByCharacterId(h.id).forEach(o->owned.put(o.itemId,o));
        return items.findAll(Sort.by("id")).stream().map(i->{Owned o=owned.get(i.id);return new ItemView(i.id,i.name,i.slot,i.appearance,i.description,i.price,o==null?null:o.id,o!=null,o!=null&&o.equipped);}).toList();
    }
    private HeroView view(Hero h) {
        // While a timezone transition waits, its old day window stays authoritative.
        Instant now=clock.instant();
        ZoneId zone=Rules.zone(h.timezone);
        LocalDate today=now.atZone(zone).toLocalDate();
        int visible=h.dayWindowEnd!=null&&now.isBefore(h.dayWindowEnd)?h.streak:Rules.visibleStreak(h.lastActivityDate,today,h.streak);
        return new HeroView(users.findById(h.userId).orElseThrow(ApiError::missing).displayName,h.avatar,Rules.progress(h.totalXp),h.gold,visible,h.bestStreak,h.timezone,h.pendingTimezone,
            attributes.findByCharacterId(h.id).stream().map(a->new AttributeView(a.type,Rules.progress(a.xp))).toList(),catalog(h).stream().filter(ItemView::equipped).toList());
    }
    @Transactional(readOnly=true) public HeroView character(long user){return view(heroes.findByUserId(user).orElseThrow(ApiError::missing));}
    public HeroView profile(long user,Profile input) {
        Hero h=lock(user);Rules.zone(input.timezone());
        User u=users.findById(user).orElseThrow(ApiError::missing);u.displayName=input.displayName().trim();
        if(input.avatar()!=null)h.avatar=input.avatar();
        if(input.timezone().equals(h.timezone)) h.pendingTimezone=null;
        else if(h.dayWindowEnd!=null&&clock.instant().isBefore(h.dayWindowEnd))h.pendingTimezone=input.timezone();
        else {h.timezone=input.timezone();h.pendingTimezone=null;if(h.lastActivityAt!=null)h.lastActivityDate=h.lastActivityAt.atZone(Rules.zone(h.timezone)).toLocalDate();}
        return view(h);
    }
    @Transactional(readOnly=true) public PageView<Quest> quests(long user,String status,Category category,String sort,int p,int size) {
        Specification<Task> spec=(root,q,cb)->cb.and(cb.equal(root.get("userId"),user),cb.equal(root.get("archived"),"archived".equals(status)));
        if("pending".equals(status)||"completed".equals(status)) spec=spec.and((root,q,cb)->cb.equal(root.get("completed"),"completed".equals(status)));
        if(category!=null)spec=spec.and((root,q,cb)->cb.equal(root.get("category"),category));
        Sort order="due".equals(sort)?Sort.by("dueDate").ascending().and(Sort.by("id").descending()):Sort.by("id").descending();
        var result=tasks.findAll(spec,page(p,size,order));return new PageView<>(result.map(Quest::of).getContent(),result.getNumber(),result.getTotalPages(),result.getTotalElements());
    }
    @Transactional(readOnly=true) public Quest quest(long user,long id){return Quest.of(task(user,id));}
    private void update(Task t,QuestInput input){t.title=input.title().trim();t.description=input.description()==null?"":input.description().trim();t.category=input.category();t.difficulty=input.difficulty();t.dueDate=input.dueDate();t.updatedAt=Instant.now();}
    public Quest create(long user,QuestInput input){lock(user);Task t=new Task();t.userId=user;update(t,input);return Quest.of(tasks.save(t));}
    public Quest edit(long user,long id,QuestInput input){lock(user);Task t=task(user,id);if(t.completed||t.archived)throw new ApiError(409,"READ_ONLY","Only pending quests can be edited.");update(t,input);return Quest.of(t);}
    public void archive(long user,long id){lock(user);Task t=task(user,id);t.archived=true;t.updatedAt=Instant.now();}
    public CompletionResult complete(long user,long id) {
        // All mutations acquire the character first. This serializes a user's economy across tabs.
        Hero h=lock(user);Task t=task(user,id);
        if(t.completed)return new CompletionResult(false,0,0,false,view(h));
        if(t.archived)throw new ApiError(409,"ARCHIVED","Archived quests cannot be completed.");
        int xp=t.difficulty.xp,gold=t.difficulty.gold,oldLevel=Rules.progress(h.totalXp).level();
        if(h.totalXp>Rules.MAX_XP-xp||h.gold>Rules.MAX_XP-gold)throw new ApiError(409,"PROGRESSION_LIMIT","Your character reached the supported progression limit.");
        Instant now=clock.instant();
        boolean sameWindow=h.dayWindowEnd!=null&&now.isBefore(h.dayWindowEnd);
        if(!sameWindow && h.pendingTimezone!=null){h.timezone=h.pendingTimezone;h.pendingTimezone=null;if(h.lastActivityAt!=null)h.lastActivityDate=h.lastActivityAt.atZone(Rules.zone(h.timezone)).toLocalDate();}
        ZoneId zone=Rules.zone(h.timezone);LocalDate today=now.atZone(zone).toLocalDate();
        if(!sameWindow) {
            h.streak=Rules.nextStreak(h.lastActivityDate,today,h.streak);h.bestStreak=Math.max(h.bestStreak,h.streak);
            ActivityDay day=new ActivityDay();day.characterId=h.id;day.localDate=today;day.timezone=h.timezone;
            Instant localStart=today.atStartOfDay(zone).toInstant();
            day.windowStart=h.dayWindowEnd!=null&&h.dayWindowEnd.isAfter(localStart)?h.dayWindowEnd:localStart;
            day.windowEnd=today.plusDays(1).atStartOfDay(zone).toInstant();day.createdAt=now;days.save(day);
            h.dayWindowEnd=day.windowEnd;h.lastActivityDate=today;
        }
        h.lastActivityAt=now;t.completed=true;t.completedAt=now;t.updatedAt=now;h.totalXp+=xp;h.gold+=gold;
        Attribute attr=attributes.findByCharacterId(h.id).stream().filter(a->a.type.equals(t.category.attribute())).findFirst().orElseThrow();attr.xp+=xp;
        Completion c=new Completion();c.taskId=t.id;c.userId=user;c.title=t.title;c.category=t.category.name();c.xp=xp;c.gold=gold;c.completedAt=now;c.localDate=today;c.timezone=h.timezone;completions.save(c);
        ledger(user,"QUEST",t.title,xp,gold,t.id,null);
        return new CompletionResult(true,xp,gold,Rules.progress(h.totalXp).level()>oldLevel,view(h));
    }
    private void ledger(long user,String kind,String description,long xp,long gold,Long task,Long item){Ledger l=new Ledger();l.userId=user;l.kind=kind;l.description=description;l.xp=xp;l.gold=gold;l.taskId=task;l.itemId=item;ledgers.save(l);}
    @Transactional(readOnly=true) public List<ItemView> shop(long user){return catalog(heroes.findByUserId(user).orElseThrow(ApiError::missing));}
    @Transactional(readOnly=true) public List<ItemView> inventory(long user){return shop(user).stream().filter(ItemView::owned).toList();}
    public PurchaseResult purchase(long user,long itemId) {
        Hero h=lock(user);Item i=items.findById(itemId).orElseThrow(ApiError::missing);
        if(inventory.findByCharacterIdAndItemId(h.id,itemId).isPresent())return new PurchaseResult(false,view(h));
        if(h.gold<i.price)throw new ApiError(409,"INSUFFICIENT_GOLD","Complete more quests to earn enough Gold.");
        h.gold-=i.price;Owned o=new Owned();o.characterId=h.id;o.itemId=i.id;inventory.save(o);ledger(user,"PURCHASE",i.name,0,-i.price,null,i.id);return new PurchaseResult(true,view(h));
    }
    public HeroView equip(long user,long id,boolean equipped) {
        Hero h=lock(user);List<Owned> owned=inventory.findByCharacterId(h.id);
        Owned target=owned.stream().filter(o->o.id==id).findFirst().orElseThrow(ApiError::missing);
        Map<Long,String> slots=new HashMap<>();items.findAll().forEach(i->slots.put(i.id,i.slot));
        if(equipped)owned.stream().filter(o->slots.get(o.itemId).equals(slots.get(target.itemId))).forEach(o->o.equipped=false);
        target.equipped=equipped;return view(h);
    }
    @Transactional(readOnly=true) public PageView<Activity> activity(long user,int p,int size) {
        var result=ledgers.findByUserId(user,page(p,size,Sort.by("id").descending()));
        return new PageView<>(result.map(l->new Activity(l.id,l.kind,l.description,l.xp,l.gold,l.createdAt)).getContent(),result.getNumber(),result.getTotalPages(),result.getTotalElements());
    }
}

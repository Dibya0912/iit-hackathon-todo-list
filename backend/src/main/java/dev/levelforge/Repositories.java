package dev.levelforge;
import dev.levelforge.Models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public final class Repositories {
    public interface Users extends JpaRepository<User,Long> { Optional<User> findByEmail(String email); }
    public interface Heroes extends JpaRepository<Hero,Long> {
        Optional<Hero> findByUserId(Long userId);
        @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select h from Models$Hero h where h.userId=:userId")
        Optional<Hero> lock(@Param("userId") Long userId);
    }
    public interface Attributes extends JpaRepository<Attribute,Long> { List<Attribute> findByCharacterId(Long id); }
    public interface Tasks extends JpaRepository<Task,Long>, JpaSpecificationExecutor<Task> { Optional<Task> findByIdAndUserId(Long id,Long userId); }
    public interface Completions extends JpaRepository<Completion,Long> {}
    public interface Days extends JpaRepository<ActivityDay,Long> {}
    public interface Items extends JpaRepository<Item,Long> {}
    public interface Inventory extends JpaRepository<Owned,Long> { List<Owned> findByCharacterId(Long id); Optional<Owned> findByCharacterIdAndItemId(Long characterId,Long itemId); }
    public interface Ledgers extends JpaRepository<Ledger,Long> { Page<Ledger> findByUserId(Long userId,Pageable page); }
}


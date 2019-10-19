package it.niedermann.nextcloud.deck.persistence.sync.adapters.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import it.niedermann.nextcloud.deck.model.User;

@Dao
public interface UserDao extends GenericDao<User> {

    @Query("SELECT * FROM user WHERE accountId = :accountId")
    LiveData<List<User>> getUsersForAccount(final long accountId);

    @Query("SELECT * FROM user WHERE accountId = :accountId and localId = :localId")
    LiveData<User> getUserByLocalId(final long accountId, final long localId);

    @Query("SELECT * FROM user WHERE accountId = :accountId and uid = :uid")
    LiveData<User> getUserByUid(final long accountId, final String uid);

    @Query("SELECT * FROM user WHERE accountId = :accountId and ( uid LIKE :searchTerm or displayname LIKE :searchTerm or primaryKey LIKE :searchTerm )")
    LiveData<List<User>> searchUserByUidOrDisplayName(final long accountId, final String searchTerm);

    @Query("SELECT * FROM user WHERE accountId = :accountId and uid = :uid")
    User getUserByUidDirectly(final long accountId, final String uid);

    @Query("SELECT * FROM user WHERE localId IN (:assignedUserIDs) and status <> 3") // not LOCAL_DELETED
    List<User> getUsersByIdDirectly(List<Long> assignedUserIDs);

    @Query("SELECT * FROM user WHERE localId = :localUserId")
    User getUserByLocalIdDirectly(long localUserId);

    @Query("    SELECT u.* FROM user u" +
            "    WHERE u.accountId = :accountId" +
            "    AND NOT EXISTS (" +
            "            select 1 from joincardwithuser ju" +
            "            where ju.userId = u.localId" +
            "            and ju.cardId = :notAssignedToLocalCardId" +
            "    )" +
            "    AND" +
            "            (" +
            "                    EXISTS (" +
            "                    select 1 from accesscontrol" +
            "                    where userId = u.localId and boardId = :boardId" +
            "            )" +
            "    OR" +
            "    EXISTS (" +
            "            select 1 from board where localId = :boardId AND ownerId = u.localId" +
            "    )" +
            ")" +
            "    ORDER BY (" +
            "            select count(*) from joincardwithuser j" +
            "    where userId = u.localId and cardId in (select c.localId from card c inner join stack s on s.localId = c.stackId where s.boardId = :boardId)" +
            ") DESC" +
            "    LIMIT :topX")
    LiveData<List<User>> findProposalsForUsersToAssign(long accountId, long boardId, long notAssignedToLocalCardId, int topX);
}
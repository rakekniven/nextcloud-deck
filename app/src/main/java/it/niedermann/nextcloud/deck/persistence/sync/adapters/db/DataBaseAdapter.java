package it.niedermann.nextcloud.deck.persistence.sync.adapters.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

import it.niedermann.nextcloud.deck.model.AccessControl;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Attachment;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Card;
import it.niedermann.nextcloud.deck.model.JoinBoardWithLabel;
import it.niedermann.nextcloud.deck.model.JoinCardWithLabel;
import it.niedermann.nextcloud.deck.model.JoinCardWithUser;
import it.niedermann.nextcloud.deck.model.Label;
import it.niedermann.nextcloud.deck.model.Stack;
import it.niedermann.nextcloud.deck.model.User;
import it.niedermann.nextcloud.deck.model.enums.DBStatus;
import it.niedermann.nextcloud.deck.model.full.FullBoard;
import it.niedermann.nextcloud.deck.model.full.FullCard;
import it.niedermann.nextcloud.deck.model.full.FullStack;
import it.niedermann.nextcloud.deck.model.interfaces.AbstractRemoteEntity;
import it.niedermann.nextcloud.deck.model.ocs.Activity;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.db.util.LiveDataHelper;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.db.util.WrappedLiveData;

public class DataBaseAdapter {

    private DeckDatabase db;

    public DataBaseAdapter(Context applicationContext) {
        this.db = DeckDatabase.getInstance(applicationContext);
    }

    private <T extends AbstractRemoteEntity> void markAsEditedIfNeeded(T entity, boolean setStatus) {
        if (!setStatus) return;
        entity.setStatusEnum(DBStatus.LOCAL_EDITED);
        entity.setLastModifiedLocal(new Date()); // now.
    }

    private <T extends AbstractRemoteEntity> void markAsDeletedIfNeeded(T entity, boolean setStatus) {
        if (!setStatus) return;
        entity.setStatusEnum(DBStatus.LOCAL_DELETED);
        entity.setLastModifiedLocal(new Date()); // now.
    }

    public LiveData<Boolean> hasAccounts(){
        return LiveDataHelper.postCustomValue(db.getAccountDao().countAccounts(), data -> data != null && data > 0);
    }

    public LiveData<Board> getBoard(long accountId, long remoteId) {
        return LiveDataHelper.onlyIfChanged(db.getBoardDao().getBoardByRemoteId(accountId, remoteId));
    }
    public Board getBoardByRemoteIdDirectly(long accountId, long remoteId) {
        return db.getBoardDao().getBoardByRemoteIdDirectly(accountId, remoteId);
    }
    public FullBoard getFullBoardByRemoteIdDirectly(long accountId, long remoteId) {
        return db.getBoardDao().getFullBoardByRemoteIdDirectly(accountId, remoteId);
    }

    public FullBoard getFullBoardByLocalIdDirectly(long accountId, long localId) {
        return db.getBoardDao().getFullBoardByLocalIdDirectly(accountId, localId);
    }

    public LiveData<Stack> getStackByRemoteId(long accountId, long localBoardId, long remoteId) {
        return LiveDataHelper.onlyIfChanged(db.getStackDao().getStackByRemoteId(accountId, localBoardId, remoteId));
    }

    public Stack getStackByLocalIdDirectly(final long localStackId) {
        return db.getStackDao().getStackByLocalIdDirectly(localStackId);
    }

    public FullStack getFullStackByLocalIdDirectly(final long localStackId) {
        return db.getStackDao().getFullStackByLocalIdDirectly(localStackId);
    }


    public FullStack getFullStackByRemoteIdDirectly(long accountId, long localBoardId, long remoteId) {
        return db.getStackDao().getFullStackByRemoteIdDirectly(accountId, localBoardId, remoteId);
    }

    public LiveData<Card> getCardByRemoteID(long accountId, long remoteId) {
        return LiveDataHelper.onlyIfChanged(db.getCardDao().getCardByRemoteId(accountId, remoteId));
    }

    public FullCard getFullCardByRemoteIdDirectly(long accountId, long remoteId) {
        FullCard card = db.getCardDao().getFullCardByRemoteIdDirectly(accountId, remoteId);
        readRelationsForCard(card);
        return card;
    }
    public FullCard getFullCardByLocalIdDirectly(long accountId, long localId) {
        return db.getCardDao().getFullCardByLocalIdDirectly(accountId, localId);
    }

    private void readRelationsForCard(FullCard card) {
        if (card != null){
            if (card.getLabelIDs() != null && !card.getLabelIDs().isEmpty()){
                List<Long> filteredIDs = db.getJoinCardWithLabelDao().filterDeleted(card.getLocalId(), card.getLabelIDs());
                card.setLabels(db.getLabelDao().getLabelsByIdsDirectly(filteredIDs));
            }
            if (card.getAssignedUserIDs() != null && !card.getAssignedUserIDs().isEmpty()){
                card.setAssignedUsers(db.getUserDao().getUsersByIdDirectly(card.getAssignedUserIDs()));
            }
        }
    }

    private void readRelationsForCard(List<FullCard> card) {
        if (card == null){
            return;
        }
        for (FullCard c : card) {
            readRelationsForCard(c);
        }
    }

    public Card getCardByRemoteIdDirectly(long accountId, long remoteId) {
        return db.getCardDao().getCardByRemoteIdDirectly(accountId, remoteId);
    }

    public LiveData<List<FullCard>> getFullCardsForStack(long accountId, long localStackId) {
        return LiveDataHelper.interceptLiveData(db.getCardDao().getFullCardsForStack(accountId, localStackId), this::readRelationsForCard);
    }

    public List<FullCard> getFullCardsForStackDirectly(long accountId, long localStackId) {
        return db.getCardDao().getFullCardsForStackDirectly(accountId, localStackId);
    }

    public User getUserByUidDirectly(long accountId, String uid) {
        return db.getUserDao().getUserByUidDirectly(accountId, uid);
    }

    public long createUser(long accountId, User user) {
        user.setAccountId(accountId);
        return db.getUserDao().insert(user);
    }

    public void updateUser(long accountId, User user, boolean setStatus) {
        markAsEditedIfNeeded(user, setStatus);
        user.setAccountId(accountId);
        db.getUserDao().update(user);
    }

    public LiveData<Label> getLabelByRemoteId(long accountId, long remoteId) {
        return LiveDataHelper.onlyIfChanged(db.getLabelDao().getLabelByRemoteId(accountId, remoteId));
    }

    public Label getLabelByRemoteIdDirectly(long accountId, long remoteId) {
        return db.getLabelDao().getLabelByRemoteIdDirectly(accountId, remoteId);
    }

    public long createLabel(long accountId, Label label) {
        label.setAccountId(accountId);
        return db.getLabelDao().insert(label);
    }

    public void createJoinCardWithLabel(long localLabelId, long localCardId) {
        createJoinCardWithLabel(localLabelId, localCardId, DBStatus.UP_TO_DATE);
    }

    public void createJoinCardWithLabel(long localLabelId, long localCardId, DBStatus status) {
        JoinCardWithLabel join = new JoinCardWithLabel();
        join.setCardId(localCardId);
        join.setLabelId(localLabelId);
        join.setStatus(status.getId());
        db.getJoinCardWithLabelDao().insert(join);
    }

    public void deleteJoinedLabelsForCard(long localCardId) {
        db.getJoinCardWithLabelDao().deleteByCardId(localCardId);
    }
    public void deleteJoinedLabelForCard(long localCardId, long localLabelId) {
        db.getJoinCardWithLabelDao().setDbStatus(localCardId, localLabelId, DBStatus.LOCAL_DELETED.getId());
    }

    public void deleteJoinedUserForCard(long localCardId, long localUserId) {
        db.getJoinCardWithUserDao().setDbStatus(localCardId, localUserId, DBStatus.LOCAL_DELETED.getId());
    }

    public void deleteJoinedLabelForCardPhysically(long localCardId, long localLabelId) {
        db.getJoinCardWithLabelDao().deleteByCardIdAndLabelId(localCardId, localLabelId);
    }
    public void deleteJoinedUserForCardPhysically(long localCardId, long localUserId) {
        db.getJoinCardWithUserDao().deleteByCardIdAndUserIdPhysically(localCardId, localUserId);
    }

    public void createJoinCardWithUser(long localUserId, long localCardId) {
        createJoinCardWithUser(localUserId, localCardId, DBStatus.UP_TO_DATE);
    }

    public void createJoinCardWithUser(long localUserId, long localCardId, DBStatus status) {
        JoinCardWithUser join = new JoinCardWithUser();
        join.setCardId(localCardId);
        join.setUserId(localUserId);
        join.setStatus(status.getId());
        db.getJoinCardWithUserDao().insert(join);
    }

    public void deleteJoinedUsersForCard(long localCardId) {
        db.getJoinCardWithUserDao().deleteByCardId(localCardId);
    }

    public void createJoinBoardWithLabel(long localBoardId, long localLabelId) {
        JoinBoardWithLabel join = new JoinBoardWithLabel();
        join.setBoardId(localBoardId);
        join.setLabelId(localLabelId);
        db.getJoinBoardWithLabelDao().insert(join);
    }

    public void deleteJoinedLabelsForBoard(Long localBoardId) {
        db.getJoinBoardWithLabelDao().deleteByBoardId(localBoardId);
    }
    
    public void updateLabel(Label label, boolean setStatus) {
        markAsEditedIfNeeded(label, setStatus);
        db.getLabelDao().update(label);
    }

    public void deleteLabel(Label label, boolean setStatus) {
        markAsDeletedIfNeeded(label, setStatus);
        db.getLabelDao().update(label);
    }
    public void deleteLabelPhysically(Label label) {
        db.getLabelDao().delete(label);
    }

    public WrappedLiveData<Account> createAccount(Account account) {
        return LiveDataHelper.wrapInLiveData(() -> {
            long id = db.getAccountDao().insert(account);
            return readAccountDirectly(id);

        });
    }

    public void deleteAccount(long id) {
        db.getAccountDao().deleteById(id);
    }

    public void updateAccount(Account account) {
        db.getAccountDao().update(account);
    }

    public LiveData<Account> readAccount(long id) {
        return LiveDataHelper.onlyIfChanged(db.getAccountDao().selectById(id));
    }

    public Account readAccountDirectly(long id) {
        return db.getAccountDao().getAccountByIdDirectly(id);
    }

    public LiveData<List<Account>> readAccounts() {
        return LiveDataHelper.onlyIfChanged(db.getAccountDao().selectAll());
    }

    public LiveData<List<Board>> getBoards(long accountId) {
        return LiveDataHelper.onlyIfChanged(db.getBoardDao().getBoardsForAccount(accountId));
    }

    public WrappedLiveData<Board> createBoard(long accountId, Board board) {
        return LiveDataHelper.wrapInLiveData(() -> {
            board.setAccountId(accountId);
            long id = db.getBoardDao().insert(board);
            return db.getBoardDao().getBoardByIdDirectly(id);

        });
    }

    public long createBoardDirectly(long accountId, Board board) {
            board.setAccountId(accountId);
            return db.getBoardDao().insert(board);
    }

    public void deleteBoard(Board board, boolean setStatus) {
        markAsDeletedIfNeeded(board, setStatus);
        db.getBoardDao().update(board);
    }

    public void deleteBoardPhysically(Board board) {
        db.getBoardDao().delete(board);
    }

    public void updateBoard(Board board, boolean setStatus) {
        markAsEditedIfNeeded(board, setStatus);
        db.getBoardDao().update(board);
    }

    public LiveData<List<FullStack>> getFullStacksForBoard(long accountId, long localBoardId) {
        return LiveDataHelper.onlyIfChanged(db.getStackDao().getFullStacksForBoard(accountId, localBoardId));
    }

    public List<FullStack> getFullStacksForBoardDirectly(long accountId, long localBoardId) {
        return db.getStackDao().getFullStacksForBoardDirectly(accountId, localBoardId);
    }

    public LiveData<FullStack> getStack(long accountId, long localStackId) {
        return LiveDataHelper.onlyIfChanged(db.getStackDao().getFullStack(accountId, localStackId));
    }
    
    public long createStack(long accountId, Stack stack) {
        stack.setAccountId(accountId);
        return db.getStackDao().insert(stack);
    }
    
    public void deleteStack(Stack stack, boolean setStatus) {
        markAsDeletedIfNeeded(stack, setStatus);
        db.getStackDao().update(stack);
    }

    public void deleteStackPhysically(Stack stack) {
        db.getStackDao().delete(stack);
    }

    public void updateStack(Stack stack, boolean setStatus) {
        markAsEditedIfNeeded(stack, setStatus);
        db.getStackDao().update(stack);
    }
    
    public Card  getCardByLocalIdDirectly(long accountId, long localCardId) {
        return db.getCardDao().getCardByLocalIdDirectly(accountId, localCardId);
    }

    public LiveData<FullCard>  getCardByLocalId(long accountId, long localCardId) {
        return LiveDataHelper.interceptLiveData(db.getCardDao().getFullCardByLocalId(accountId, localCardId), this::readRelationsForCard);
    }
    public List<FullCard> getLocallyChangedCardsDirectly(long accountId) {
        return db.getCardDao().getLocallyChangedCardsDirectly(accountId);
    }
    public List<FullCard> getLocallyChangedCardsByLocalStackIdDirectly(long accountId, long localStackId) {
        return db.getCardDao().getLocallyChangedCardsByLocalStackIdDirectly(accountId, localStackId);
    }

    public long createCard(long accountId, Card card) {
        card.setAccountId(accountId);
        return db.getCardDao().insert(card);
    }

    public void deleteCard(Card card, boolean setStatus) {
        markAsDeletedIfNeeded(card, setStatus);
        if (setStatus){
            db.getCardDao().update(card);
        } else {
            deleteCardPhysically(card);
        }
    }

    public void deleteCardPhysically(Card card) {
        db.getCardDao().delete(card);
    }

    public void updateCard(Card card, boolean setStatus) {
        markAsEditedIfNeeded(card, setStatus);
        db.getCardDao().update(card);
    }

    public long createAccessControl(long accountId, AccessControl entity) {
        entity.setAccountId(accountId);
        return db.getAccessControlDao().insert(entity);
    }

    public AccessControl getAccessControlByRemoteIdDirectly(long accountId, Long id) {
        return db.getAccessControlDao().getAccessControlByRemoteIdDirectly(accountId, id);
    }

    public void updateAccessControl(AccessControl entity, boolean setStatus) {
        markAsEditedIfNeeded(entity, setStatus);
        db.getAccessControlDao().update(entity);
    }

    public LiveData<FullBoard> getFullBoardById(Long accountId, Long localId) {
        return db.getBoardDao().getFullBoardById(accountId, localId);
    }
    public Board getBoardByLocalIdDirectly(long localId) {
        return db.getBoardDao().getBoardByIdDirectly(localId);
    }

    public LiveData<User> getUserByLocalId(long accountId, long localId){
        return db.getUserDao().getUserByLocalId(accountId, localId);
    }

    public LiveData<User> getUserByUid(long accountId, String uid){
        return db.getUserDao().getUserByUid(accountId, uid);
    }

    public LiveData<List<User>> getUsersForAccount(final long accountId){
        return db.getUserDao().getUsersForAccount(accountId);
    }
    public LiveData<List<User>> searchUserByUidOrDisplayName(final long accountId, final String searchTerm){
        validateSearchTerm(searchTerm);
        return db.getUserDao().searchUserByUidOrDisplayName(accountId, "%"+searchTerm.trim()+"%");
    }

    public LiveData<List<Label>> searchLabelByTitle(final long accountId, final long boardId, String searchTerm){
        validateSearchTerm(searchTerm);
        return db.getLabelDao().searchLabelByTitle(accountId, boardId,"%"+searchTerm.trim()+"%");
    }


    public Attachment getAttachmentByRemoteIdDirectly(long accountId, Long id) {
        return db.getAttachmentDao().getAttachmentByRemoteIdDirectly(accountId, id);
    }

    public long createAttachment(long accountId, Attachment attachment) {
        attachment.setAccountId(accountId);
        return db.getAttachmentDao().insert(attachment);
    }

    public void updateAttachment(long accountId, Attachment attachment, boolean setStatus) {
        markAsEditedIfNeeded(attachment, setStatus);
        attachment.setAccountId(accountId);
        db.getAttachmentDao().update(attachment);
    }

    private void validateSearchTerm(String searchTerm){
        // TODO @desperateCoder
        // Issue: https://github.com/stefan-niedermann/nextcloud-deck/issues/52
        // if searchTerm.length() === 0
        // then return a list (max. 3 entries) with the users / labels / whatever which are assigned most often, ordered by count of how often they are assigned to any cards and which are NOT yet assigned to this card.
        if (searchTerm == null || searchTerm.trim().length()<1) {
            throw new IllegalArgumentException("please provide a proper search term! \""+searchTerm+"\" doesn't seem right...");
        }
    }

    public Account getAccountByIdDirectly(long accountId) {
        return db.getAccountDao().getAccountByIdDirectly(accountId);
    }

    public User getUserByLocalIdDirectly(long localUserId) {
        return db.getUserDao().getUserByLocalIdDirectly(localUserId);
    }

    public void setStatusForJoinCardWithUser(long localCardId, long localUserId, int status) {
        db.getJoinCardWithUserDao().setDbStatus(localCardId, localUserId, status);
    }

    public void setStatusForJoinCardWithLabel(long localCardId, long localLabelId, int status) {
        db.getJoinCardWithLabelDao().setDbStatus(localCardId, localLabelId, status);
    }

    public Label getLabelByLocalIdDirectly(long localLabelId) {
        return db.getLabelDao().getLabelsByIdDirectly(localLabelId);
    }

    public List<FullBoard> getLocallyChangedBoards(long accountId) {
        return db.getBoardDao().getLocallyChangedBoardsDirectly(accountId);
    }
    public List<FullBoard> getAllFullBoards(long accountId) {
        return db.getBoardDao().getAllFullBoards(accountId);
    }

    public List<FullStack> getLocallyChangedStacksForBoard(long accountId, long localBoardId) {
        return db.getStackDao().getLocallyChangedStacksForBoardDirectly(accountId, localBoardId);
    }
    public List<FullStack> getLocallyChangedStacks(long accountId) {
        return db.getStackDao().getLocallyChangedStacksDirectly(accountId);
    }
    public List<Label> getLocallyChangedLabels(long accountId) {
        return db.getLabelDao().getLocallyChangedLabelsDirectly(accountId);
    }

    public Board getBoardByLocalCardIdDirectly(long localCardId) {
        return db.getBoardDao().getBoardByLocalCardIdDirectly(localCardId);
    }

    public JoinCardWithLabel getJoinCardWithLabel(Long localLabelId, Long localCardId) {
        return db.getJoinCardWithLabelDao().getJoin(localLabelId, localCardId);
    }

    public JoinCardWithUser getJoinCardWithUser(Long localUserId, Long localCardId) {
        return db.getJoinCardWithUserDao().getJoin(localUserId, localCardId);
    }

    public List<JoinCardWithLabel> getAllDeletedJoinsWithRemoteIDs() {
        return db.getJoinCardWithLabelDao().getAllDeletedJoinsWithRemoteIDs();
    }

    public List<JoinCardWithLabel> getAllDeletedJoins() {
        return db.getJoinCardWithLabelDao().getAllDeletedJoins();
    }
    public JoinCardWithLabel getRemoteIdsForJoin(Long localCardId, Long localLabelId) {
        return db.getJoinCardWithLabelDao().getRemoteIdsForJoin(localCardId, localLabelId);
    }

    public List<JoinCardWithUser> getAllDeletedUserJoinsWithRemoteIDs() {
        return db.getJoinCardWithUserDao().getDeletedJoinsWithRemoteIDs();
    }

    public void deleteJoinedLabelForCardPhysicallyByRemoteIDs(Long accountId, Long remoteCardId, Long remoteLabelId) {
        db.getJoinCardWithLabelDao().deleteJoinedLabelForCardPhysicallyByRemoteIDs(accountId, remoteCardId, remoteLabelId);
    }

    public void deleteJoinedUserForCardPhysicallyByRemoteIDs(Long accountId, Long remoteCardId, String userUid) {
        db.getJoinCardWithUserDao().deleteJoinedUserForCardPhysicallyByRemoteIDs(accountId, remoteCardId, userUid);
    }

    public LiveData<List<Activity>> getActivitiesForCard(Long localCardId) {
        return db.getActivityDao().getActivitiesForCard(localCardId);
    }

    public long createActivity(long accountId, Activity activity) {
        activity.setAccountId(accountId);
        return db.getActivityDao().insert(activity);
    }
    public Activity getActivityByRemoteIdDirectly(long accountId, long remoteActivityId) {
        return db.getActivityDao().getActivityByRemoteIdDirectly(accountId, remoteActivityId);
    }

    public void updateActivity(Activity activity, boolean setStatus) {
        markAsEditedIfNeeded(activity, setStatus);
        db.getActivityDao().update(activity);
    }

    public void deleteActivity(Activity activity) {
        db.getActivityDao().delete(activity);
    }

}

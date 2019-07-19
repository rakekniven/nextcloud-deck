package it.niedermann.nextcloud.deck.persistence.sync.helpers.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import it.niedermann.nextcloud.deck.api.IResponseCallback;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Attachment;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Card;
import it.niedermann.nextcloud.deck.model.JoinCardWithLabel;
import it.niedermann.nextcloud.deck.model.JoinCardWithUser;
import it.niedermann.nextcloud.deck.model.Label;
import it.niedermann.nextcloud.deck.model.User;
import it.niedermann.nextcloud.deck.model.enums.DBStatus;
import it.niedermann.nextcloud.deck.model.full.FullCard;
import it.niedermann.nextcloud.deck.model.full.FullStack;
import it.niedermann.nextcloud.deck.model.propagation.CardUpdate;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.ServerAdapter;
import it.niedermann.nextcloud.deck.persistence.sync.adapters.db.DataBaseAdapter;
import it.niedermann.nextcloud.deck.persistence.sync.helpers.SyncHelper;

public class CardDataProvider extends AbstractSyncDataProvider<FullCard> {

    protected Board board;
    protected FullStack stack;

    public CardDataProvider(AbstractSyncDataProvider<?> parent, Board board, FullStack stack) {
        super(parent);
        this.board = board;
        this.stack = stack;
    }

    @Override
    public void getAllFromServer(ServerAdapter serverAdapter, long accountId, IResponseCallback<List<FullCard>> responder, Date lastSync) {

        List<FullCard> result = new ArrayList<>();
        if (stack.getCards() == null || stack.getCards().isEmpty()){
            responder.onResponse(result);
        }
        for (Card card : stack.getCards()) {
            serverAdapter.getCard(board.getId(), stack.getId(), card.getId(), new IResponseCallback<FullCard>(responder.getAccount()) {
                @Override
                public void onResponse(FullCard response) {
                    result.add(response);
                    if (result.size() == stack.getCards().size()) {
                        responder.onResponse(result);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    responder.onError(throwable);
                }
            });
        }
    }

    @Override
    public FullCard getSingleFromDB(DataBaseAdapter dataBaseAdapter, long accountId, FullCard entity) {
        return dataBaseAdapter.getFullCardByRemoteIdDirectly(accountId, entity.getEntity().getId());
    }

    @Override
    public long createInDB(DataBaseAdapter dataBaseAdapter, long accountId, FullCard entity) {
        fixRelations(dataBaseAdapter, accountId, entity);
        return dataBaseAdapter.createCard(accountId, entity.getCard());
    }

    protected CardUpdate toCardUpdate(FullCard card){
        CardUpdate c = new CardUpdate(card);
        c.setOwner(card.getOwner().get(0));
        return c;
    }

    protected void fixRelations(DataBaseAdapter dataBaseAdapter, long accountId, FullCard entity) {
        entity.getCard().setStackId(stack.getLocalId());
        if (entity.getOwner() != null && !entity.getOwner().isEmpty()){
            User user = entity.getOwner().get(0);
            User u = dataBaseAdapter.getUserByUidDirectly(accountId, user.getUid());
            if (u == null){
                dataBaseAdapter.createUser(accountId, user);
            } else {
                user.setLocalId(u.getLocalId());
                dataBaseAdapter.updateUser(accountId, user, false);
            }
            u = dataBaseAdapter.getUserByUidDirectly(accountId, user.getUid());

            user.setLocalId(u.getLocalId());
            entity.getCard().setUserId(u.getLocalId());
        }
    }

    @Override
    public void updateInDB(DataBaseAdapter dataBaseAdapter, long accountId, FullCard entity) {
        fixRelations(dataBaseAdapter, accountId, entity);
        dataBaseAdapter.updateCard(entity.getCard(), false);
    }

    @Override
    public void goDeeper(SyncHelper syncHelper, FullCard existingEntity, FullCard entityFromServer, IResponseCallback<Boolean> callback) {
        List<Label> labels = entityFromServer.getLabels();
        existingEntity.setLabels(labels);
        List<User> assignedUsers = entityFromServer.getAssignedUsers();
        existingEntity.setAssignedUsers(assignedUsers);
        List<Attachment> attachments = entityFromServer.getAttachments();
        existingEntity.setAttachments(attachments);

        syncHelper.fixRelations(new CardLabelRelationshipProvider(existingEntity.getCard(), existingEntity.getLabels()));
        if(assignedUsers != null && !assignedUsers.isEmpty()){
            syncHelper.doSyncFor(new UserDataProvider(this, board, stack, existingEntity, existingEntity.getAssignedUsers()));
        }

        syncHelper.fixRelations(new CardUserRelationshipProvider(existingEntity.getCard(), existingEntity.getAssignedUsers()));
        if(attachments != null && !attachments.isEmpty()){
            syncHelper.doSyncFor(new AttachmentDataProvider(this, existingEntity, attachments));
        }
    }

    @Override
    public void createOnServer(ServerAdapter serverAdapter, DataBaseAdapter dataBaseAdapter, long accountId, IResponseCallback<FullCard> responder, FullCard entity) {
        serverAdapter.createCard(board.getId(), stack.getId(), entity.getCard(), responder);
    }

    @Override
    public void updateOnServer(ServerAdapter serverAdapter, DataBaseAdapter dataBaseAdapter, long accountId, IResponseCallback<FullCard> callback, FullCard entity) {
        CardUpdate update = toCardUpdate(entity);
        update.setStackId(stack.getId());
        serverAdapter.updateCard(board.getId(), stack.getId(), update, callback);
    }

    @Override
    public void deleteInDB(DataBaseAdapter dataBaseAdapter, long accountId, FullCard fullCard) {
        dataBaseAdapter.deleteCard(fullCard.getCard(), false);
    }

    @Override
    public void deleteOnServer(ServerAdapter serverAdapter, long accountId, IResponseCallback<Void> callback, FullCard entity, DataBaseAdapter dataBaseAdapter) {
        serverAdapter.deleteCard(board.getId(), stack.getId(), entity.getCard(), callback);
    }

    @Override
    public List<FullCard> getAllChangedFromDB(DataBaseAdapter dataBaseAdapter, long accountId, Date lastSync) {
        if (board == null || stack == null){
            // no cards changed!
            // (see call from StackDataProvider: goDeeperForUpSync called with null for board.)
            // so we can just skip this one and proceed with anything else (users, labels).
            return Collections.emptyList();
        }
        return dataBaseAdapter.getLocallyChangedCardsByLocalStackIdDirectly(accountId, stack.getStack().getLocalId());
    }

    @Override
    public void goDeeperForUpSync(SyncHelper syncHelper, ServerAdapter serverAdapter, DataBaseAdapter dataBaseAdapter, IResponseCallback<Boolean> callback) {
        FullStack stack;
        Board board;


        List<JoinCardWithLabel> deletedLabels = dataBaseAdapter.getAllDeletedLabelJoinsByStackWithRemoteIDs();
        Account account = callback.getAccount();
        for (JoinCardWithLabel deletedLabel : deletedLabels) {
            Card card = dataBaseAdapter.getCardByRemoteIdDirectly(account.getId(), deletedLabel.getCardId());
            if (this.stack == null){
                stack = dataBaseAdapter.getFullStackByLocalIdDirectly(card.getLocalId());
            } else {
                stack = this.stack;
            }

            if (this.board == null) {
                board = dataBaseAdapter.getBoardByLocalIdDirectly(stack.getStack().getBoardId());
            } else {
                board = this.board;
            }

            if (deletedLabel.getStatusEnum() == DBStatus.LOCAL_DELETED){
                serverAdapter.unassignLabelFromCard(board.getId(), stack.getId(), deletedLabel.getCardId(), deletedLabel.getLabelId(), new IResponseCallback<Void>(account) {
                    @Override
                    public void onResponse(Void response) {
                        dataBaseAdapter.deleteJoinedLabelForCardPhysicallyByRemoteIDs(account.getId(), deletedLabel.getCardId(), deletedLabel.getLabelId());
                    }
                });
            } else if (deletedLabel.getStatusEnum() == DBStatus.LOCAL_EDITED){
                serverAdapter.assignLabelToCard(board.getId(), stack.getId(), deletedLabel.getCardId(), deletedLabel.getLabelId(), new IResponseCallback<Void>(account) {
                    @Override
                    public void onResponse(Void response) {
                        Label label = dataBaseAdapter.getLabelByRemoteIdDirectly(account.getId(), deletedLabel.getLabelId());
                        dataBaseAdapter.setStatusForJoinCardWithLabel(card.getLocalId(), label.getLocalId(), DBStatus.UP_TO_DATE.getId());
                    }
                });
            }
        }
        List<JoinCardWithUser> deletedUsers = dataBaseAdapter.getAllDeletedUserJoinsWithRemoteIDs();
        for (JoinCardWithUser deletedUser : deletedUsers) {
            Card card = dataBaseAdapter.getCardByRemoteIdDirectly(account.getId(), deletedUser.getCardId());
            if (this.stack == null){
                stack = dataBaseAdapter.getFullStackByLocalIdDirectly(card.getLocalId());
            } else {
                stack = this.stack;
            }

            if (this.board == null) {
                board = dataBaseAdapter.getBoardByLocalIdDirectly(stack.getStack().getBoardId());
            } else {
                board = this.board;
            }
            User user = dataBaseAdapter.getUserByLocalIdDirectly(deletedUser.getUserId());
            if (deletedUser.getStatusEnum() == DBStatus.LOCAL_DELETED){
                serverAdapter.unassignUserFromCard(board.getId(), stack.getId(), deletedUser.getCardId(), user.getUid(), new IResponseCallback<Void>(account) {
                    @Override
                    public void onResponse(Void response) {
                        dataBaseAdapter.deleteJoinedUserForCardPhysicallyByRemoteIDs(account.getId(), deletedUser.getCardId(), user.getUid());
                    }
                });
            } else if (deletedUser.getStatusEnum() == DBStatus.LOCAL_EDITED){
                serverAdapter.assignUserToCard(board.getId(), stack.getId(), deletedUser.getCardId(), user.getUid(), new IResponseCallback<Void>(account) {
                    @Override
                    public void onResponse(Void response) {
                        dataBaseAdapter.setStatusForJoinCardWithUser(card.getLocalId(), user.getLocalId(), DBStatus.UP_TO_DATE.getId());
                    }
                });
            }
        }
        callback.onResponse(Boolean.TRUE);
    }
}

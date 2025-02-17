package it.niedermann.nextcloud.deck.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import it.niedermann.nextcloud.deck.model.enums.DBStatus;
import it.niedermann.nextcloud.deck.model.interfaces.AbstractRemoteEntity;

@Entity(inheritSuperIndices = true,
    indices = {
        @Index(value = "accountId", name = "card_accID"),
        @Index("stackId")
    },
    foreignKeys = {
        @ForeignKey(
            entity = Stack.class,
            parentColumns = "localId",
            childColumns = "stackId", onDelete = ForeignKey.CASCADE
        )
    }
)
public class Card extends AbstractRemoteEntity {

    private String title;
    private String description;
    @NonNull
    private long stackId;
    private String type;
    private Date createdAt;
    private Date deletedAt;
    private int attachmentCount;

    private Long userId;
    @NonNull
    private int order;
    private boolean archived;
    @SerializedName("duedate")
    private Date dueDate;
    private boolean notified;
    private int overdue;
    private int commentsUnread;

    public Card() {}

    @Ignore
    public Card(String title, String description, long stackId) {
        this.title = title;
        this.description = description;
        this.stackId = stackId;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DBStatus getStatusEnum() {
        return DBStatus.findById(status);
    }

    public void setStatusEnum(DBStatus status) {
        this.status = status.getId();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public int getOverdue() {
        return overdue;
    }

    public void setOverdue(int overdue) {
        this.overdue = overdue;
    }

    public int getCommentsUnread() {
        return commentsUnread;
    }

    public void setCommentsUnread(int commentsUnread) {
        this.commentsUnread = commentsUnread;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getOrder() {
        return this.order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Card card = (Card) o;

        if (stackId != card.stackId) return false;
        if (attachmentCount != card.attachmentCount) return false;
        if (order != card.order) return false;
        if (archived != card.archived) return false;
        if (notified != card.notified) return false;
        if (overdue != card.overdue) return false;
        if (commentsUnread != card.commentsUnread) return false;
        if (title != null ? !title.equals(card.title) : card.title != null) return false;
        if (description != null ? !description.equals(card.description) : card.description != null)
            return false;
        if (type != null ? !type.equals(card.type) : card.type != null) return false;
        if (createdAt != null ? !createdAt.equals(card.createdAt) : card.createdAt != null)
            return false;
        if (deletedAt != null ? !deletedAt.equals(card.deletedAt) : card.deletedAt != null)
            return false;
        if (userId != null ? !userId.equals(card.userId) : card.userId != null) return false;
        return dueDate != null ? dueDate.equals(card.dueDate) : card.dueDate == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (int) (stackId ^ (stackId >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (deletedAt != null ? deletedAt.hashCode() : 0);
        result = 31 * result + attachmentCount;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + order;
        result = 31 * result + (archived ? 1 : 0);
        result = 31 * result + (dueDate != null ? dueDate.hashCode() : 0);
        result = 31 * result + (notified ? 1 : 0);
        result = 31 * result + overdue;
        result = 31 * result + commentsUnread;
        return result;
    }

    @Override
    public String toString() {
        return "Card{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", stackId=" + stackId +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                ", deletedAt=" + deletedAt +
                ", attachmentCount=" + attachmentCount +
                ", userId=" + userId +
                ", order=" + order +
                ", archived=" + archived +
                ", dueDate=" + dueDate +
                ", notified=" + notified +
                ", overdue=" + overdue +
                ", commentsUnread=" + commentsUnread +
                ", localId=" + localId +
                ", accountId=" + accountId +
                ", id=" + id +
                ", status=" + status +
                ", lastModified=" + lastModified +
                ", lastModifiedLocal=" + lastModifiedLocal +
                '}';
    }
}

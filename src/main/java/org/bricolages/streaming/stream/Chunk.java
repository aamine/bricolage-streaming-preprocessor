package org.bricolages.streaming.stream;
import org.bricolages.streaming.util.SQLUtils;
import javax.persistence.*;
import java.sql.Timestamp;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name="strload_chunks")
public class Chunk {
    @Getter
    @Id
    @Column(name="chunk_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    long id;

    @Getter
    @Column(name="object_url")
    String objectUrl;

    @Getter
    @Column(name="object_size")
    long objectSize;

    @Getter
    @Column(name="object_rows")
    int objectRows;

    @Getter
    @Column(name="error_rows")
    int errorRows;

    @Getter
    @Column(name="object_created_time")
    Timestamp objectCreatedTime = null;

    @Column(name="table_id")
    long tableId;

    @Column(name="dispatched")
    boolean dispatched;

    @Column(name="loaded")
    boolean loaded;

    public Chunk(long tableId, PacketFilterResult result) {
        this(result.getObjectUrl(), result.getObjectSize(), result.getOutputRows(), result.getErrorRows(), SQLUtils.getTimestamp(result.getObjectCreatedTime()), tableId);
    }

    public Chunk(String objectUrl, long objectSize, int objectRows, int errorRows, Timestamp objectCreatedTime, long tableId) {
        this.objectUrl = objectUrl;
        this.objectSize = objectSize;
        this.objectRows = objectRows;
        this.errorRows = errorRows;
        this.objectCreatedTime = objectCreatedTime;
        this.tableId = tableId;
    }

    public void changeStateToDispatched() {
        this.dispatched = true;
    }

    public void merge(Chunk other) {
        this.objectSize = other.objectSize;
        this.objectRows = other.objectRows;
        this.errorRows = other.errorRows;
        this.objectCreatedTime = other.objectCreatedTime;
        this.tableId = other.tableId;
    }
}

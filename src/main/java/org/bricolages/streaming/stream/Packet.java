package org.bricolages.streaming.stream;
import org.bricolages.streaming.table.Chunk;
import org.bricolages.streaming.object.S3ObjectMetadata;
import org.bricolages.streaming.util.SQLUtils;
import javax.persistence.*;
import java.sql.Timestamp;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name="strload_packets")
public class Packet {
    @Getter
    @Id
    @Column(name="packet_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    long id;

    @Getter
    @Column(name="object_url")
    String objectUrl;

    @Getter
    @Column(name="object_size")
    long objectSize;

    @Getter
    @Column(name="object_created_time")
    Timestamp objectCreatedTime;

    @Column(name="stream_id")
    long streamId;

    @Column(name="chunk_id")
    Long chunkId;

    @Column(name="processed")
    boolean processed = false;

    public Packet(S3ObjectMetadata obj, PacketStream stream) {
        this(obj.url(), obj.size(), SQLUtils.getTimestamp(obj.createdTime()), stream.getId());
    }

    public Packet(String objectUrl, long objectSize, Timestamp objectCreatedTime, long streamId) {
        this.objectUrl = objectUrl;
        this.objectSize = objectSize;
        this.objectCreatedTime = objectCreatedTime;
        this.streamId = streamId;
    }

    public void changeStateToProcessed(Chunk chunk) {
        this.chunkId = chunk.getId();
        this.processed = true;
    }

    // other -> this
    public void merge(Packet other) {
        if (other.objectSize > 0) {
            this.objectSize = other.objectSize;
        }
        if (other.objectCreatedTime != null) {
            this.objectCreatedTime = other.objectCreatedTime;
        }
        this.streamId = other.streamId;
        if (this.chunkId == null) {
            this.chunkId = other.chunkId;
        }
        this.processed = other.processed;
    }
}

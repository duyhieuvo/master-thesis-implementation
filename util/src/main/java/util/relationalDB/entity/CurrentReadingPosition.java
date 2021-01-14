package util.relationalDB.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class CurrentReadingPosition {
    @Id
    private int sourcePartition;
    private long currentReadingPosition;

    public CurrentReadingPosition(){}

    public CurrentReadingPosition(int sourcePartition, long currentReadingPosition) {
        this.sourcePartition= sourcePartition;
        this.currentReadingPosition = currentReadingPosition;
    }

    public int getSourcePartition() {
        return sourcePartition;
    }

    public void setSourcePartition(int sourcePartition) {
        this.sourcePartition = sourcePartition;
    }

    public long getCurrentReadingPosition() {
        return currentReadingPosition;
    }

    public void setCurrentReadingPosition(long currentReadingPosition) {
        this.currentReadingPosition = currentReadingPosition;
    }
}

package util.relationalDB.entity;

import javax.persistence.*;

//Entity class of the current balance
@Entity
@Table
@NamedQueries({
        @NamedQuery(name=CurrentBalance.BY_SOURCEPARTITION, query = CurrentBalance.BY_SOURCEPARTITION_QUERY)
})
public class CurrentBalance {
    //The custom query to get current balances of all customers with events published to a particular partition on the ESP platform
    //This is necessary for Kafka and Pulsar since each topic can be consumed concurrently by multiple instances, each of which has a different subset of assigned partitions
    //In case an instance is assigned a new partition, it can use this query to retrieve the current snapshot of the current balances of customer which is left off by another instance
    public static final String BY_SOURCEPARTITION = "QUERY_BY_SOURCEPARTITION";
    public static final String BY_SOURCEPARTITION_QUERY = "SELECT c FROM CurrentBalance c WHERE c.sourcePartition = :sourcePartition";

    @Id
    private String customerId;
    private float currentBalance;
    private int sourcePartition;

    public CurrentBalance(){}

    public CurrentBalance(String customerId, float currentBalance, int sourcePartition) {
        this.customerId = customerId;
        this.currentBalance = currentBalance;
        this.sourcePartition = sourcePartition;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public float getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(float currentBalance) {
        this.currentBalance = currentBalance;
    }

    public int getSourcePartition() {
        return sourcePartition;
    }

    public void setSourcePartition(int sourcePartition) {
        this.sourcePartition = sourcePartition;
    }
}

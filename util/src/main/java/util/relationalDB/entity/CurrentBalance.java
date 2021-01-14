package util.relationalDB.entity;

import javax.persistence.*;


@Entity
@Table
@NamedQueries({
        @NamedQuery(name=CurrentBalance.BY_SOURCEPARTITION, query = CurrentBalance.BY_SOURCEPARTITION_QUERY)
})
public class CurrentBalance {
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

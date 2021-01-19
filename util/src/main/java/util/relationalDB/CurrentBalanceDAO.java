package util.relationalDB;

import util.relationalDB.entity.CurrentBalance;
import util.relationalDB.entity.CurrentReadingPosition;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CurrentBalanceDAO {
    private static final javax.persistence.EntityManagerFactory ENTITY_MANAGER_FACTORY = Persistence
            .createEntityManagerFactory("Hibernate_JPA");

    public boolean updateCustomerBalance(CurrentBalance currentBalance, CurrentReadingPosition currentReadingPosition){
        EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
        EntityTransaction transaction = null;

        boolean status = false;

        try {
            transaction = entityManager.getTransaction();
            transaction.begin();

            entityManager.merge(currentBalance);
            entityManager.merge(currentReadingPosition);
            transaction.commit();
            status = true;

        } catch (Exception e){
            if(transaction != null){
                transaction.rollback();
                status = false;
            }
            e.printStackTrace();
        } finally {
            entityManager.close();
        }
        return status;
    }

    public boolean updateListCustomerBalance(Map<String,CurrentBalance> currentBalanceList, Map<Integer,CurrentReadingPosition> currentReadingPositionList){
        EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
        EntityTransaction transaction = null;

        boolean status = false;

        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            for(Map.Entry<String, CurrentBalance> currentBalance : currentBalanceList.entrySet()){
                entityManager.merge(currentBalance.getValue());
            }

            for(Map.Entry<Integer,CurrentReadingPosition> currentReadingPosition : currentReadingPositionList.entrySet()){
                entityManager.merge(currentReadingPosition.getValue());
            }
            transaction.commit();
            status = true;

        } catch (Exception e){
            if(transaction != null){
                transaction.rollback();
                status = false;
            }
            e.printStackTrace();
        } finally {
            entityManager.close();
        }
        return status;
    }

    public Map<String,Float> getCurrentBalance(List<Integer> sourcePartitions){
        EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
        EntityTransaction transaction = null;

        Map<String,Float> currentBalances = new HashMap<>();

        List<CurrentBalance> currentBalanceList  = new ArrayList<>();

        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            for(Integer sourcePartition : sourcePartitions){
                currentBalanceList.addAll(entityManager.createNamedQuery(CurrentBalance.BY_SOURCEPARTITION).setParameter("sourcePartition", sourcePartition).getResultList());
            }
            transaction.commit();
            currentBalances = currentBalanceList.stream().collect(Collectors.toMap(CurrentBalance::getCustomerId,currentBalance -> currentBalance.getCurrentBalance()));



        } catch (Exception e){
            if(transaction != null){
                transaction.rollback();
            }
            e.printStackTrace();

        } finally {
            entityManager.close();
        }
        return currentBalances;
    }

    public Map<Integer,Long> getCurrentReadingPosition(List<Integer> sourcePartitions){
        EntityManager entityManager = ENTITY_MANAGER_FACTORY.createEntityManager();
        EntityTransaction transaction = null;

        Map<Integer,Long> currentReadingPositions = new HashMap<>();
        CurrentReadingPosition currentReadingPosition;

        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            for(Integer sourcePartition : sourcePartitions){
                currentReadingPosition = entityManager.find(CurrentReadingPosition.class,sourcePartition);
                if(currentReadingPosition!=null){
                    currentReadingPositions.put(sourcePartition,currentReadingPosition.getCurrentReadingPosition());
                }
            }
            transaction.commit();

        } catch (Exception e){
            if(transaction != null){
                transaction.rollback();
            }
            e.printStackTrace();

        } finally {
            entityManager.close();
        }
        return currentReadingPositions;
    }
}

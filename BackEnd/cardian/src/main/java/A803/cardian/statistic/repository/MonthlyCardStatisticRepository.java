package A803.cardian.statistic.repository;

import A803.cardian.statistic.domain.MonthlyCardStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MonthlyCardStatisticRepository extends JpaRepository<MonthlyCardStatistic,Integer> {
    @Query("SELECT s FROM MonthlyCardStatistic s WHERE s.member.id = :memberId")
    List<MonthlyCardStatistic> findByMember_Id(Integer memberId);

    Optional<MonthlyCardStatistic> findByMyCardIdAndAndMonth(int cardId, int month);

    List<MonthlyCardStatistic> findMonthlyCardStatisticsByMemberId(int memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MonthlyCardStatistic m SET m.categoryMonthConsumes = :updateConsume where m.myCardId = :myCardId and m.month = :month")
    void updateMonthlyCardStatistic(int myCardId, int month, int updateConsume);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MonthlyCardStatistic m SET m.categoryMonthConsumes = :updateConsume where m.member.id = :memberId")
    void clearMonthlyCardStatistic(int memberId, int updateConsume);
}
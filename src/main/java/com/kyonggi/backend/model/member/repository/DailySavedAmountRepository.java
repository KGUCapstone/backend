package com.kyonggi.backend.model.member.repository;

import com.kyonggi.backend.model.member.entity.DailySavedAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DailySavedAmountRepository extends JpaRepository<DailySavedAmount, Long> {
    @Query("SELECT d FROM DailySavedAmount d WHERE d.member.id = :memberId")
    List<DailySavedAmount> findByMemberId(@Param("memberId") Long memberId);
}

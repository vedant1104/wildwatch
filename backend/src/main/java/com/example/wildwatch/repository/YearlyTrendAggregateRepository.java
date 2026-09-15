package com.example.wildwatch.repository;

import com.example.wildwatch.entity.YearlyTrendAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YearlyTrendAggregateRepository extends JpaRepository<YearlyTrendAggregate, Long> {
}

package com.example.spacetraderspicyber.repositories;

import com.example.spacetraderspicyber.model.TradeGood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeGoodRepository extends JpaRepository<TradeGood, Long> {
    List<TradeGood> findBySymbol(String symbol);

}



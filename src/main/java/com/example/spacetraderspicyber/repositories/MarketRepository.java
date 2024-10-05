package com.example.spacetraderspicyber.repositories;

import com.example.spacetraderspicyber.model.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {

    Market findBySymbol(String symbol);

    List<Market> findByGoodsToSellContaining(String searchString);


}
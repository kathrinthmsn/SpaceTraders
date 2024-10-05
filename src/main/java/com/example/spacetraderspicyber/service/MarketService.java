package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.TradeGood;
import com.example.spacetraderspicyber.repositories.MarketRepository;
import com.example.spacetraderspicyber.repositories.TradeGoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarketService {

    @Autowired
    private MarketRepository marketRepository;
    @Autowired
    private TradeGoodRepository tradeGoodRepository;

    public List<Market> getAllMarkets(){
        return marketRepository.findAll();
    }

    public Market findByName(String symbol){
        return marketRepository.findBySymbol(symbol);
    }


    public List<Market> findAll() {
        return marketRepository.findAll();
    }

    public List<Market> findMarketsByGoodsToSell(String searchString) {
        return marketRepository.findByGoodsToSellContaining(searchString);
    }

    public List<TradeGood> findTradeGoodsBySymbol(String searchString) {
        return tradeGoodRepository.findBySymbol(searchString);
    }

    public void deleteTradeGoods(List<TradeGood> tradeGoods) {
        tradeGoodRepository.deleteAll(tradeGoods);
    }

    public Set<String> findAllUniqueSymbols() {
        // Retrieve all unique symbols using Java streams
        return tradeGoodRepository.findAll()
                .stream()
                .map(TradeGood::getSymbol)
                .collect(Collectors.toSet());
    }

    public List<TradeGood> findBySymbol(String symbol) {
        return tradeGoodRepository.findBySymbol(symbol);
    }
    public void saveMarket(Market market){
        marketRepository.save(market);
    }

    public void deleteMarket(Market market){
        marketRepository.delete(market);
    }

    public void deleteAllMarkets() {
        marketRepository.deleteAll();
    }

}

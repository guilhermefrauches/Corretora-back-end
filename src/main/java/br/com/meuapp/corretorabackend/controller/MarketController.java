package br.com.meuapp.corretorabackend.controller;

import br.com.meuapp.corretorabackend.dto.BrapiInflationResponse;
import br.com.meuapp.corretorabackend.dto.BrapiPrimeRateResponse;
import br.com.meuapp.corretorabackend.dto.BrapiQuoteResponse;
import br.com.meuapp.corretorabackend.dto.BrapiStockListResponse;
import br.com.meuapp.corretorabackend.service.BrapiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final BrapiService brapiService;

    @GetMapping("/quote/{tickers}")
    public ResponseEntity<BrapiQuoteResponse> getQuote(@PathVariable String tickers) {
        return ResponseEntity.ok(brapiService.getQuote(tickers));
    }

    @GetMapping("/quote/{tickers}/history")
    public ResponseEntity<BrapiQuoteResponse> getQuoteWithHistory(
            @PathVariable String tickers,
            @RequestParam(defaultValue = "1mo") String range,
            @RequestParam(defaultValue = "1d") String interval) {
        return ResponseEntity.ok(brapiService.getQuoteWithHistory(tickers, range, interval));
    }

    @GetMapping("/stocks")
    public ResponseEntity<BrapiStockListResponse> listStocks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "close") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(brapiService.listStocks(search, type, sortBy, sortOrder, limit, page));
    }

    @GetMapping("/inflation")
    public ResponseEntity<BrapiInflationResponse> getInflation(
            @RequestParam(defaultValue = "brazil") String country,
            @RequestParam(defaultValue = "false") boolean historical,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return ResponseEntity.ok(brapiService.getInflation(country, historical, start, end));
    }

    @GetMapping("/prime-rate")
    public ResponseEntity<BrapiPrimeRateResponse> getPrimeRate(
            @RequestParam(defaultValue = "brazil") String country,
            @RequestParam(defaultValue = "false") boolean historical,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return ResponseEntity.ok(brapiService.getPrimeRate(country, historical, start, end));
    }
}

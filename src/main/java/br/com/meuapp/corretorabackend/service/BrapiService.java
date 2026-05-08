package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.config.BrapiConfig;
import br.com.meuapp.corretorabackend.dto.BrapiInflationResponse;
import br.com.meuapp.corretorabackend.dto.BrapiPrimeRateResponse;
import br.com.meuapp.corretorabackend.dto.BrapiQuoteResponse;
import br.com.meuapp.corretorabackend.dto.BrapiStockListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrapiService {

    private final BrapiConfig brapiConfig;
    private final RestTemplate brapiRestTemplate;

    public BrapiQuoteResponse getQuote(String tickers) {
        String url = brapiConfig.getBaseUrl() + "/quote/" + tickers;
        return executeGet(url, BrapiQuoteResponse.class);
    }

    public BrapiQuoteResponse getQuoteWithHistory(String tickers, String range, String interval) {
        String url = UriComponentsBuilder
                .fromUriString(brapiConfig.getBaseUrl() + "/quote/" + tickers)
                .queryParam("range", range)
                .queryParam("interval", interval)
                .toUriString();
        return executeGet(url, BrapiQuoteResponse.class);
    }

    public BrapiStockListResponse listStocks(String search, String type, String sortBy,
                                             String sortOrder, int limit, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(brapiConfig.getBaseUrl() + "/quote/list")
                .queryParam("sortBy", sortBy)
                .queryParam("sortOrder", sortOrder)
                .queryParam("limit", limit)
                .queryParam("page", page);

        if (search != null && !search.isBlank()) {
            builder.queryParam("search", search);
        }
        if (type != null && !type.isBlank()) {
            builder.queryParam("type", type);
        }

        return executeGet(builder.toUriString(), BrapiStockListResponse.class);
    }

    public BrapiInflationResponse getInflation(String country, boolean historical, String start, String end) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(brapiConfig.getBaseUrl() + "/v2/inflation")
                .queryParam("country", country)
                .queryParam("historical", historical)
                .queryParam("sortBy", "date")
                .queryParam("sortOrder", "desc");
        if (start != null && !start.isBlank()) {
            builder.queryParam("start", start);
        }
        if (end != null && !end.isBlank()) {
            builder.queryParam("end", end);
        }
        return executeGet(builder.toUriString(), BrapiInflationResponse.class);
    }

    public BrapiPrimeRateResponse getPrimeRate(String country, boolean historical, String start, String end) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(brapiConfig.getBaseUrl() + "/v2/prime-rate")
                .queryParam("country", country)
                .queryParam("historical", historical)
                .queryParam("sortBy", "date")
                .queryParam("sortOrder", "desc");
        if (start != null && !start.isBlank()) {
            builder.queryParam("start", start);
        }
        if (end != null && !end.isBlank()) {
            builder.queryParam("end", end);
        }
        return executeGet(builder.toUriString(), BrapiPrimeRateResponse.class);
    }

    private <T> T executeGet(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + brapiConfig.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<T> response = brapiRestTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Brapi client error — status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao consultar a API de mercado: " + e.getStatusCode());
        } catch (HttpServerErrorException e) {
            log.error("Brapi server error — status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Serviço de mercado indisponível: " + e.getStatusCode());
        }
    }
}

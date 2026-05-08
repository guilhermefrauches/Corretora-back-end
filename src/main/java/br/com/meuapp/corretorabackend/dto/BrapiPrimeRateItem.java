package br.com.meuapp.corretorabackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrapiPrimeRateItem {

    private String date;
    private Double value;
    private Long epochDate;
}

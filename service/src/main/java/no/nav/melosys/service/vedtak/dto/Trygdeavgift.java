package no.nav.melosys.service.vedtak.dto;

import java.math.BigDecimal;

public record Trygdeavgift(Long avgiftspliktigInntekt, BigDecimal avgiftsbeløpMd, BigDecimal trygdesats, String avgiftskode) {
}

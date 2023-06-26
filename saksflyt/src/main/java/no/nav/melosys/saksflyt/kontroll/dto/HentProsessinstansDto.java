package no.nav.melosys.saksflyt.kontroll.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HentProsessinstansDto(UUID id,
                                    Long behandlingId,
                                    String saksnummer,
                                    String prosessType,
                                    LocalDateTime endretDato,
                                    LocalDateTime registrertDato,
                                    String feiletSteg,
                                    String sisteFeilmelding,
                                    String correlationId) {
}

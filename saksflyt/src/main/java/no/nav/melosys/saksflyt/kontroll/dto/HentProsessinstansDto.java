package no.nav.melosys.saksflyt.kontroll.dto;

import no.nav.melosys.saksflytapi.domain.ProsessStatus;

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
                                    ProsessStatus status,
                                    String correlationId) {
}

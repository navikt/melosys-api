package no.nav.melosys.saksflyt.prosessflyt;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessType;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

public final class ProsessflytDefinisjon {

    private ProsessflytDefinisjon() {}

    private static final Map<ProsessType, ProsessFlyt> PROSESS_FLYT_MAP = new EnumMap<>(ProsessType.class);

    static {
        PROSESS_FLYT_MAP.put(
            ProsessType.MANGELBREV,
            new ProsessFlyt(ProsessType.MANGELBREV,
                MANGELBREV
            )
        );

        PROSESS_FLYT_MAP.put(
            ProsessType.FORVALTNINGSMELDING_SEND,
            new ProsessFlyt(ProsessType.FORVALTNINGSMELDING_SEND,
                SEND_FORVALTNINGSMELDING
            )
        );

        PROSESS_FLYT_MAP.put(
            ProsessType.HENLEGG_SAK,
            new ProsessFlyt(ProsessType.HENLEGG_SAK,
                HS_SEND_BREV,
                IV_STATUS_BEH_AVSL
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.VIDERESEND_SOKNAD,
            new ProsessFlyt(ProsessType.VIDERESEND_SOKNAD,
                AVKLAR_MYNDIGHET,
                VS_SEND_ORIENTERINGSBREV,
                VS_SEND_SOKNAD,
                VS_DISTRIBUER_JOURNALPOST,
                IV_STATUS_BEH_AVSL
            )
        );

        PROSESS_FLYT_MAP.put(
            ProsessType.ANMODNING_OM_UNNTAK,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK,
                AVKLAR_MYNDIGHET,
                AOU_OPPDATER_MEDL,
                AOU_SEND_BREV,
                AOU_SEND_SED,
                AOU_OPPDATER_OPPGAVE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.IVERKSETT_VEDTAK,
            new ProsessFlyt(ProsessType.IVERKSETT_VEDTAK,
                AVKLAR_MYNDIGHET,
                AVKLAR_ARBEIDSGIVER,
                IV_OPPDATER_MEDL,
                IV_SEND_BREV,
                IV_SEND_SED,
                UL_DISTRIBUER_JOURNALPOST,
                IV_OPPDATER_RESULTAT,
                IV_OPPRETT_AVGIFTSOPPGAVE,
                IV_AVSLUTT_BEHANDLING,
                IV_STATUS_BEH_AVSL
            )
        );
    }

    public static Optional<ProsessFlyt> finnFlytForProsessType(ProsessType prosessType) {
        return Optional.ofNullable(PROSESS_FLYT_MAP.get(prosessType));
    }
}

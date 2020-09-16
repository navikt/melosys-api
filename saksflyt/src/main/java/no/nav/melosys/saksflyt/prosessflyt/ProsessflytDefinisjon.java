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
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.VIDERESEND_SOKNAD,
            new ProsessFlyt(ProsessType.VIDERESEND_SOKNAD,
                AVKLAR_MYNDIGHET,
                VS_SEND_ORIENTERINGSBREV,
                VS_SEND_SOKNAD,
                DISTRIBUER_JOURNALPOST_UTLAND,
                SOB_BEHANDLING_AVSLUTTET
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

        PROSESS_FLYT_MAP.put(ProsessType.JFR_NY_SAK,
            new ProsessFlyt(ProsessType.JFR_NY_SAK,
                JFR_OPPRETT_SAK_OG_BEH,
                JFR_OPPRETT_SØKNAD,
                OPPRETT_ARKIVSAK,
                STATUS_BEH_OPPR,
                JFR_OPPDATER_SAKSRELASJON,
                OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
                HENT_REGISTER_OPPL,
                JFR_VURDER_INNGANGSVILKÅR,
                OPPRETT_OPPGAVE,
                SEND_FORVALTNINGSMELDING
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.JFR_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.JFR_NY_BEHANDLING,
                OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
                REPLIKER_BEHANDLING,
                OPPRETT_OPPGAVE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.JFR_KNYTT,
            new ProsessFlyt(ProsessType.JFR_KNYTT,
                OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
                JFR_SETT_VURDER_DOKUMENT,
                JFR_TILDEL_BEHANDLINGSOPPGAVE
            )
        );

        /*PROSESS_FLYT_MAP.put(ProsessType.JFR_AOU_BREV, TODO
            new ProsessFlyt(ProsessType.JFR_AOU_BREV,
                JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING,
                JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST,
                JFR_AOU_BREV_OPPRETT_SEDDOKUMENT,
                AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE,
                AOU_MOTTAK_SAK_OG_BEHANDLING_OPPRETTET,
                AOU_MOTTAK_AVSLUTT_TIDLIGERE_PERIODE,
                AOU_MOTTAK_HENT_REGISTEROPPLYSNINGER,
                AOU_MOTTAK_REGISTERKONTROLL,
                AOU_MOTTAK_OPPRETT_PERIODE_MEDL,
                AOU_MOTTAK_OPPRETT_OPPGAVE
            )
        );*/

        PROSESS_FLYT_MAP.put(ProsessType.OPPRETT_NY_SAK,
            new ProsessFlyt(ProsessType.OPPRETT_NY_SAK,
                JFR_OPPRETT_SAK_OG_BEH,
                JFR_OPPRETT_SØKNAD,
                OPPRETT_ARKIVSAK,
                STATUS_BEH_OPPR,
                HENT_REGISTER_OPPL,
                JFR_VURDER_INNGANGSVILKÅR,
                GJENBRUK_OPPGAVE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.IVERKSETT_VEDTAK,
            new ProsessFlyt(ProsessType.IVERKSETT_VEDTAK,
                AVKLAR_MYNDIGHET,
                AVKLAR_ARBEIDSGIVER,
                OPPDATER_MEDL_VEDTAK,
                SEND_VEDTAKSBREV_INNLAND,
                SEND_VEDTAK_UTLAND,
                DISTRIBUER_JOURNALPOST_UTLAND,
                OPPRETT_AVGIFTSOPPGAVE,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );
    }

    public static Optional<ProsessFlyt> finnFlytForProsessType(ProsessType prosessType) {
        return Optional.ofNullable(PROSESS_FLYT_MAP.get(prosessType));
    }
}

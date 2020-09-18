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
                OPPDATER_SAKSRELASJON,
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

        PROSESS_FLYT_MAP.put(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE,
            new ProsessFlyt(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE,
                HENT_MOTTAKERINSTITUSJON_FORKORTET_PERIODE,
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

        PROSESS_FLYT_MAP.put(ProsessType.MOTTAK_SED,
            new ProsessFlyt(ProsessType.MOTTAK_SED,
                SED_MOTTAK_RUTING
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.MOTTAK_SED_JOURNALFØRING,
            new ProsessFlyt(ProsessType.MOTTAK_SED_JOURNALFØRING,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ANMODNING_OM_UNNTAK_SVAR,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK_SVAR
                //TODO
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.REGISTRERING_UNNTAK_NY_SAK,
            new ProsessFlyt(ProsessType.REGISTRERING_UNNTAK_NY_SAK,
                SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
                OPPRETT_ARKIVSAK,
                OPPDATER_SAKSRELASJON,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET, //TODO: gjebruk steg
                REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE, //TODO: trengs nok ikke når vi oppretter ny sak
                REG_UNNTAK_OPPRETT_SEDDOKUMENT, //TODO: bør gjøres gjenbrukbart
                REG_UNNTAK_HENT_REGISTEROPPLYSNINGER, //TODO: HENT_REGISTER_OPPL
                REG_UNNTAK_REGISTERKONTROLL, //TODO: gjør gjenbrukbart
                REG_UNNTAK_BESTEM_BEHANDLINGSMAATE
            )
        ); //TODO: flyter for godkjenning/avvisning

        PROSESS_FLYT_MAP.put(ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING,
                SED_MOTTAK_OPPRETT_NY_BEHANDLING,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                REG_UNNTAK_SAK_OG_BEHANDLING_OPPRETTET,
                REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE,
                REG_UNNTAK_OPPRETT_SEDDOKUMENT,
                REG_UNNTAK_HENT_REGISTEROPPLYSNINGER,
                REG_UNNTAK_REGISTERKONTROLL,
                REG_UNNTAK_BESTEM_BEHANDLINGSMAATE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ARBEID_FLERE_LAND_NY_SAK,
            new ProsessFlyt(ProsessType.ARBEID_FLERE_LAND_NY_SAK,
                SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
                OPPRETT_ARKIVSAK,
                OPPDATER_SAKSRELASJON,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                AFL_SAK_OG_BEHANDLING_OPPRETTET,
                AFL_AVSLUTT_TIDLIGERE_PERIODE,
                AFL_HENT_REGISTEROPPLYSNINGER,
                AFL_OPPRETT_BEHANDLINGSGRUNNLAG,
                AFL_VURDER_INNGANGSVILKÅR, //FIXME: skal ikke vurderes når Norge IKKE er utpekt
                AFL_REGISTERKONTROLL //TODO: etter dette må det opprettes en ny flyt om Norge ikke er utpekt og ingen treff i kontroll
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING,
                SED_MOTTAK_OPPRETT_NY_BEHANDLING,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                AFL_SAK_OG_BEHANDLING_OPPRETTET,
                AFL_AVSLUTT_TIDLIGERE_PERIODE,
                AFL_HENT_REGISTEROPPLYSNINGER,
                AFL_OPPRETT_BEHANDLINGSGRUNNLAG,
                AFL_VURDER_INNGANGSVILKÅR, //FIXME: skal ikke vurderes når Norge IKKE er utpekt
                AFL_REGISTERKONTROLL //TODO: etter dette må det opprettes en ny flyt om Norge ikke er utpekt og ingen treff i kontroll
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ANMODNING_OM_UNNTAK_SVAR,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK_SVAR,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                AOU_SVAR_OPPRETT_ANMODNINGSPERIODESVAR,
                AOU_SVAR_OPPDATER_BEHANDLING
            )
        );



    }

    public static Optional<ProsessFlyt> finnFlytForProsessType(ProsessType prosessType) {
        return Optional.ofNullable(PROSESS_FLYT_MAP.get(prosessType));
    }
}

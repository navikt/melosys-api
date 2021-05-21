package no.nav.melosys.saksflyt.prosessflyt;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessType;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

public final class ProsessflytDefinisjon {

    private ProsessflytDefinisjon() {
    }

    private static final Map<ProsessType, ProsessFlyt> PROSESS_FLYT_MAP = new EnumMap<>(ProsessType.class);

    static {

        PROSESS_FLYT_MAP.put(
            ProsessType.OPPRETT_OG_DISTRIBUER_BREV,
            new ProsessFlyt(ProsessType.OPPRETT_OG_DISTRIBUER_BREV,
                OPPRETT_OG_JOURNALFØR_BREV,
                DISTRIBUER_JOURNALPOST
            )
        );

        PROSESS_FLYT_MAP.put(
            ProsessType.HENLEGG_SAK,
            new ProsessFlyt(ProsessType.HENLEGG_SAK,
                SEND_HENLEGGELSESBREV,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.VIDERESEND_SOKNAD,
            new ProsessFlyt(ProsessType.VIDERESEND_SOKNAD,
                AVKLAR_MYNDIGHET,
                SEND_ORIENTERINGSBREV_VIDERESENDING_SØKNAD,
                VIDERESEND_SØKNAD,
                DISTRIBUER_JOURNALPOST_UTLAND,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(
            ProsessType.ANMODNING_OM_UNNTAK,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK,
                AVKLAR_MYNDIGHET,
                LAGRE_ANMODNINGSPERIODE_MEDL,
                SEND_ORIENTERING_ANMODNING_UNNTAK,
                SEND_ANMODNING_OM_UNNTAK,
                OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.JFR_NY_SAK,
            new ProsessFlyt(ProsessType.JFR_NY_SAK,
                JFR_OPPRETT_SAK_OG_BEH,
                JFR_OPPRETT_SØKNAD,
                OPPRETT_ARKIVSAK,
                SOB_BEHANDLING_OPPRETTET,
                OPPDATER_SAKSRELASJON,
                OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
                HENT_REGISTEROPPLYSNINGER,
                VURDER_INNGANGSVILKÅR,
                OPPRETT_OPPGAVE,
                SEND_FORVALTNINGSMELDING
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.JFR_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.JFR_NY_BEHANDLING,
                REPLIKER_BEHANDLING,
                OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
                OPPDATER_SAKSRELASJON,
                OPPRETT_OPPGAVE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.JFR_KNYTT,
            new ProsessFlyt(ProsessType.JFR_KNYTT,
                OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
                OPPDATER_SAKSRELASJON,
                JFR_SETT_VURDER_DOKUMENT,
                JFR_TILDEL_BEHANDLINGSOPPGAVE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.OPPRETT_NY_SAK,
            new ProsessFlyt(ProsessType.OPPRETT_NY_SAK,
                JFR_OPPRETT_SAK_OG_BEH,
                JFR_OPPRETT_SØKNAD,
                OPPRETT_ARKIVSAK,
                OPPDATER_SAKSRELASJON,
                SOB_BEHANDLING_OPPRETTET,
                HENT_REGISTEROPPLYSNINGER,
                VURDER_INNGANGSVILKÅR,
                GJENBRUK_OPPGAVE
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.IVERKSETT_VEDTAK_EOS,
            new ProsessFlyt(ProsessType.IVERKSETT_VEDTAK_EOS,
                AVKLAR_MYNDIGHET,
                AVKLAR_ARBEIDSGIVER,
                LAGRE_LOVVALGSPERIODE_MEDL,
                SEND_VEDTAKSBREV_INNLAND,
                SEND_VEDTAK_UTLAND,
                DISTRIBUER_JOURNALPOST_UTLAND,
                OPPRETT_AVGIFTSOPPGAVE,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE,
            new ProsessFlyt(ProsessType.IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE,
                HENT_MOTTAKERINSTITUSJON_FORKORTET_PERIODE,
                LAGRE_LOVVALGSPERIODE_MEDL,
                SEND_VEDTAKSBREV_INNLAND,
                SEND_VEDTAK_UTLAND,
                DISTRIBUER_JOURNALPOST_UTLAND,
                OPPRETT_AVGIFTSOPPGAVE,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.IVERKSETT_VEDTAK,
            new ProsessFlyt(ProsessType.IVERKSETT_VEDTAK,
                LAGRE_MEDLEMSKAPSPERIODE_MEDL,
                PUBLISER_VEDTAK,
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

        PROSESS_FLYT_MAP.put(ProsessType.REGISTRERING_UNNTAK_NY_SAK,
            new ProsessFlyt(ProsessType.REGISTRERING_UNNTAK_NY_SAK,
                SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
                OPPRETT_ARKIVSAK,
                OPPDATER_SAKSRELASJON,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                SOB_BEHANDLING_OPPRETTET,
                OPPRETT_SEDDOKUMENT,
                HENT_REGISTEROPPLYSNINGER,
                REGISTERKONTROLL,
                BESTEM_BEHANDLINGMÅTE_SED
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING,
                SED_MOTTAK_OPPRETT_NY_BEHANDLING,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                SOB_BEHANDLING_OPPRETTET,
                AVSLUTT_TIDLIGERE_MEDL_PERIODE,
                OPPRETT_SEDDOKUMENT,
                HENT_REGISTEROPPLYSNINGER,
                REGISTERKONTROLL,
                BESTEM_BEHANDLINGMÅTE_SED
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ARBEID_FLERE_LAND_NY_SAK,
            new ProsessFlyt(ProsessType.ARBEID_FLERE_LAND_NY_SAK,
                SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
                OPPRETT_ARKIVSAK,
                OPPDATER_SAKSRELASJON,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                SOB_BEHANDLING_OPPRETTET,
                OPPRETT_SEDDOKUMENT,
                OPPRETT_SED_GRUNNLAG,
                HENT_REGISTEROPPLYSNINGER,
                VURDER_INNGANGSVILKÅR,
                REGISTERKONTROLL,
                BESTEM_BEHANDLINGMÅTE_SED
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING,
                SED_MOTTAK_OPPRETT_NY_BEHANDLING,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                SOB_BEHANDLING_OPPRETTET,
                AVSLUTT_TIDLIGERE_MEDL_PERIODE,
                OPPRETT_SEDDOKUMENT,
                OPPRETT_SED_GRUNNLAG,
                HENT_REGISTEROPPLYSNINGER,
                VURDER_INNGANGSVILKÅR,
                REGISTERKONTROLL,
                BESTEM_BEHANDLINGMÅTE_SED
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ANMODNING_OM_UNNTAK_SVAR,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK_SVAR,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                OPPRETT_ANMODNINGSPERIODESVAR,
                BESTEM_BEHANDLINGSMÅTE_SVAR_ANMODNING_UNNTAK
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK,
                SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
                OPPRETT_ARKIVSAK,
                OPPDATER_SAKSRELASJON,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                OPPRETT_SEDDOKUMENT,
                OPPRETT_ANMODNINGSPERIODE_FRA_SED,
                SOB_BEHANDLING_OPPRETTET,
                HENT_REGISTEROPPLYSNINGER,
                REGISTERKONTROLL,
                LAGRE_ANMODNINGSPERIODE_MEDL,
                BESTEM_BEHANDLINGMÅTE_SED
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING,
                SED_MOTTAK_OPPRETT_NY_BEHANDLING,
                SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
                OPPRETT_SEDDOKUMENT,
                OPPRETT_ANMODNINGSPERIODE_FRA_SED,
                SOB_BEHANDLING_OPPRETTET,
                AVSLUTT_TIDLIGERE_MEDL_PERIODE,
                HENT_REGISTEROPPLYSNINGER,
                REGISTERKONTROLL,
                LAGRE_ANMODNINGSPERIODE_MEDL,
                BESTEM_BEHANDLINGMÅTE_SED
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.REGISTRERING_UNNTAK_GODKJENN,
            new ProsessFlyt(ProsessType.REGISTRERING_UNNTAK_GODKJENN,
                LAGRE_LOVVALGSPERIODE_MEDL,
                SEND_GODKJENNING_REGISTRERING_UNNTAK,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.REGISTRERING_UNNTAK_AVVIS,
            new ProsessFlyt(ProsessType.REGISTRERING_UNNTAK_AVVIS,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.UTPEKING_AVVIS,
            new ProsessFlyt(ProsessType.UTPEKING_AVVIS,
                UTPEKING_SEND_AVSLAG,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR,
            new ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR,
                LAGRE_LOVVALGSPERIODE_MEDL,
                SEND_SVAR_ANMODNING_UNNTAK,
                AVSLUTT_SAK_OG_BEHANDLING,
                SOB_BEHANDLING_AVSLUTTET
            )
        );

        PROSESS_FLYT_MAP.put(ProsessType.MOTTAK_SOKNAD_ALTINN,
            new ProsessFlyt(ProsessType.MOTTAK_SOKNAD_ALTINN,
                OPPRETT_SAK_OG_BEHANDLING_FRA_ALTINN_SØKNAD,
                OPPRETT_ARKIVSAK,
                OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN,
                SOB_BEHANDLING_OPPRETTET,
                HENT_REGISTEROPPLYSNINGER,
                VURDER_INNGANGSVILKÅR,
                OPPRETT_OPPGAVE,
                SEND_FORVALTNINGSMELDING
            )
        );
    }

    public static Optional<ProsessFlyt> finnFlytForProsessType(ProsessType prosessType) {
        return Optional.ofNullable(PROSESS_FLYT_MAP.get(prosessType));
    }
}

package no.nav.melosys.saksflyt.prosessflyt

import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessSteg.*
import no.nav.melosys.saksflytapi.domain.ProsessType
import java.util.*

object ProsessflytDefinisjon {
    private val PROSESS_FLYT_MAP: MutableMap<ProsessType, ProsessFlyt> = EnumMap(ProsessType::class.java)

    init {
        PROSESS_FLYT_MAP[ProsessType.OPPRETT_OG_DISTRIBUER_BREV] = ProsessFlyt(
            ProsessType.OPPRETT_OG_DISTRIBUER_BREV,
            OPPRETT_OG_JOURNALFØR_BREV,
            DISTRIBUER_JOURNALPOST
        )

        PROSESS_FLYT_MAP[ProsessType.SEND_BREV] = ProsessFlyt(
            ProsessType.SEND_BREV,
            BESTILL_BREV
        )

        PROSESS_FLYT_MAP[ProsessType.HENLEGG_SAK] = ProsessFlyt(
            ProsessType.HENLEGG_SAK,
            SEND_HENLEGGELSESBREV
        )

        PROSESS_FLYT_MAP[ProsessType.VIDERESEND_SOKNAD] = ProsessFlyt(
            ProsessType.VIDERESEND_SOKNAD,
            AVKLAR_MYNDIGHET,
            SEND_ORIENTERINGSBREV_VIDERESENDING_SØKNAD,
            VIDERESEND_SØKNAD,
            DISTRIBUER_JOURNALPOST_UTLAND
        )

        PROSESS_FLYT_MAP[ProsessType.ANMODNING_OM_UNNTAK] = ProsessFlyt(
            ProsessType.ANMODNING_OM_UNNTAK,
            AVKLAR_MYNDIGHET,
            LAGRE_ANMODNINGSPERIODE_MEDL,
            SEND_ORIENTERING_ANMODNING_UNNTAK,
            SEND_ANMODNING_OM_UNNTAK,
            OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT
        )

        PROSESS_FLYT_MAP[ProsessType.REGISTRERE_UNNTAK_FRA_MEDLEMSKAP] = ProsessFlyt(
            ProsessType.REGISTRERE_UNNTAK_FRA_MEDLEMSKAP,
            LAGRE_LOVVALGSPERIODE_MEDL,
            AVSLUTT_SAK_OG_BEHANDLING
        )

        PROSESS_FLYT_MAP[ProsessType.JFR_NY_SAK_BRUKER] = ProsessFlyt(
            ProsessType.JFR_NY_SAK_BRUKER,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        )

        PROSESS_FLYT_MAP[ProsessType.JFR_NY_SAK_VIRKSOMHET] = ProsessFlyt(
            ProsessType.JFR_NY_SAK_VIRKSOMHET,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_ARKIVSAK,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPRETT_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING] = ProsessFlyt(
            ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING,
            REPLIKER_BEHANDLING,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPDATER_SAKSRELASJON,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        )

        PROSESS_FLYT_MAP[ProsessType.JFR_ANDREGANG_NY_BEHANDLING] = ProsessFlyt(
            ProsessType.JFR_ANDREGANG_NY_BEHANDLING,
            OPPRETT_NY_BEHANDLING,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPDATER_SAKSRELASJON,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        )

        PROSESS_FLYT_MAP[ProsessType.ANNULLER_SAK] = ProsessFlyt(
            ProsessType.ANNULLER_SAK,
            LAGRE_MEDLEMSKAPSPERIODE_MEDL,
            KANSELLER_FAKTURASERIE,
            AVSLUTT_SAK_OG_BEHANDLING
        )

        PROSESS_FLYT_MAP[ProsessType.JFR_KNYTT] = ProsessFlyt(
            ProsessType.JFR_KNYTT,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPDATER_SAKSRELASJON,
            OPPRETT_TIDLIGERE_JOURNALPOSTER_FOR_SAK,
            JFR_SETT_VURDER_DOKUMENT,
            JFR_TILDEL_BEHANDLINGSOPPGAVE,
            SEND_FORVALTNINGSMELDING
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE] = ProsessFlyt(
            ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            GJENBRUK_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE] = ProsessFlyt(
            ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            GJENBRUK_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_SAK] = ProsessFlyt(
            ProsessType.OPPRETT_SAK,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING] = ProsessFlyt(
            ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING,
            OPPRETT_MANGLENDE_INNBETALING_BEHANDLING,
            OPPRETT_OPPGAVE,
            SEND_MANGLENDE_INNBETALING_VARSELBREV
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING] = ProsessFlyt(
            ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING,
            OPPRETT_AARSAVREGNING_BEHANDLING,
            OPPRETT_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK] = ProsessFlyt(
            ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK,
            REPLIKER_BEHANDLING,
            OPPRETT_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.OPPRETT_NY_BEHANDLING_FOR_SAK] = ProsessFlyt(
            ProsessType.OPPRETT_NY_BEHANDLING_FOR_SAK,
            OPPRETT_NY_BEHANDLING,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE
        )

        PROSESS_FLYT_MAP[ProsessType.IVERKSETT_VEDTAK_EOS] = ProsessFlyt(
            ProsessType.IVERKSETT_VEDTAK_EOS,
            AVKLAR_MYNDIGHET,
            AVKLAR_ARBEIDSGIVER,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_VEDTAKSBREV_INNLAND,
            SEND_VEDTAK_UTLAND,
            DISTRIBUER_JOURNALPOST_UTLAND,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        )

        PROSESS_FLYT_MAP[ProsessType.IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE] = ProsessFlyt(
            ProsessType.IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE,
            HENT_MOTTAKERINSTITUSJON_FORKORTET_PERIODE,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_VEDTAKSBREV_INNLAND,
            SEND_VEDTAK_UTLAND,
            DISTRIBUER_JOURNALPOST_UTLAND,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        )

        PROSESS_FLYT_MAP[ProsessType.IVERKSETT_VEDTAK_FTRL] = ProsessFlyt(
            ProsessType.IVERKSETT_VEDTAK_FTRL,
            LAGRE_MEDLEMSKAPSPERIODE_MEDL,
            OPPRETT_FAKTURASERIE,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        )

        PROSESS_FLYT_MAP[ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE] = ProsessFlyt(
            ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE,
            AVKLAR_MYNDIGHET,
            AVKLAR_ARBEIDSGIVER,
            LAGRE_LOVVALGSPERIODE_MEDL,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        )

        PROSESS_FLYT_MAP[ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV] = ProsessFlyt(
            ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_VEDTAKSBREV_INNLAND,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        )


        PROSESS_FLYT_MAP[ProsessType.MOTTAK_SED] = ProsessFlyt(
            ProsessType.MOTTAK_SED,
            SED_MOTTAK_RUTING
        )

        PROSESS_FLYT_MAP[ProsessType.MOTTAK_SED_JOURNALFØRING] = ProsessFlyt(
            ProsessType.MOTTAK_SED_JOURNALFØRING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST
        )

        PROSESS_FLYT_MAP[ProsessType.REGISTRERING_UNNTAK_NY_SAK] = ProsessFlyt(
            ProsessType.REGISTRERING_UNNTAK_NY_SAK,
            SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_SEDDOKUMENT,
            HENT_REGISTEROPPLYSNINGER,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        )

        PROSESS_FLYT_MAP[ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING] = ProsessFlyt(
            ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING,
            SED_MOTTAK_OPPRETT_NY_BEHANDLING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            AVSLUTT_TIDLIGERE_MEDL_PERIODE,
            OPPRETT_SEDDOKUMENT,
            HENT_REGISTEROPPLYSNINGER,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        )

        PROSESS_FLYT_MAP[ProsessType.ARBEID_FLERE_LAND_NY_SAK] = ProsessFlyt(
            ProsessType.ARBEID_FLERE_LAND_NY_SAK,
            SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_SEDDOKUMENT,
            OPPRETT_SED_GRUNNLAG,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        )

        PROSESS_FLYT_MAP[ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING] = ProsessFlyt(
            ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING,
            SED_MOTTAK_OPPRETT_NY_BEHANDLING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            AVSLUTT_TIDLIGERE_MEDL_PERIODE,
            OPPRETT_SEDDOKUMENT,
            OPPRETT_SED_GRUNNLAG,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        )

        PROSESS_FLYT_MAP[ProsessType.ANMODNING_OM_UNNTAK_SVAR] = ProsessFlyt(
            ProsessType.ANMODNING_OM_UNNTAK_SVAR,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_ANMODNINGSPERIODESVAR,
            BESTEM_BEHANDLINGSMÅTE_SVAR_ANMODNING_UNNTAK
        )

        PROSESS_FLYT_MAP[ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK] = ProsessFlyt(
            ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK,
            SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_SEDDOKUMENT,
            OPPRETT_SED_GRUNNLAG,
            OPPRETT_ANMODNINGSPERIODE_FRA_SED,
            HENT_REGISTEROPPLYSNINGER,
            REGISTERKONTROLL,
            LAGRE_ANMODNINGSPERIODE_MEDL,
            BESTEM_BEHANDLINGMÅTE_SED
        )

        PROSESS_FLYT_MAP[ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING] = ProsessFlyt(
            ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING,
            SED_MOTTAK_OPPRETT_NY_BEHANDLING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_SEDDOKUMENT,
            OPPRETT_SED_GRUNNLAG,
            OPPRETT_ANMODNINGSPERIODE_FRA_SED,
            AVSLUTT_TIDLIGERE_MEDL_ANMODNINGSPERIODE,
            AVSLUTT_TIDLIGERE_MEDL_PERIODE,
            HENT_REGISTEROPPLYSNINGER,
            REGISTERKONTROLL,
            LAGRE_ANMODNINGSPERIODE_MEDL,
            BESTEM_BEHANDLINGMÅTE_SED
        )

        PROSESS_FLYT_MAP[ProsessType.REGISTRERING_UNNTAK_GODKJENN] = ProsessFlyt(
            ProsessType.REGISTRERING_UNNTAK_GODKJENN,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_GODKJENNING_REGISTRERING_UNNTAK,
            AVSLUTT_SAK_OG_BEHANDLING
        )

        PROSESS_FLYT_MAP[ProsessType.REGISTRERING_UNNTAK_AVVIS] = ProsessFlyt(
            ProsessType.REGISTRERING_UNNTAK_AVVIS,
            AVSLUTT_SAK_OG_BEHANDLING
        )

        PROSESS_FLYT_MAP[ProsessType.UTPEKING_AVVIS] = ProsessFlyt(
            ProsessType.UTPEKING_AVVIS,
            UTPEKING_SEND_AVSLAG,
            AVSLUTT_SAK_OG_BEHANDLING
        )

        PROSESS_FLYT_MAP[ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR] = ProsessFlyt(
            ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_SVAR_ANMODNING_UNNTAK,
            AVSLUTT_SAK_OG_BEHANDLING
        )

        PROSESS_FLYT_MAP[ProsessType.MOTTAK_SOKNAD_ALTINN] = ProsessFlyt(
            ProsessType.MOTTAK_SOKNAD_ALTINN,
            OPPRETT_SAK_OG_BEHANDLING_FRA_ALTINN_SØKNAD,
            OPPRETT_ARKIVSAK,
            OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        )

        PROSESS_FLYT_MAP[ProsessType.OPPDATER_FAKTURAMOTTAKER] = ProsessFlyt(
            ProsessType.OPPDATER_FAKTURAMOTTAKER,
            OPPDATER_FAKTURAMOTTAKER
        )
    }

    @JvmStatic
    fun finnFlytForProsessType(prosessType: ProsessType): Optional<ProsessFlyt> {
        return Optional.ofNullable(PROSESS_FLYT_MAP[prosessType])
    }

    @JvmStatic
    fun hentNesteSteg(prosessType: ProsessType, sistFullfortSteg: ProsessSteg?): Optional<ProsessSteg> {
        return finnFlytForProsessType(prosessType)
            .map { it: ProsessFlyt ->
                try {
                    return@map it.nesteSteg(sistFullfortSteg)
                } catch (ignored: Exception) {
                    return@map null
                }
            }
    }
}

package no.nav.melosys.saksflyt.prosessflyt

import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessSteg.*
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.ProsessType.*
import java.util.*

object ProsessflytDefinisjon {
    private val PROSESS_FLYT_MAP: Map<ProsessType, ProsessFlyt> = mapOf(
        OPPRETT_OG_DISTRIBUER_BREV to ProsessFlyt(
            prosessType = OPPRETT_OG_DISTRIBUER_BREV,
            OPPRETT_OG_JOURNALFØR_BREV,
            DISTRIBUER_JOURNALPOST
        ),
        SEND_BREV to ProsessFlyt(
            prosessType = SEND_BREV,
            BESTILL_BREV
        ),
        HENLEGG_SAK to ProsessFlyt(
            prosessType = HENLEGG_SAK,
            SEND_HENLEGGELSESBREV
        ),
        VIDERESEND_SOKNAD to ProsessFlyt(
            prosessType = VIDERESEND_SOKNAD,
            AVKLAR_MYNDIGHET,
            SEND_ORIENTERINGSBREV_VIDERESENDING_SØKNAD,
            VIDERESEND_SØKNAD,
            DISTRIBUER_JOURNALPOST_UTLAND
        ),
        ANMODNING_OM_UNNTAK to ProsessFlyt(
            prosessType = ANMODNING_OM_UNNTAK,
            AVKLAR_MYNDIGHET,
            LAGRE_ANMODNINGSPERIODE_MEDL,
            SEND_ORIENTERING_ANMODNING_UNNTAK,
            SEND_ANMODNING_OM_UNNTAK,
            OPPDATER_OPPGAVE_ANMODNING_UNNTAK_SENDT
        ),
        REGISTRERE_UNNTAK_FRA_MEDLEMSKAP to ProsessFlyt(
            prosessType = REGISTRERE_UNNTAK_FRA_MEDLEMSKAP,
            LAGRE_LOVVALGSPERIODE_MEDL,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        JFR_NY_SAK_BRUKER to ProsessFlyt(
            prosessType = JFR_NY_SAK_BRUKER,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        ),
        JFR_NY_SAK_VIRKSOMHET to ProsessFlyt(
            prosessType = JFR_NY_SAK_VIRKSOMHET,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_ARKIVSAK,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPRETT_OPPGAVE
        ),
        JFR_ANDREGANG_REPLIKER_BEHANDLING to ProsessFlyt(
            prosessType = JFR_ANDREGANG_REPLIKER_BEHANDLING,
            REPLIKER_BEHANDLING,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPDATER_SAKSRELASJON,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        ),
        JFR_ANDREGANG_NY_BEHANDLING to ProsessFlyt(
            prosessType = JFR_ANDREGANG_NY_BEHANDLING,
            OPPRETT_NY_BEHANDLING,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPDATER_SAKSRELASJON,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        ),
        ANNULLER_SAK to ProsessFlyt(
            prosessType = ANNULLER_SAK,
            LAGRE_MEDLEMSKAPSPERIODE_MEDL,
            KANSELLER_FAKTURASERIE,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        JFR_KNYTT to ProsessFlyt(
            prosessType = JFR_KNYTT,
            OPPDATER_OG_FERDIGSTILL_JOURNALPOST,
            OPPDATER_SAKSRELASJON,
            OPPRETT_TIDLIGERE_JOURNALPOSTER_FOR_SAK,
            JFR_SETT_VURDER_DOKUMENT,
            JFR_TILDEL_BEHANDLINGSOPPGAVE,
            SEND_FORVALTNINGSMELDING
        ),
        OPPRETT_NY_SAK_EOS_FRA_OPPGAVE to ProsessFlyt(
            prosessType = OPPRETT_NY_SAK_EOS_FRA_OPPGAVE,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            GJENBRUK_OPPGAVE
        ),
        OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE to ProsessFlyt(
            prosessType = OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            GJENBRUK_OPPGAVE
        ),
        OPPRETT_SAK to ProsessFlyt(
            prosessType = OPPRETT_SAK,
            OPPRETT_SAK_OG_BEH,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            OPPRETT_ARKIVSAK,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE
        ),
        OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING to ProsessFlyt(
            prosessType = OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING,
            OPPRETT_MANGLENDE_INNBETALING_BEHANDLING,
            OPPRETT_OPPGAVE,
            SEND_MANGLENDE_INNBETALING_VARSELBREV
        ),
        MANGLENDE_INNBETALING_VARSELBREV to ProsessFlyt(
            prosessType = MANGLENDE_INNBETALING_VARSELBREV,
            SEND_MANGLENDE_INNBETALING_VARSELBREV
        ),
        OPPRETT_NY_BEHANDLING_AARSAVREGNING to ProsessFlyt(
            prosessType = OPPRETT_NY_BEHANDLING_AARSAVREGNING,
            OPPRETT_AARSAVREGNING_BEHANDLING,
            OPPRETT_OPPGAVE
        ),
        OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to ProsessFlyt(
            prosessType = OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK,
            REPLIKER_BEHANDLING,
            OPPRETT_OPPGAVE
        ),
        OPPRETT_NY_BEHANDLING_FOR_SAK to ProsessFlyt(
            prosessType = OPPRETT_NY_BEHANDLING_FOR_SAK,
            OPPRETT_NY_BEHANDLING,
            OPPRETT_MOTTATTEOPPLYSNINGER,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE
        ),
        SATSENDRING to ProsessFlyt(
            prosessType = SATSENDRING,
            OPPRETT_SATSBEHANDLING,
            BEREGN_OG_SEND_FAKTURA,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        SATSENDRING_TILBAKESTILL_NY_VURDERING to ProsessFlyt(
            prosessType = SATSENDRING_TILBAKESTILL_NY_VURDERING,
            OPPRETT_SATSBEHANDLING,
            BEREGN_OG_SEND_FAKTURA,
            AVSLUTT_SAK_OG_BEHANDLING,
            TILBAKESTILL_TRYGDEAVGIFT
        ),
        IVERKSETT_VEDTAK_AARSAVREGNING to ProsessFlyt(
            prosessType = IVERKSETT_VEDTAK_AARSAVREGNING,
            SEND_FAKTURA_AARSAVREGNING,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK,
        ),
        IVERKSETT_VEDTAK_EOS to ProsessFlyt(
            prosessType = IVERKSETT_VEDTAK_EOS,
            AVKLAR_MYNDIGHET,
            AVKLAR_ARBEIDSGIVER,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_VEDTAKSBREV_INNLAND,
            SEND_VEDTAK_UTLAND,
            DISTRIBUER_JOURNALPOST_UTLAND,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        ),
        IVERKSETT_EOS_PENSJONIST_AVGIFT to ProsessFlyt(
            prosessType = IVERKSETT_EOS_PENSJONIST_AVGIFT,
            OPPRETT_FAKTURASERIE,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_ORIENTERINGSBREV_TRYGDEAVGIFT
        ),
        IVERKSETT_VEDTAK_FTRL to ProsessFlyt(
            prosessType = IVERKSETT_VEDTAK_FTRL,
            LAGRE_MEDLEMSKAPSPERIODE_MEDL,
            OPPRETT_FAKTURASERIE,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        ),
        IVERKSETT_VEDTAK_IKKE_YRKESAKTIV to ProsessFlyt(
            prosessType = IVERKSETT_VEDTAK_IKKE_YRKESAKTIV,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_VEDTAKSBREV_INNLAND,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        ),
        IVERKSETT_VEDTAK_TRYGDEAVTALE to ProsessFlyt(
            prosessType = IVERKSETT_VEDTAK_TRYGDEAVTALE,
            AVKLAR_MYNDIGHET,
            AVKLAR_ARBEIDSGIVER,
            LAGRE_LOVVALGSPERIODE_MEDL,
            AVSLUTT_SAK_OG_BEHANDLING,
            SEND_MELDING_OM_VEDTAK
        ),
        MOTTAK_SED to ProsessFlyt(
            prosessType = MOTTAK_SED,
            SED_MOTTAK_RUTING
        ),
        MOTTAK_SED_JOURNALFØRING to ProsessFlyt(
            prosessType = MOTTAK_SED_JOURNALFØRING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST
        ),
        REGISTRERING_UNNTAK_NY_SAK to ProsessFlyt(
            prosessType = REGISTRERING_UNNTAK_NY_SAK,
            SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH,
            OPPRETT_ARKIVSAK,
            OPPDATER_SAKSRELASJON,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_SEDDOKUMENT,
            HENT_REGISTEROPPLYSNINGER,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        ),
        REGISTRERING_UNNTAK_NY_BEHANDLING to ProsessFlyt(
            prosessType = REGISTRERING_UNNTAK_NY_BEHANDLING,
            SED_MOTTAK_OPPRETT_NY_BEHANDLING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            AVSLUTT_TIDLIGERE_MEDL_PERIODE,
            OPPRETT_SEDDOKUMENT,
            HENT_REGISTEROPPLYSNINGER,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        ),
        ARBEID_FLERE_LAND_NY_SAK to ProsessFlyt(
            prosessType = ARBEID_FLERE_LAND_NY_SAK,
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
        ),
        ARBEID_FLERE_LAND_NY_BEHANDLING to ProsessFlyt(
            prosessType = ARBEID_FLERE_LAND_NY_BEHANDLING,
            SED_MOTTAK_OPPRETT_NY_BEHANDLING,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            AVSLUTT_TIDLIGERE_MEDL_PERIODE,
            OPPRETT_SEDDOKUMENT,
            OPPRETT_SED_GRUNNLAG,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            REGISTERKONTROLL,
            BESTEM_BEHANDLINGMÅTE_SED
        ),
        ANMODNING_OM_UNNTAK_SVAR to ProsessFlyt(
            prosessType = ANMODNING_OM_UNNTAK_SVAR,
            SED_MOTTAK_FERDIGSTILL_JOURNALPOST,
            OPPRETT_ANMODNINGSPERIODESVAR,
            BESTEM_BEHANDLINGSMÅTE_SVAR_ANMODNING_UNNTAK
        ),
        ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK to ProsessFlyt(
            prosessType = ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK,
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
        ),
        ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING to ProsessFlyt(
            prosessType = ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING,
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
        ),
        REGISTRERING_UNNTAK_GODKJENN to ProsessFlyt(
            prosessType = REGISTRERING_UNNTAK_GODKJENN,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_GODKJENNING_REGISTRERING_UNNTAK,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        REGISTRERING_UNNTAK_AVVIS to ProsessFlyt(
            prosessType = REGISTRERING_UNNTAK_AVVIS,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        UTPEKING_AVVIS to ProsessFlyt(
            prosessType = UTPEKING_AVVIS,
            UTPEKING_SEND_AVSLAG,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        ANMODNING_OM_UNNTAK_MOTTAK_SVAR to ProsessFlyt(
            prosessType = ANMODNING_OM_UNNTAK_MOTTAK_SVAR,
            LAGRE_LOVVALGSPERIODE_MEDL,
            SEND_SVAR_ANMODNING_UNNTAK,
            AVSLUTT_SAK_OG_BEHANDLING
        ),
        MOTTAK_SOKNAD_ALTINN to ProsessFlyt(
            prosessType = MOTTAK_SOKNAD_ALTINN,
            OPPRETT_SAK_OG_BEHANDLING_FRA_ALTINN_SØKNAD,
            OPPRETT_ARKIVSAK,
            OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN,
            HENT_REGISTEROPPLYSNINGER,
            VURDER_INNGANGSVILKÅR,
            OPPRETT_OPPGAVE,
            SEND_FORVALTNINGSMELDING
        ),
        ProsessType.OPPDATER_FAKTURAMOTTAKER to ProsessFlyt(
            prosessType = ProsessType.OPPDATER_FAKTURAMOTTAKER,
            ProsessSteg.OPPDATER_FAKTURAMOTTAKER
        )
    )

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

package no.nav.melosys.service.oppgave

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFactoryGammelMappingTest {

    // Testene er på format hva som er input i lagBehandlingsoppgave:
    // sakstype_sakstema_behandlignstype_behandlingstema_radIConfluenceTabell

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_flere_1() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.ENDRET_PERIODE,
            ),
            melosysBehandlingstemaer = listOf(
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Behandlingstema.UTSENDT_SELVSTENDIG,
                Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
                Behandlingstema.ARBEID_FLERE_LAND,
                Behandlingstema.ARBEID_KUN_NORGE
            ),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_beslutningLovvalgNorge_2() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.ENDRET_PERIODE,
                Behandlingstyper.KLAGE,
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = OppgaveBehandlingstype.EOS_LOVVALG_NORGE,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SED
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_trygdetid_3() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.TRYGDETID),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SED
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_beggeYrkesaktiv_4() {
        test(
            sakstyper = listOf(Sakstyper.FTRL),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV),
            expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_standard_beggeYrkesaktiv_5() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV),
            expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_unntakMedlemskap_6() {
        test(
            sakstyper = listOf(Sakstyper.FTRL),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.UNNTAK_MEDLEMSKAP),
            expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_standard_pensjonist_7() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.PENSJONIST),
            expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = Sakstyper.EU_EOS.beskrivelse
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_standard_pensjonist_7() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.PENSJONIST),
            expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = Sakstyper.TRYGDEAVTALE.beskrivelse
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_pensjonist_7() {
        test(
            sakstyper = listOf(Sakstyper.FTRL),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.PENSJONIST),
            expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = Sakstyper.FTRL.beskrivelse
        )
    }

    @Test
    fun alle_trygdeavgift_standard_pensjonist_8() {
        test(
            sakstyper = Sakstyper.values().toList(),
            sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.PENSJONIST),
            expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET,
            expectedBehandlingstype = null,
            expectedTema = Tema.TRY,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun alle_trygdeavgift_standard_yrkesaktiv_9() {
        test(
            sakstyper = Sakstyper.values().toList(),
            sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.YRKESAKTIV),
            expectedBehandlingstema = OppgaveBehandlingstema.YRKESAKTIV,
            expectedBehandlingstype = null,
            expectedTema = Tema.TRY,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun eueos_unntak_flere_annmodningUnntakHovedregel_10() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
            melosysBehandlingstemaer = listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
            expectedBehandlingstema = OppgaveBehandlingstema.ANMODNING_UNNTAK,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = "SEDA001"
        )
    }

    @Test
    fun eueos_unntak_flere_registreringUnntakNorskTrygdUtstasjonering_11() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
            melosysBehandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING),
            expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = "SEDA009"
        )
    }

    @Test
    fun eueos_unntak_flere_registreringUnntakNorskTrygdOvrige_11() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
            melosysBehandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE),
            expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = "SEDA010"
        )
    }

    @Test
    fun eueos_unntak_flere_beslutningLovvalgAnnetLand_11() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
            melosysBehandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND),
            expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = "SEDA003"
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_foresporselTrygdemyndighet_12() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = "SEDA005"
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_henvendelse_foresporselTrygdemyndighet_12() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),

            expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK
        )
    }

    @Test
    fun eueos_unntak_henvendelse_foresporselTrygdemyndighet_13() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),

            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = "SEDA005"
        )
    }

    @Test
    fun trygdeavtale_unntak_henvendelse_foresporselTrygdemyndighet_13() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),

            expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK
        )
    }

    @Test
    fun trygdeavtale_unntak_flere_anmodningOmUnntakHovedregel_14() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
            melosysBehandlingstemaer = listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),

            expectedBehandlingstema = OppgaveBehandlingstema.ANMODNING_UNNTAK,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun trygdeavtale_unntak_standard_registreringUnntak_15() {

        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK),
            expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK,

            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_beslutningLovvalgNorge_16() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE),

            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = OppgaveBehandlingstype.EOS_LOVVALG_NORGE,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_utsendtArbeidstaker_16() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.UTSENDT_ARBEIDSTAKER),

            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_henvendelse_alle_16() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = finnAlleBehandlingstemaUnntatt(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),

            expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_henvendelse_alle_16() {
        test(
            sakstyper = listOf(Sakstyper.FTRL),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = Behandlingstema.values().toList(),

            expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun eueos_unntak_henvendelse_ikkeSed_17() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun trygdeavtale_unntak_henvendelse_alle_17() {
        test(
            sakstyper = listOf(Sakstyper.TRYGDEAVTALE),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = finnAlleBehandlingstemaUnntatt(
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            ),
            expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun ftrl_unntak_henvendelse_alle_17() {
        test(
            sakstyper = listOf(Sakstyper.FTRL),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = Behandlingstema.values().toList(),
            expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.VURD_HENV
        )
    }

    @Test
    fun alle_trygdeavgift_henvendelse_pensjonist_18() {
        test(
            sakstyper = Sakstyper.values().toList(),
            sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.PENSJONIST),
            expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET,
            expectedBehandlingstype = null,
            expectedTema = Tema.TRY,
            expectedOppgavetype = Oppgavetyper.VURD_HENV,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun alle_trygdeavgift_henvendelse_yrkesaktiv_18() {
        test(
            sakstyper = Sakstyper.values().toList(),
            sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT),
            behandlingstyper = listOf(Behandlingstyper.HENVENDELSE),
            melosysBehandlingstemaer = listOf(Behandlingstema.YRKESAKTIV),
            expectedBehandlingstema = OppgaveBehandlingstema.YRKESAKTIV,
            expectedBehandlingstype = null,
            expectedTema = Tema.TRY,
            expectedOppgavetype = Oppgavetyper.VURD_HENV,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_soeknad_foresporselTrygdemyndighet_xx() {
        test(
            sakstype = Sakstyper.EU_EOS,
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            behandlingstype = Behandlingstyper.FØRSTEGANG,
            melosysBehandlingstema = Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SED,
            forventetBegrunnelse = Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET.beskrivelse
        )
    }

    @Test
    fun `A1_ANMODNING_OM_UNNTAK_PAPIR skal gi gosys oppgave EU_EOS_LAND, UFM, BEH_SAK_MK`() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.UNNTAK),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE
            ),
            melosysBehandlingstemaer = listOf(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.UFM,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR.beskrivelse
        )
    }

    @Test
    fun ftrl_trygdeavgift_soeknad_arbeidFlereLand_xx() {
        test(
            sakstype = Sakstyper.FTRL,
            sakstema = Sakstemaer.TRYGDEAVGIFT,
            behandlingstype = Behandlingstyper.FØRSTEGANG,
            melosysBehandlingstema = Behandlingstema.ARBEID_FLERE_LAND,
            expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND,
            expectedBehandlingstype = null,
            expectedTema = Tema.TRY,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            forventetBegrunnelse = Behandlingstema.ARBEID_FLERE_LAND.beskrivelse
        )
    }

    private fun test(
        sakstyper: Collection<Sakstyper>,
        sakstemaer: Collection<Sakstemaer>,
        behandlingstyper: Collection<Behandlingstyper>,
        melosysBehandlingstemaer: Collection<Behandlingstema>,
        expectedBehandlingstema: OppgaveBehandlingstema?,
        expectedBehandlingstype: OppgaveBehandlingstype?,
        expectedTema: Tema,
        expectedOppgavetype: Oppgavetyper,
        forventetBegrunnelse: String? = null
    ) {
        sakstyper.forEach { sakstype ->
            sakstemaer.forEach { sakstema ->
                behandlingstyper.forEach { behandlignstype ->
                    melosysBehandlingstemaer.forEach { melosysBehandlingstema ->
                        test(
                            sakstype,
                            sakstema,
                            behandlignstype,
                            melosysBehandlingstema,
                            expectedBehandlingstema,
                            expectedBehandlingstype,
                            expectedTema,
                            expectedOppgavetype,
                            forventetBegrunnelse ?: melosysBehandlingstema.beskrivelse
                        )
                    }
                }
            }
        }
    }

    private fun test(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        melosysBehandlingstema: Behandlingstema,
        expectedBehandlingstema: OppgaveBehandlingstema?,
        expectedBehandlingstype: OppgaveBehandlingstype?,
        expectedTema: Tema,
        expectedOppgavetype: Oppgavetyper,
        forventetBegrunnelse: String
    ) {
        val behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                type = sakstype
                tema = sakstema

            }
            type = behandlingstype
            tema = melosysBehandlingstema
        }

        val fakeUnleash = FakeUnleash().apply { disableAll() }
        val oppgave = OppgaveFactory(fakeUnleash).lagBehandlingsoppgave(behandling, LocalDate.now(), behandling::hentSedDokument).build()

        withClue(
            "\nsakstype:               $sakstype " +
                    "\nsakstema:               $sakstema " +
                    "\nbehandlingstype:        $behandlingstype " +
                    "\nmelosysBehandlingstema: $melosysBehandlingstema"
        ) {
            withClue("oppgave.behandlingstema") {
                oppgave.behandlingstema.shouldBe(expectedBehandlingstema?.kode)
            }
            withClue("oppgave.behandlingstype") {
                oppgave.behandlingstype.shouldBe(expectedBehandlingstype?.kode)
            }
            withClue("oppgave.tema") {
                oppgave.tema.shouldBe(expectedTema)
            }
            withClue("oppgave.oppgavetype") {
                oppgave.oppgavetype.shouldBe(expectedOppgavetype)
            }
            withClue("oppgave.beskrivelse") {
                oppgave.beskrivelse.shouldBe(forventetBegrunnelse)
            }
        }
    }

    private fun finnAlleBehandlingstemaUnntatt(vararg ekskluderteBehandlingstema: Behandlingstema) =
        Behandlingstema.values().filter { !ekskluderteBehandlingstema.contains(it) }
}

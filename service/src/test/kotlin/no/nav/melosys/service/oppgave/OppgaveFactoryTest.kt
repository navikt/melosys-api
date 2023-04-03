package no.nav.melosys.service.oppgave

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
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

class OppgaveFactoryTest {

    // Testene er på format hva som er input i lagBehandlingsoppgave:
    // sakstype_sakstema_behandlignstype_behandlingstema_radIConfluenceTabell

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_flere_1() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(
            Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.ENDRET_PERIODE,
        )
        val behandlingstemaer = listOf(
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstema.UTSENDT_SELVSTENDIG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
            Behandlingstema.ARBEID_FLERE_LAND,
            Behandlingstema.ARBEID_KUN_NORGE
        )

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_beslutningLovvalgNorge_2() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.ENDRET_PERIODE,
            Behandlingstyper.KLAGE,
        )
        val behandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = OppgaveBehandlingstype.EOS_LOVVALG_NORGE
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_trygdetid_3() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.TRYGDETID)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_beggeYrkesaktiv_4() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_standard_beggeYrkesaktiv_5() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_unntakMedlemskap_6() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.UNNTAK_MEDLEMSKAP)

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_standard_pensjonist_7() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = Sakstyper.EU_EOS.beskrivelse
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_standard_pensjonist_7() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = Sakstyper.TRYGDEAVTALE.beskrivelse
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_pensjonist_7() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = Sakstyper.FTRL.beskrivelse
        )
    }

    @Test
    fun alle_trygdeavgift_standard_pensjonist_8() {
        val sakstyper = Sakstyper.values().toList()
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedBehandlingstype = null
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun alle_trygdeavgift_standard_yrkesaktiv_9() {
        val sakstyper = Sakstyper.values().toList()
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.YRKESAKTIV
        val expectedBehandlingstype = null
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun eueos_unntak_flere_annmodningUnntakHovedregel_10() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)

        val expectedBehandlingstema = OppgaveBehandlingstema.ANMODNING_UNNTAK
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = "SEDA001"
        )
    }

    @Test
    fun eueos_unntak_flere_registreringUnntakNorskTrygdUtstasjonering_11() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = "SEDA009"
        )
    }

    @Test
    fun eueos_unntak_flere_registreringUnntakNorskTrygdOvrige_11() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = "SEDA010"
        )
    }

    @Test
    fun eueos_unntak_flere_beslutningLovvalgAnnetLand_11() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = "SEDA003"
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_foresporselTrygdemyndighet_12() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = "SEDA005"
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_henvendelse_foresporselTrygdemyndighet_12() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun eueos_unntak_henvendelse_foresporselTrygdemyndighet_13() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = "SEDA005"
        )
    }

    @Test
    fun trygdeavtale_unntak_henvendelse_foresporselTrygdemyndighet_13() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_unntak_flere_anmodningOmUnntakHovedregel_14() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)

        val expectedBehandlingstema = OppgaveBehandlingstema.ANMODNING_UNNTAK
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun trygdeavtale_unntak_standard_registreringUnntak_15() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_beslutningLovvalgNorge_16() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = OppgaveBehandlingstype.EOS_LOVVALG_NORGE
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_utsendtArbeidstaker_16() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.UTSENDT_ARBEIDSTAKER)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_henvendelse_alle_16() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = finnAlleBehandlingstemaUnntatt(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_henvendelse_alle_16() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = Behandlingstema.values().toList()

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun eueos_unntak_henvendelse_ikkeSed_17() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_unntak_henvendelse_alle_17() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = finnAlleBehandlingstemaUnntatt(
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun ftrl_unntak_henvendelse_alle_17() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = Behandlingstema.values().toList()

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedBehandlingstype = null
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype
        )
    }

    @Test
    fun alle_trygdeavgift_henvendelse_pensjonist_18() {
        val sakstyper = Sakstyper.values().toList()
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedBehandlingstype = null
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = ""
        )
    }

    @Test
    fun alle_trygdeavgift_henvendelse_yrkesaktiv_18() {
        val sakstyper = Sakstyper.values().toList()
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.YRKESAKTIV
        val expectedBehandlingstype = null
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = expectedBehandlingstype,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
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
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        val behandlingstemaer = listOf(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper = sakstyper,
            sakstemaer = sakstemaer,
            behandlingstyper = behandlingstyper,
            melosysBehandlingstemaer = behandlingstemaer,
            expectedBehandlingstema = expectedBehandlingstema,
            expectedBehandlingstype = null,
            expectedTema = expectedTema,
            expectedOppgavetype = expectedOppgavetype,
            forventetBegrunnelse = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR.beskrivelse
        )
    }

    @Test
    fun ftrl_trygdeavgift_soeknad_arbeidFlereLand_xx() {
        test(
            Sakstyper.FTRL,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ARBEID_FLERE_LAND,
            OppgaveBehandlingstema.UTENFOR_AVTALELAND,
            null,
            Tema.TRY,
            Oppgavetyper.BEH_SAK_MK,
            Behandlingstema.ARBEID_FLERE_LAND.beskrivelse
        )
    }

    private fun test(
        sakstyper: Collection<Sakstyper>,
        sakstemaer: Collection<Sakstemaer>,
        behandlingstyper: Collection<Behandlingstyper>,
        melosysBehandlingstemaer: Collection<Behandlingstema>,
        expectedBehandlingstema: OppgaveBehandlingstema,
        expectedBehandlingstype: OppgaveBehandlingstype?,
        expectedTema: Tema,
        expectedOppgavetype: Oppgavetyper,
        forventetBegrunnelse: String? = null
    ) {
        for (sakstype in sakstyper) {
            for (sakstema in sakstemaer) {
                for (behandlignstype in behandlingstyper) {
                    for (melosysBehandlingstema in melosysBehandlingstemaer) {
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
        expectedBehandlingstema: OppgaveBehandlingstema,
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

        val oppgave = OppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()).build()

        withClue(
            "\nsakstype:               $sakstype " +
                "\nsakstema:               $sakstema " +
                "\nbehandlingstype:        $behandlingstype " +
                "\nmelosysBehandlingstema: $melosysBehandlingstema"
        ) {
            withClue("oppgave.behandlingstema") {
                oppgave.behandlingstema.shouldBe(expectedBehandlingstema.kode)
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

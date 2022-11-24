package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OppgaveFactoryTest {

    // Testene er på format hva som er input i lagBehandlingsoppgave:
    // sakstype_sakstema_behandlignstype_behandlingstema_radIConfluenceTabell

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_flere_1() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD_MED_ENDRET_PERIODE
        val behandlingstemaer = listOf(
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstema.UTSENDT_SELVSTENDIG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
            Behandlingstema.ARBEID_FLERE_LAND,
            Behandlingstema.ARBEID_KUN_NORGE
        )

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_beslutningLovvalgNorge_2() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD_MED_ENDRET_PERIODE
        val behandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_trygdetid_3() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.TRYGDETID)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_beggeYrkesaktiv_4() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_standard_beggeYrkesaktiv_5() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV, Behandlingstema.IKKE_YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_unntakMedlemskap_6() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.UNNTAK_MEDLEMSKAP)

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_standard_pensjonist_7() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            Sakstyper.EU_EOS.beskrivelse
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_standard_pensjonist_7() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            Sakstyper.TRYGDEAVTALE.beskrivelse
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_standard_pensjonist_7() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            Sakstyper.FTRL.beskrivelse
        )
    }

    @Test
    fun alle_trygdeavgift_standard_pensjonist_8() {
        val sakstyper = SAKSTYPE_ALLE
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            ""
        )
    }

    @Test
    fun alle_trygdeavgift_standard_yrkesaktiv_9() {
        val sakstyper = SAKSTYPE_ALLE
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.YRKESAKTIV
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            ""
        )
    }

    @Test
    fun eueos_unntak_flere_annmodningUnntakHovedregel_10() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)

        val expectedBehandlingstema = OppgaveBehandlingstema.ANMODNING_UNNTAK
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            "SEDA001"
        )
    }

    @Test
    fun eueos_unntak_flere_registreringUnntakNorskTrygdUtstasjonering_11() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            "SEDA009"
        )
    }

    @Test
    fun eueos_unntak_flere_registreringUnntakNorskTrygdOvrige_11() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            "SEDA010"
        )
    }

    @Test
    fun eueos_unntak_flere_beslutningLovvalgAnnetLand_11() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SED

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            "SEDA003"
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_foresporselTrygdemyndighet_12() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            "SEDA005"
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_henvendelse_foresporselTrygdemyndighet_12() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun eueos_unntak_henvendelse_foresporselTrygdemyndighet_13() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            "SEDA005"
        )
    }

    @Test
    fun trygdeavtale_unntak_henvendelse_foresporselTrygdemyndighet_13() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_unntak_flere_anmodningOmUnntakHovedregel_14() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        val behandlingstemaer = listOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)

        val expectedBehandlingstema = OppgaveBehandlingstema.ANMODNING_UNNTAK
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            ""
        )
    }

    @Test
    fun trygdeavtale_unntak_standard_registreringUnntak_15() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = BEHANDLINGSTYPE_STANDARD
        val behandlingstemaer = listOf(Behandlingstema.REGISTRERING_UNNTAK)

        val expectedBehandlingstema = OppgaveBehandlingstema.REGISTRERING_UNNTAK
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.BEH_SAK_MK

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            ""
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_henvendelse_alle_16() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer =
            getAlleBehandlingstemaUnntatt(Behandlingstema.TRYGDETID, Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_medlemskapLovvalg_henvendelse_alle_16() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = getAlleBehandlingstemaUnntatt(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun ftrl_medlemskapLovvalg_henvendelse_alle_16() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = BEHANDLINGSTEMA_ALLE

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedTema = Tema.MED
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun eueos_unntak_henvendelse_alle_17() {
        val sakstyper = listOf(Sakstyper.EU_EOS)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = getAlleBehandlingstemaUnntatt(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        val expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_LAND
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun trygdeavtale_unntak_henvendelse_alle_17() {
        val sakstyper = listOf(Sakstyper.TRYGDEAVTALE)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = getAlleBehandlingstemaUnntatt(
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )

        val expectedBehandlingstema = OppgaveBehandlingstema.AVTALELAND
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun ftrl_unntak_henvendelse_alle_17() {
        val sakstyper = listOf(Sakstyper.FTRL)
        val sakstemaer = listOf(Sakstemaer.UNNTAK)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = BEHANDLINGSTEMA_ALLE

        val expectedBehandlingstema = OppgaveBehandlingstema.UTENFOR_AVTALELAND
        val expectedTema = Tema.UFM
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype
        )
    }

    @Test
    fun alle_trygdeavgift_henvendelse_pensjonist_18() {
        val sakstyper = SAKSTYPE_ALLE
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.PENSJONIST)

        val expectedBehandlingstema = OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            ""
        )
    }

    @Test
    fun alle_trygdeavgift_henvendelse_yrkesaktiv_18() {
        val sakstyper = SAKSTYPE_ALLE
        val sakstemaer = listOf(Sakstemaer.TRYGDEAVGIFT)
        val behandlingstyper = listOf(Behandlingstyper.HENVENDELSE)
        val behandlingstemaer = listOf(Behandlingstema.YRKESAKTIV)

        val expectedBehandlingstema = OppgaveBehandlingstema.YRKESAKTIV
        val expectedTema = Tema.TRY
        val expectedOppgavetype = Oppgavetyper.VURD_HENV

        test(
            sakstyper,
            sakstemaer,
            behandlingstyper,
            behandlingstemaer,
            expectedBehandlingstema,
            expectedTema,
            expectedOppgavetype,
            ""
        )
    }

    @Test
    fun eueos_medlemskapLovvalg_soeknad_foresporselTrygdemyndighet_xx() {
        test(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstyper.SOEKNAD,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            OppgaveBehandlingstema.EU_EOS_LAND,
            Tema.MED,
            Oppgavetyper.BEH_SAK_MK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET.beskrivelse
        )
    }

    @Test
    fun ftrl_trygdeavgift_soeknad_arbeidFlereLand_xx() {
        test(
            Sakstyper.FTRL,
            Sakstemaer.TRYGDEAVGIFT,
            Behandlingstyper.SOEKNAD,
            Behandlingstema.ARBEID_FLERE_LAND,
            OppgaveBehandlingstema.UTENFOR_AVTALELAND,
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
        expectedTema: Tema,
        expectedOppgavetype: Oppgavetyper,
        forventetBegrunnelse: String
    ) {
        val fagsak = Fagsak()
        fagsak.type = sakstype
        fagsak.tema = sakstema
        val behandling = Behandling()
        behandling.fagsak = fagsak
        behandling.type = behandlingstype
        behandling.tema = melosysBehandlingstema

        val oppgave = OppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()).build()

        Assertions.assertThat(oppgave.behandlingstema)
            .`as`("Behandlingstema (${sakstype}, ${sakstema}, ${behandlingstype}, ${melosysBehandlingstema})")
            .isEqualTo(expectedBehandlingstema.kode)
        Assertions.assertThat(oppgave.tema)
            .`as`("Tema (${sakstype}, ${sakstema}, ${behandlingstype}, ${melosysBehandlingstema})")
            .isEqualTo(expectedTema)
        Assertions.assertThat(oppgave.oppgavetype)
            .`as`("Oppgavetype (${sakstype}, ${sakstema}, ${behandlingstype}, ${melosysBehandlingstema})")
            .isEqualTo(expectedOppgavetype)
        Assertions.assertThat(oppgave.beskrivelse)
            .`as`("Beskrivelse (${sakstype}, ${sakstema}, ${behandlingstype}, ${melosysBehandlingstema})")
            .isEqualTo(forventetBegrunnelse)
    }

    private fun getAlleBehandlingstemaUnntatt(vararg ekskluderteBehandlingstema: Behandlingstema): Collection<Behandlingstema> {
        val ekskluderteBehandlingstemaList = Arrays.stream(ekskluderteBehandlingstema).toList()
        return BEHANDLINGSTEMA_ALLE.stream()
            .filter { tema: Behandlingstema -> !ekskluderteBehandlingstemaList.contains(tema) }
            .toList()
    }

    companion object {
        private val SAKSTYPE_ALLE: Collection<Sakstyper> = listOf(*Sakstyper.values())
        private val BEHANDLINGSTEMA_ALLE: Collection<Behandlingstema> = listOf(*Behandlingstema.values())
        private val BEHANDLINGSTYPE_STANDARD: Collection<Behandlingstyper> =
            listOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        private val BEHANDLINGSTYPE_STANDARD_MED_ENDRET_PERIODE: Collection<Behandlingstyper> = listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.ENDRET_PERIODE,
            Behandlingstyper.ENDRET_PERIODE
        )
    }
}

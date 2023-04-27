package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

internal class OppgaveGosysMapping {

    internal fun finnOppgave(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): Oppgave = finnOppgaveFraTabell(sakstype, sakstema, behandlingstema, behandlingstype)
        ?: finnOppgaveVedBehandlingstypeHenvendelse(sakstype, behandlingstema)
        ?: throw IllegalStateException(
            "Fant ikke oppgave mapping for " +
                "sakstype:$sakstype, sakstema:$sakstema, behandlingstema:$behandlingstema, behandlingstype:$behandlingstype"
        )

    // https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
    internal fun finnOppgaveFraTabell(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): Oppgave? = rows.find {
        it.sakstype == sakstype && it.sakstema == sakstema && behandlingstype in it.behandlingstype && behandlingstema in it.behandlingstema
    }?.oppgave

    fun finnOppgaveVedBehandlingstypeHenvendelse(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
    ): Oppgave? = rows.find {
        it.sakstype == sakstype && behandlingstema in it.behandlingstema
    }?.oppgave?.let {
        Oppgave(
            oppgaveBehandlingstema = it.oppgaveBehandlingstema,
            oppgaveType = Oppgavetyper.VURD_HENV,
            tema = it.tema,
            beskrivelsefelt = Beskrivelsefelt.SED_ELLER_TOMT
        )
    }


    internal enum class Beskrivelsefelt(val beskrivelse: String) {
        TOMT(""),
        SED(""),
        SED_ELLER_TOMT(""),
        A1_ANMODNING_OM_UNNTAK_PAPIR(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR.beskrivelse)
    }

    internal data class Oppgave(
        val oppgaveBehandlingstema: OppgaveBehandlingstema,
        val tema: Tema,
        val oppgaveType: Oppgavetyper,
        val beskrivelsefelt: Beskrivelsefelt
    )

    internal data class TableRow(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstype: Set<Behandlingstyper>,
        val behandlingstema: Set<Behandlingstema>,
        val oppgave: Oppgave
    )

    // Skal være samme data som fylt inn tabellen: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
    // Laget av OppgaveGosysMappingCodeGenerator
    internal val rows by lazy {
        listOf(
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstyper.KLAGE
                ),
                setOf(
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingstema.UTSENDT_SELVSTENDIG,
                    Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
                    Behandlingstema.ARBEID_FLERE_LAND,
                    Behandlingstema.ARBEID_KUN_NORGE
                ),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_YRKESAKTIV,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstyper.KLAGE
                ),
                setOf(Behandlingstema.PENSJONIST),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_PENSJONIST_ELLER_UFORETRYGDET,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstyper.KLAGE
                ),
                setOf(Behandlingstema.IKKE_YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_IKKE_YRKESAKTIV,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstyper.KLAGE
                ),
                setOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_NORGE_ER_UTPEKT_SOM_LOVVALGSLAND,
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.TRYGDETID),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_FORESPORSEL_OM_TRYGDETID,
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_YRKESAKTIV,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_PENSJONIST_ELLER_UFORETRYGDET,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.IKKE_YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_IKKE_YRKESAKTIV,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.IKKE_YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_IKKE_YRKESAKTIV,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_PENSJONIST_ELLER_UFORETRYGDET,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_YRKESAKTIV,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.UNNTAK_MEDLEMSKAP),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_SOKNAD_OM_UNNTAK,
                    Tema.UFM,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_PENSJONIST_ELLER_UFORETRYGDET,
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_PENSJONIST_ELLER_UFORETRYGDET,
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_PENSJONIST_ELLER_UFORETRYGDET,
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_YRKESAKTIV,
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_YRKESAKTIV,
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_YRKESAKTIV,
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
                setOf(
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
                ),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_MELDING_OM_UNNTAK_UNNTAK,
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
                setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_SOKNAD_OM_UNNTAK,
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
                setOf(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_SOKNAD_OM_UNNTAK,
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_FORESPORSEL_FRA_TRYGDEMYNDIGHET,
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_FORESPORSEL_FRA_TRYGDEMYNDIGHET,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.REGISTRERING_UNNTAK),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_MELDING_OM_UNNTAK,
                    Tema.UFM,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
                setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_SOKNAD_OM_UNNTAK,
                    Tema.UFM,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            )
        )
    }
}

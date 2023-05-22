package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

internal class OppgaveGosysMapping(private val unleash: Unleash) {

    private val teamaUtleder = OppgaveTemaUtleder()

    internal fun finnOppgave(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): Oppgave = finnOppgaveFraTabell(sakstype, sakstema, behandlingstema, behandlingstype)
        ?: finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet(sakstype, sakstema, behandlingstema, behandlingstype)
        ?: finnOppgaveVedBehandlingstypeHenvendelse(sakstype, sakstema, behandlingstema, behandlingstype)
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

    fun finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?,
    ): Oppgave? {
        if (behandlingstema != Behandlingstema.VIRKSOMHET) return null
        if (behandlingstype != Behandlingstyper.HENVENDELSE) return null
        return Oppgave(
            oppgaveBehandlingstema = null,
            oppgaveType = Oppgavetyper.VURD_HENV,
            tema = teamaUtleder.utledTema(sakstype, sakstema, behandlingstema),
            beskrivelsefelt = Beskrivelsefelt.TOMT,
            regelTruffet = Regel.HENVENDELSE_OG_VIRKSOMHET

        )
    }

    fun finnOppgaveVedBehandlingstypeHenvendelse(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): Oppgave? {
        if (behandlingstype != Behandlingstyper.HENVENDELSE) return null
        return rows.find {
            it.sakstype == sakstype && behandlingstema in it.behandlingstema
        }?.oppgave?.let {
            Oppgave(
                oppgaveBehandlingstema = it.oppgaveBehandlingstema,
                oppgaveType = Oppgavetyper.VURD_HENV,
                tema = teamaUtleder.utledTema(sakstype, sakstema, behandlingstema),
                beskrivelsefelt = Beskrivelsefelt.SED_ELLER_TOMT,
                regelTruffet = Regel.HENVENDELSE
            )
        }
    }


    internal enum class Beskrivelsefelt(val beskrivelse: String) {
        TOMT(""),
        SED(""),
        SED_ELLER_TOMT(""),
        BEHANDLINGSTEMA("Hent behandlingstemaet på behandlingen i Melosys"),
        A1_ANMODNING_OM_UNNTAK_PAPIR(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR.beskrivelse)
    }

    enum class Regel(val beskrivelse: String) {
        FRA_TABELL("tabell"),
        HENVENDELSE("henvendelse"),
        HENVENDELSE_OG_VIRKSOMHET("henv-virksomhet"),
        KUN_VED_MIGRERING("migrering")
    }
    internal data class Oppgave(
        val oppgaveBehandlingstema: OppgaveBehandlingstema?,
        val tema: Tema,
        val oppgaveType: Oppgavetyper,
        val beskrivelsefelt: Beskrivelsefelt,
        val regelTruffet: Regel = Regel.FRA_TABELL
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
                    OppgaveBehandlingstema.EU_EOS_YRKESAKTIV, // ab0483
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.BEHANDLINGSTEMA
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
                    OppgaveBehandlingstema.EU_EOS_PENSJONIST_ELLER_UFORETRYGDET, // ab0480
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
                    OppgaveBehandlingstema.EU_EOS_IKKE_YRKESAKTIV, // ab0481
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(
                    Behandlingstyper.FØRSTEGANG
                ),
                setOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_NORGE_ER_UTPEKT_SOM_LOVVALGSLAND, // ab0482
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.ENDRET_PERIODE,
                    Behandlingstyper.KLAGE
                ),
                setOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_NORGE_ER_UTPEKT_SOM_LOVVALGSLAND, // ab0482
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED_ELLER_TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.TRYGDETID),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_FORESPORSEL_OM_TRYGDETID, // ab0479
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED_ELLER_TOMT
                )
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                Oppgave(
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_YRKESAKTIV, // ab0484
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
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_PENSJONIST_ELLER_UFORETRYGDET, // ab0485
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
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_IKKE_YRKESAKTIV, // ab0486
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
                    OppgaveBehandlingstema.AVTALAND_IKKE_YRKESAKTIV, // ab0475
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
                    OppgaveBehandlingstema.AVTALAND_PENSJONIST_ELLER_UFORETRYGDET, // ab0476
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
                    OppgaveBehandlingstema.AVTALAND_YRKESAKTIV, // ab0477
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
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_SOKNAD_OM_UNNTAK, // ab0493
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
                    OppgaveBehandlingstema.EU_EOS_PENSJONIST_ELLER_UFORETRYGDET, // ab0480
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
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_PENSJONIST_ELLER_UFORETRYGDET, // ab0485
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
                    OppgaveBehandlingstema.AVTALAND_PENSJONIST_ELLER_UFORETRYGDET, // ab0476
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
                    OppgaveBehandlingstema.EU_EOS_YRKESAKTIV, // ab0483
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
                    OppgaveBehandlingstema.UTENFOR_AVTALAND_YRKESAKTIV, // ab0484
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
                    OppgaveBehandlingstema.AVTALAND_YRKESAKTIV, // ab0477
                    Tema.TRY,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG),
                setOf(
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
                ),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_MELDING_OM_UNNTAK_UNNTAK, // ab0490
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
                ),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_MELDING_OM_UNNTAK_UNNTAK, // ab0490
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED_ELLER_TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG),
                setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_SOKNAD_OM_UNNTAK, // ab0491
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_SOKNAD_OM_UNNTAK, // ab0491
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED_ELLER_TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_MELDING_OM_UNNTAK_UNNTAK, // ab0490
                    Tema.UFM,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_FORESPORSEL_FRA_TRYGDEMYNDIGHET, // ab0492
                    Tema.MED,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED_ELLER_TOMT
                )
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                Oppgave(
                    OppgaveBehandlingstema.EU_EOS_FORESPORSEL_FRA_TRYGDEMYNDIGHET, // ab0492
                    Tema.UFM,
                    Oppgavetyper.BEH_SED,
                    Beskrivelsefelt.SED_ELLER_TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                Oppgave(
                    null,
                    Tema.MED,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                Oppgave(
                    null,
                    Tema.UFM,
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
                    OppgaveBehandlingstema.AVTALAND_MELDING_OM_UNNTAK, // ab0488
                    Tema.UFM,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
                Oppgave(
                    OppgaveBehandlingstema.AVTALAND_SOKNAD_OM_UNNTAK, // ab0489
                    Tema.UFM,
                    Oppgavetyper.BEH_SAK_MK,
                    Beskrivelsefelt.TOMT
                )
            )
        ) + spesialReglerForMigringVedIkkeStøttetKombinasjon()
    }

    fun spesialReglerForMigringVedIkkeStøttetKombinasjon(): List<TableRow> =
        if (unleash.isEnabled(NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING))
            listOf(
                TableRow(
                    Sakstyper.EU_EOS,
                    Sakstemaer.MEDLEMSKAP_LOVVALG,
                    setOf(
                        Behandlingstyper.FØRSTEGANG,
                        Behandlingstyper.NY_VURDERING
                    ),
                    setOf(Behandlingstema.TRYGDETID),
                    Oppgave(
                        OppgaveBehandlingstema.EU_EOS_FORESPORSEL_OM_TRYGDETID,
                        Tema.MED,
                        Oppgavetyper.BEH_SED,
                        Beskrivelsefelt.SED,
                        Regel.KUN_VED_MIGRERING
                    )
                )
            ) else listOf()

    companion object {
        // Den skal kun settes av Oppgave migrering
        const val NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING = "melosys.ny_gosys_mapping_untakk_for_migrering"
    }
}

package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveBehandlingstemaNyUtleder : OppgaveBehandlingstemaUtleder {
    override fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema {
        return tableRows.find {
            it.sakstype == sakstype &&
                    it.sakstema == sakstema &&
                    it.behandlingstype.contains(behandlingstype) &&
                    it.behandlingstema.contains(behandlingstema)
        }?.getOppgaveBehandlingstema() ?: throw IllegalStateException("Fant ikke OppgaveBehandlingstema")
    }

    private data class TableRow(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstype: Set<Behandlingstyper>,
        val behandlingstema: Set<Behandlingstema>,
        val oppgavetema: String
    ) {
        fun getOppgaveBehandlingstema(): OppgaveBehandlingstema {
            return OppgaveBehandlingstema.valueOf(oppgavetema.uppercase())
        }
    }

    private val tableRows by lazy {
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
                "ab0483"
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
                "ab0480"
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
                "ab0482"
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.TRYGDETID),
                "ab0479"
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                "ab0484"
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                "ab0485"
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.IKKE_YRKESAKTIV),
                "ab0486"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.IKKE_YRKESAKTIV),
                "ab0475"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                "ab0476"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                "ab0477"
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.UNNTAK_MEDLEMSKAP),
                "ab0493"
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                "ab0480"
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                "ab0485"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.PENSJONIST),
                "ab0476"
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                "ab0483"
            ),
            TableRow(
                Sakstyper.FTRL,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                "ab0484"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.TRYGDEAVGIFT,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.YRKESAKTIV),
                "ab0477"
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
                "ab0490"
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
                setOf(
                    Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                    Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
                ),
                "ab0491"
            ),
            TableRow(
                Sakstyper.EU_EOS,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                "ab0492"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.MEDLEMSKAP_LOVVALG,
                setOf(Behandlingstyper.HENVENDELSE),
                setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET),
                "ab0478"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE),
                setOf(Behandlingstema.REGISTRERING_UNNTAK),
                "ab0488"
            ),
            TableRow(
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING),
                setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
                "ab0489"
            )
        )
    }
}

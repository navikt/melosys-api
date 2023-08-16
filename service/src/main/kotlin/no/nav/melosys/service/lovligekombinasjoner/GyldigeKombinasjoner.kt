package no.nav.melosys.service.lovligekombinasjoner

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class GyldigeKombinasjoner {

    companion object {
        @JvmStatic
        fun finnGyldige(
            sakstype: Sakstyper? = null,
            sakstema: Sakstemaer? = null,
            behandlingstype: Behandlingstyper? = null,
            behandlingstema: Behandlingstema? = null
        ): List<TableRow> {
            return rowsMelosysOgDatavarehus.filter {
                it.match(sakstype, sakstema, behandlingstype, behandlingstema)
            }
        }

        private data class Sak(
            val sakstype: Sakstyper,
            val sakstema: Sakstemaer,
            val behandlingstype: Behandlingstyper,
            val behandlingstema: Behandlingstema,
        )

        private fun TableRow.tilSak(): Sak =
            Sak(sakstype, sakstema, behandlingstype, behandlingstema)

        val rowsMelosysOgDatavarehus: List<TableRow> by lazy {
            (rowsMelosys + rowsDatavarehus).groupBy { it.tilSak() }.map { (sak, rows) ->
                if (rows.size > 2) {
                    throw IllegalStateException(
                        "Skal aldri ha mer enn to treff! fant:${rows.size} for:" +
                            rows.joinToString("\n")
                    )
                }
                if (rows.size == 1) rows.first() else TableRow(
                    sak.sakstype,
                    sak.sakstema,
                    sak.behandlingstype,
                    sak.behandlingstema,
                    Regel.Begge
                )
            }
        }

        val rowsMelosys: List<TableRow> by lazy { tableRows(tableRowsMelosys, Regel.MELOSYS) }

        private val rowsDatavarehus: List<TableRow> by lazy { tableRows(tableRowsDatavarehus, Regel.DVH) }

        private fun tableRows(rows: List<TableRowMedGruppering>, regel: Regel) = sequence {
            rows.forEach { row ->
                row.behandlingstyper.forEach { behandlingstype ->
                    row.behandlingstemaer.forEach { behandlingstema ->
                        yield(
                            TableRow(
                                row.sakstype,
                                row.sakstema,
                                behandlingstype,
                                behandlingstema,
                                regel
                            )
                        )
                    }
                }
            }
        }.toList()

        // https://confluence.adeo.no/display/TEESSI/Lovlige+kombinasjoner+av+sakstype%2C+sakstema%2C+behandlingstype+og+behandlingstema
        private val tableRowsMelosys = listOf(
            // EU_EOS
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingstema.UTSENDT_SELVSTENDIG,
                    Behandlingstema.ARBEID_FLERE_LAND,
                    Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
                    Behandlingstema.ARBEID_KUN_NORGE,
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingstema.PENSJONIST
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstyper = setOf(Behandlingstyper.HENVENDELSE),
                behandlingstemaer = setOf(
                    Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                    Behandlingstema.TRYGDETID
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstyper = setOf(Behandlingstyper.HENVENDELSE),
                behandlingstemaer = setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST
                )
            ),
            // FTRL
            TableRowMedGruppering(
                sakstype = Sakstyper.FTRL,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST,
                    Behandlingstema.UNNTAK_MEDLEMSKAP
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.FTRL,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingstema.PENSJONIST
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstyper = setOf(Behandlingstyper.HENVENDELSE),
                behandlingstemaer = setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                    Behandlingstema.REGISTRERING_UNNTAK
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstyper = setOf(Behandlingstyper.HENVENDELSE),
                behandlingstemaer = setOf(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                ),
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST
                )
            )
        )

        // https://confluence.adeo.no/display/TEESSI/Gyldige+kombinasjoner+av+sakstype%2C+sakstema%2C+behandlingstype+og+behandlingstema+-+dvh
        private val tableRowsDatavarehus = listOf(
            // Sakstyper.EU_EOS
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstemaer = setOf(
                    Behandlingstema.UTSENDT_ARBEIDSTAKER,
                    Behandlingstema.UTSENDT_SELVSTENDIG,
                    Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
                    Behandlingstema.ARBEID_FLERE_LAND,
                    Behandlingstema.ARBEID_KUN_NORGE,
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingstema.PENSJONIST,
                    Behandlingstema.BESLUTNING_LOVVALG_NORGE
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstemaer = setOf(
                    Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                    Behandlingstema.TRYGDETID,
                    Behandlingstema.VIRKSOMHET
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstemaer = setOf(
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                    Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                    Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstemaer = setOf(
                    Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                    Behandlingstema.VIRKSOMHET
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.EU_EOS,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),

            // Sakstyper.FTRL
            TableRowMedGruppering(
                sakstype = Sakstyper.FTRL,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstemaer = setOf(
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST,
                    Behandlingstema.UNNTAK_MEDLEMSKAP,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.FTRL,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstemaer = setOf(
                    Behandlingstema.VIRKSOMHET,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.FTRL,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),

            // Sakstyper.TRYGDEAVTALE
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstemaer = setOf(
                    Behandlingstema.IKKE_YRKESAKTIV,
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                behandlingstemaer = setOf(
                    Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                    Behandlingstema.VIRKSOMHET,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstemaer = setOf(
                    Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                    Behandlingstema.REGISTRERING_UNNTAK,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.UNNTAK,
                behandlingstemaer = setOf(
                    Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                    Behandlingstema.VIRKSOMHET,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.HENVENDELSE
                )
            ),
            TableRowMedGruppering(
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandlingstemaer = setOf(
                    Behandlingstema.YRKESAKTIV,
                    Behandlingstema.PENSJONIST,
                ),
                behandlingstyper = setOf(
                    Behandlingstyper.FØRSTEGANG,
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.HENVENDELSE
                )
            )
        )

    }

    private data class TableRowMedGruppering(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstyper: Set<Behandlingstyper>,
        val behandlingstemaer: Set<Behandlingstema>
    )

    enum class Regel(val beskrivelse: String) {
        MELOSYS("Melosys"),
        DVH("Datavarehus"),
        Begge("Begge")
    }

    data class TableRow(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstype: Behandlingstyper,
        val behandlingstema: Behandlingstema,
        val regel: Regel = Regel.Begge
    ) {
        fun match(
            sakstype: Sakstyper? = null,
            sakstema: Sakstemaer? = null,
            behandlingstype: Behandlingstyper? = null,
            behandlingstema: Behandlingstema? = null
        ): Boolean = (sakstype == null || sakstype == this.sakstype) &&
            (sakstema == null || sakstema == this.sakstema) &&
            (behandlingstype == null || behandlingstype == this.behandlingstype) &&
            (behandlingstema == null || behandlingstema == this.behandlingstema)
    }
}

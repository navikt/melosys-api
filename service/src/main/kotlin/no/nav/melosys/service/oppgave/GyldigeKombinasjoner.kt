import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class GyldigeKombinasjoner {

    companion object {
        fun listAlleKombinasjoner() {
            rows.forEach { row ->
                println("${row.sakstype}, ${row.sakstema}, ${row.behandlingstype}, ${row.behandlingstema}")
            }
        }

        fun finnGyldige(
            sakstype: Sakstyper? = null,
            sakstema: Sakstemaer? = null,
            behandlingstype: Behandlingstyper? = null,
            behandlingstema: Behandlingstema? = null
        ): List<TableRow> {
            return rows.filter {
                it.match(sakstype, sakstema, behandlingstype, behandlingstema)
            }
        }

        private val rows: List<TableRow> by lazy {
            sequence {
                tableRowMedGrupperings.forEach { row ->
                    row.behandlingstyper.forEach { behandlingstype ->
                        row.behandlingstemaer.forEach { behandlingstema ->
                            yield(
                                TableRow(
                                    row.sakstype,
                                    row.sakstema,
                                    behandlingstype,
                                    behandlingstema
                                )
                            )
                        }
                    }
                }
            }.toList()
        }
        private val tableRowMedGrupperings = listOf(
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
    }

    private data class TableRowMedGruppering(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstyper: Set<Behandlingstyper>,
        val behandlingstemaer: Set<Behandlingstema>
    )

    data class TableRow(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstype: Behandlingstyper,
        val behandlingstema: Behandlingstema
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

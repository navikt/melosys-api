package no.nav.melosys.migrering

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.Test

/**
 * Låser backfill-listene i V155 til ProsessType. prosess_type lagres med @Enumerated(STRING) =
 * enum-navnet (ikke kode), så en literal som bruker kode i stedet for navn (f.eks.
 * 'OPPRETT_NY_BEHANDLING_ARSAVREGNING' i stedet for navnet med dobbel A) ville treffe null rader.
 * Testen fanger den fellen og feil prioritet-gruppering, uten å starte database.
 */
class V155PrioritetBackfillTest {

    private val migrasjon: String =
        requireNotNull(javaClass.getResource("/db/migration/melosysDB/V155__prosessinstans_prioritet.sql")) {
            "Fant ikke V155-migrasjonen på classpath"
        }.readText()

    @Test
    fun `backfill-listene matcher ProsessType-prioritet eksakt`() {
        assertBlokk(ProsessPrioritet.HØY)
        assertBlokk(ProsessPrioritet.LAV)
    }

    /**
     * Låser begge retninger: hver SQL-literal må være et gyldig ProsessType-navn med riktig prioritet,
     * OG settet av literaler må være identisk med settet av ProsessType-er med den prioriteten. Sistnevnte
     * fanger at en ny HØY/LAV-type legges til i ProsessType uten å oppdatere backfillen (NORMAL backfilles
     * via kolonne-default, så den blokken finnes ikke i SQL-en).
     */
    private fun assertBlokk(forventet: ProsessPrioritet) {
        val literaler = prosessTypeLiteralerForBlokk(forventet).toSet()
        val forventedeTyper = ProsessType.values()
            .filter { it.prioritet == forventet }
            .map { it.name }
            .toSet()
        literaler shouldBe forventedeTyper
    }

    private fun prosessTypeLiteralerForBlokk(prioritet: ProsessPrioritet): List<String> {
        val blokk = Regex(
            "prioritet\\s*=\\s*'${Regex.escape(prioritet.name)}'\\s+WHERE\\s+prosess_type\\s+IN\\s*\\(([^)]*)\\)",
            RegexOption.DOT_MATCHES_ALL
        ).find(migrasjon)?.groupValues?.get(1)
            ?: error("Fant ikke UPDATE-blokk for prioritet ${prioritet.name} i V155")
        return Regex("'([^']+)'").findAll(blokk).map { it.groupValues[1] }.toList()
    }
}

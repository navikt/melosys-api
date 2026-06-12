package no.nav.melosys.migrering

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.Test

/**
 * Verifiserer backfill-listene i V156 uten å starte database. prosess_type lagres med
 * @Enumerated(STRING) = enum-navnet (ikke kode), så en literal som bruker kode i stedet for navn
 * (f.eks. 'OPPRETT_NY_BEHANDLING_ARSAVREGNING' med enkel A i stedet for navnet med dobbel A) ville
 * treffe null rader. Testen fanger den fellen.
 *
 * VIKTIG – frosset, ikke koblet til den levende enumen: V156 er en immutabel, allerede deploybar
 * migrasjon. Testen låser derfor mot et FROSSET øyeblikksbilde av hva V156 skulle backfille
 * ([forventetVedV156]), ikke mot dagens [ProsessType]. Skal du endre en prioritet senere, lag en NY
 * migrasjon (VNNN) med sin egen test – ikke rediger V156 (det ville brutt Flyway-checksum i miljøer
 * der V156 allerede er kjørt), og ikke juster det frosne settet under. En helt ny HØY/LAV-type
 * trenger ingen backfill: den har ingen eksisterende rader, og nye rader får riktig prioritet fra
 * entitetens type-default.
 */
class V156PrioritetBackfillTest {

    private val migrasjon: String =
        requireNotNull(javaClass.getResource("/db/migration/melosysDB/V156__prosessinstans_prioritet.sql")) {
            "Fant ikke V156-migrasjonen på classpath"
        }.readText()

    /**
     * Frosset øyeblikksbilde av hva V156 backfiller. NORMAL backfilles via kolonne-default, så den
     * blokken finnes ikke i SQL-en og er bevisst utelatt her.
     */
    private val forventetVedV156 = mapOf(
        ProsessPrioritet.HØY to setOf(
            "IVERKSETT_VEDTAK_EOS",
            "IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE",
            "IVERKSETT_VEDTAK_FTRL",
            "IVERKSETT_VEDTAK_IKKE_YRKESAKTIV",
            "IVERKSETT_VEDTAK_TRYGDEAVTALE",
            "JFR_ANDREGANG_NY_BEHANDLING",
            "JFR_ANDREGANG_REPLIKER_BEHANDLING",
            "JFR_KNYTT",
            "JFR_NY_SAK_BRUKER",
            "JFR_NY_SAK_VIRKSOMHET",
        ),
        ProsessPrioritet.LAV to setOf(
            "OPPRETT_NY_BEHANDLING_AARSAVREGNING",
            "SATSENDRING",
            "SATSENDRING_TILBAKESTILL_NY_VURDERING",
        ),
    )

    @Test
    fun `V156 backfiller nøyaktig det frosne settet per prioritet`() {
        forventetVedV156.forEach { (prioritet, forventet) ->
            prosessTypeLiteralerForBlokk(prioritet).toSet() shouldBe forventet
        }
    }

    @Test
    fun `det frosne settet refererer kun gyldige ProsessType-navn (fanger navn-vs-kode-fellen)`() {
        // Resolver navnet mot enumen; valueOf kaster for f.eks. enkel-A-kode-varianten som ikke er et navn.
        val gyldigeNavn = ProsessType.entries.map { it.name }.toSet()
        forventetVedV156.values.flatten().forEach { navn ->
            (navn in gyldigeNavn) shouldBe true
        }
    }

    private fun prosessTypeLiteralerForBlokk(prioritet: ProsessPrioritet): List<String> {
        val blokk = Regex(
            "prioritet\\s*=\\s*'${Regex.escape(prioritet.name)}'\\s+WHERE\\s+prosess_type\\s+IN\\s*\\(([^)]*)\\)",
            RegexOption.DOT_MATCHES_ALL
        ).find(migrasjon)?.groupValues?.get(1)
            ?: error("Fant ikke UPDATE-blokk for prioritet ${prioritet.name} i V156")
        return Regex("'([^']+)'").findAll(blokk).map { it.groupValues[1] }.toList()
    }
}

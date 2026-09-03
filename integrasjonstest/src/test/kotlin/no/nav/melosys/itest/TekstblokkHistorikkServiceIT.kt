package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkStatus
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import no.nav.melosys.service.tekstblokk.Endringstype
import no.nav.melosys.service.tekstblokk.TekstblokkHistorikkService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

import java.time.Instant

/**
 * Verifiserer at Envers-oppsettet (V166) faktisk skriver revisjoner for tekstblokker.
 * Hver lagring skjer i sin egen transaksjon, så den gir én revisjon hver. Rekkefølgen
 * i historikken følger revisjonsnummeret, ikke tidsstempelet – to lagringer på rad
 * lander gjerne på samme millisekund.
 */
class TekstblokkHistorikkServiceIT(
    @Autowired val tekstblokkHistorikkService: TekstblokkHistorikkService,
    @Autowired val tekstblokkRepository: TekstblokkRepository,
    @Autowired val jdbcTemplate: JdbcTemplate,
) : ComponentTestBase() {

    @Test
    fun `hver lagring gir en ny versjon i historikken`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Første utgave"))
        val id = requireNotNull(lagret.id)

        lagret.tittel = "Andre utgave"
        tekstblokkRepository.save(lagret)

        val historikk = tekstblokkHistorikkService.hentHistorikk(id)

        historikk shouldHaveSize 2
        historikk.map { it.versjon } shouldContainExactly listOf(1, 2)
        historikk.map { it.tittel } shouldContainExactly listOf("Første utgave", "Andre utgave")
        historikk.map { it.endringstype } shouldContainExactly listOf(Endringstype.OPPRETTET, Endringstype.ENDRET)
        historikk.last().gyldigTil.shouldBeNull()
    }

    @Test
    fun `versjon paa tidspunkt gir teksten slik den var da`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Første utgave"))
        val id = requireNotNull(lagret.id)
        val førEndring = Instant.now()

        // Oppslaget er tidsbasert (le på timestamp), så endringen må lande på et senere millisekund
        Thread.sleep(2)
        lagret.tittel = "Andre utgave"
        tekstblokkRepository.save(lagret)

        tekstblokkHistorikkService.hentVersjonPaaTidspunkt(id, førEndring)?.tittel shouldBe "Første utgave"
    }

    @Test
    fun `endring av kun sakstyper gir en ny versjon med den nye avgrensningen`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Avgrenset"))
        val id = requireNotNull(lagret.id)

        lagret.sakstyper.add(Sakstyper.TRYGDEAVTALE)
        tekstblokkRepository.save(lagret)

        val historikk = tekstblokkHistorikkService.hentHistorikk(id)

        historikk shouldHaveSize 2
        historikk.first().sakstyper shouldContainExactly listOf(Sakstyper.EU_EOS)
        historikk.last().sakstyper shouldContainExactlyInAnyOrder listOf(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE)
        historikk.last().tags shouldContainExactly listOf("historikk")
        historikk.last().behandlingstemaer shouldContainExactly listOf(Behandlingstema.ARBEID_FLERE_LAND)
        historikk.last().sakstemaer shouldContainExactly listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
    }

    @Test
    fun `endring av kun sakstemaer gir en ny versjon med den nye avgrensningen`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Avgrenset paa sakstema"))
        val id = requireNotNull(lagret.id)

        lagret.sakstemaer.add(Sakstemaer.TRYGDEAVGIFT)
        tekstblokkRepository.save(lagret)

        val historikk = tekstblokkHistorikkService.hentHistorikk(id)

        historikk shouldHaveSize 2
        historikk.first().sakstemaer shouldContainExactly listOf(Sakstemaer.MEDLEMSKAP_LOVVALG)
        historikk.last().sakstemaer shouldContainExactlyInAnyOrder
            listOf(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstemaer.TRYGDEAVGIFT)
    }

    @Test
    fun `blokk uten avgrensning gir tomme lister i historikken`() {
        val lagret = tekstblokkRepository.save(
            nyTekstblokk("Uten avgrensning").apply {
                sakstyper.clear()
                sakstemaer.clear()
                behandlingstemaer.clear()
                tags.clear()
            },
        )

        val versjon = tekstblokkHistorikkService.hentHistorikk(requireNotNull(lagret.id)).single()

        versjon.sakstyper.shouldBeEmpty()
        versjon.sakstemaer.shouldBeEmpty()
        versjon.behandlingstemaer.shouldBeEmpty()
        versjon.tags.shouldBeEmpty()
    }

    // Aud-rader skrevet før V167 har status null; blokkene var publiserte
    @Test
    fun `revisjon uten status leses som publisert`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Uten status").apply { status = TekstblokkStatus.UTKAST })
        val id = requireNotNull(lagret.id)
        jdbcTemplate.update("update tekstblokk_aud set status = null where id = ?", id)

        tekstblokkHistorikkService.hentHistorikk(id).single().status shouldBe TekstblokkStatus.PUBLISERT
    }

    @Test
    fun `historikken tar med status per versjon`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("Utkast").apply { status = TekstblokkStatus.UTKAST })
        val id = requireNotNull(lagret.id)

        lagret.status = TekstblokkStatus.PUBLISERT
        tekstblokkRepository.save(lagret)

        tekstblokkHistorikkService.hentHistorikk(id).map { it.status } shouldContainExactly
            listOf(TekstblokkStatus.UTKAST, TekstblokkStatus.PUBLISERT)
    }

    /**
     * Reproduserer historikken fra produksjon: revisjonsnummeret følger ikke tiden, og da
     * havnet «Opprettet» midt i lista mens tidsrommene løp bakover (v2: 10:09 – 09:51).
     *
     * Tilstanden lages ved å skrive om revtstmp, slik at revisjonsrekkefølgen og den
     * kronologiske rekkefølgen er uenige. Det er nøyaktig den uenigheten historikken må
     * tåle. Testen sier ikke noe om *hvorfor* numrene kommer ut av rekkefølge i drift –
     * bare at lesningen skal følge tiden når de gjør det.
     */
    @Test
    fun `historikken folger tiden selv naar revisjonsnummeret sier noe annet`() {
        val lagret = tekstblokkRepository.save(nyTekstblokk("A"))
        val id = requireNotNull(lagret.id)
        lagret.tittel = "B"
        tekstblokkRepository.save(lagret)
        lagret.tittel = "C"
        tekstblokkRepository.save(lagret)

        val revisjoner: List<Long> = jdbcTemplate.queryForList(
            "select rev from tekstblokk_aud where id = ? order by rev",
            Long::class.javaObjectType,
            id,
        ).filterNotNull()
        revisjoner shouldHaveSize 3

        // C ble skrevet sist, men får det tidligste tidsstempelet; A er opprettelsen og
        // ligger nå midt i tidslinja. Rev-rekkefølgen er A, B, C – tida sier C, A, B.
        val basis = Instant.now().minusSeconds(3600).toEpochMilli()
        settRevisjonstidspunkt(revisjoner[0], basis + 60_000)
        settRevisjonstidspunkt(revisjoner[1], basis + 120_000)
        settRevisjonstidspunkt(revisjoner[2], basis)

        val historikk = tekstblokkHistorikkService.hentHistorikk(id)

        historikk.map { it.tittel } shouldContainExactly listOf("C", "A", "B")
        historikk.map { it.versjon } shouldContainExactly listOf(1, 2, 3)
        // Tidsrommene henger sammen og løper framover – ingen «10:09 – 09:51»
        historikk[0].gyldigTil shouldBe historikk[1].gyldigFra
        historikk[1].gyldigTil shouldBe historikk[2].gyldigFra
        historikk.last().gyldigTil.shouldBeNull()
        historikk.map { it.gyldigFra } shouldBe historikk.map { it.gyldigFra }.sorted()
    }

    private fun settRevisjonstidspunkt(rev: Long, epochMilli: Long) {
        jdbcTemplate.update("update revinfo set revtstmp = ? where rev = ?", epochMilli, rev)
    }

    private fun nyTekstblokk(tittel: String) = Tekstblokk(
        tittel = tittel,
        innhold = "<p>$tittel</p>",
        type = TekstblokkType.TEKSTBLOKK,
        tags = mutableSetOf("historikk"),
        sakstyper = mutableSetOf(Sakstyper.EU_EOS),
        sakstemaer = mutableSetOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
        behandlingstemaer = mutableSetOf(Behandlingstema.ARBEID_FLERE_LAND),
    ).apply {
        registrertDato = Instant.now()
        registrertAv = "Z999999"
        endretDato = Instant.now()
        endretAv = "Z999999"
    }
}

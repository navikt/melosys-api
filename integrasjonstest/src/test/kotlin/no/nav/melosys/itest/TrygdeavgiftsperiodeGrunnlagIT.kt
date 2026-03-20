package no.nav.melosys.itest

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.ReplikerBehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Import(
    value = [
        ReplikerBehandlingsresultatService::class,
        BehandlingsresultatService::class,
        SaksbehandlingRegler::class,
        FakeUnleash::class,
        VilkaarsresultatService::class,
    ]
)
class TrygdeavgiftsperiodeGrunnlagIT(
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val replikerBehandlingsresultatService: ReplikerBehandlingsresultatService,
) : DataJpaTestBase() {

    @Test
    fun `trygdeavgiftsperiode med grunnlagListe persisteres og lastes korrekt`() {
        val (original, _) = lagFagsakMedBehandlinger()
        val br = lagBehandlingsresultatMedTrygdeavgift(original, antallGrunnlag = 1)
        behandlingsresultatRepository.save(br)

        val lastet = behandlingsresultatRepository.findById(original.id).get()
        val tap = lastet.trygdeavgiftsperioder.single()

        tap.grunnlagListe shouldHaveSize 1
        tap.beregningstype shouldBe Avgiftsberegningstype.ORDINAER
        tap.trygdesats shouldBe BigDecimal("6.80")

        val grunnlag = tap.grunnlagListe.first()
        grunnlag.inntektsperiode.shouldNotBeNull()
        grunnlag.skatteforhold.shouldNotBeNull()
        grunnlag.medlemskapsperiode.shouldNotBeNull()
    }

    @Test
    fun `trygdeavgiftsperiode med flere grunnlag og nullable sats persisteres korrekt`() {
        val (original, _) = lagFagsakMedBehandlinger()
        val br = lagBehandlingsresultatMedTrygdeavgift(original, antallGrunnlag = 3, begrenset = true)
        behandlingsresultatRepository.save(br)

        val lastet = behandlingsresultatRepository.findById(original.id).get()
        val tap = lastet.trygdeavgiftsperioder.single()

        tap.grunnlagListe shouldHaveSize 3
        tap.beregningstype shouldBe Avgiftsberegningstype.TJUEFEM_PROSENT_REGEL
        tap.trygdesats.shouldBeNull()
        tap.harAvgift() shouldBe true // beløp > 0 selv om sats er null
    }

    @Test
    fun `clearTrygdeavgiftsperioder sletter også grunnlag-rader via cascade`() {
        val (original, _) = lagFagsakMedBehandlinger()
        val br = lagBehandlingsresultatMedTrygdeavgift(original, antallGrunnlag = 2)
        behandlingsresultatRepository.save(br)

        // Verifiser at data finnes
        val lastet = behandlingsresultatRepository.findById(original.id).get()
        lastet.trygdeavgiftsperioder.single().grunnlagListe shouldHaveSize 2

        // Fjern trygdeavgiftsperioder via cascade
        lastet.finnAvgiftspliktigPerioder().forEach { it.clearTrygdeavgiftsperioder() }
        behandlingsresultatRepository.save(lastet)

        // Verifiser at alt er slettet
        val etterSletting = behandlingsresultatRepository.findById(original.id).get()
        etterSletting.trygdeavgiftsperioder shouldHaveSize 0
    }

    @Test
    fun `replikerBehandlingsresultat kopierer grunnlagListe korrekt`() {
        val (original, replika) = lagFagsakMedBehandlinger()
        val br = lagBehandlingsresultatMedTrygdeavgift(original, antallGrunnlag = 2, begrenset = true)
        behandlingsresultatRepository.save(br)

        replikerBehandlingsresultatService.replikerBehandlingsresultat(original, replika)

        val replikaResultat = behandlingsresultatRepository.findById(replika.id).get()
        val tap = replikaResultat.trygdeavgiftsperioder.single()

        tap.grunnlagListe shouldHaveSize 2
        tap.beregningstype shouldBe Avgiftsberegningstype.TJUEFEM_PROSENT_REGEL
        tap.trygdesats.shouldBeNull()

        // Verifiser at grunnlag er deep-copier (nye IDer)
        tap.grunnlagListe.forEach { grunnlag ->
            grunnlag.id.shouldNotBeNull()
            grunnlag.inntektsperiode.shouldNotBeNull()
            grunnlag.skatteforhold.shouldNotBeNull()
        }
    }

    // --- Hjelpemetoder ---

    private data class Behandlinger(val original: Behandling, val replika: Behandling)

    private fun lagFagsakMedBehandlinger(): Behandlinger {
        val fagsak = Fagsak("MEL-grunnlag-test", null, Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.LOVVALG_AVKLART)
            .apply { leggTilRegisteringInfo() }
        fagsakRepository.save(fagsak)

        val original = Behandling.forTest {
            this.fagsak = fagsak
            behandlingsfrist = LocalDate.now().plusYears(1)
            status = Behandlingsstatus.AVSLUTTET
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.YRKESAKTIV
        }.also { behandlingRepository.save(it) }

        val replika = Behandling.forTest {
            this.fagsak = fagsak
            behandlingsfrist = LocalDate.now().plusYears(1)
            status = Behandlingsstatus.OPPRETTET
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.YRKESAKTIV
            opprinneligBehandling = original
        }.also { behandlingRepository.save(it) }

        return Behandlinger(original, replika)
    }

    private fun lagBehandlingsresultatMedTrygdeavgift(
        behandling: Behandling,
        antallGrunnlag: Int = 1,
        begrenset: Boolean = false
    ): Behandlingsresultat {
        val br = Behandlingsresultat().apply {
            this.behandling = behandling
            behandlingsmåte = Behandlingsmaate.MANUELT
            leggTilRegisteringInfo()
        }

        val medlemskapsperiode = Medlemskapsperiode().apply {
            behandlingsresultat = br
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            fom = LocalDate.of(2025, 1, 1)
            tom = LocalDate.of(2025, 12, 31)
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        }
        br.addMedlemskapsperiode(medlemskapsperiode)

        val tap = Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2025, 1, 1),
            periodeTil = LocalDate.of(2025, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal("1500.00")),
            trygdesats = if (begrenset) null else BigDecimal("6.80"),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagInntekstperiode = lagInntektsperiode(1),
            grunnlagSkatteforholdTilNorge = lagSkatteforhold(),
            beregningstype = if (begrenset) Avgiftsberegningstype.TJUEFEM_PROSENT_REGEL else Avgiftsberegningstype.ORDINAER,
        )

        // Legg til N grunnlag i grunnlagListe
        for (i in 1..antallGrunnlag) {
            val grunnlag = TrygdeavgiftsperiodeGrunnlag(
                trygdeavgiftsperiode = tap,
                medlemskapsperiode = medlemskapsperiode,
                inntektsperiode = if (i == 1) tap.grunnlagInntekstperiode!! else lagInntektsperiode(i),
                skatteforhold = if (i == 1) tap.grunnlagSkatteforholdTilNorge!! else lagSkatteforhold(),
            )
            tap.leggTilGrunnlag(grunnlag)
        }

        medlemskapsperiode.trygdeavgiftsperioder.add(tap)

        return br
    }

    private fun lagInntektsperiode(nr: Int) = Inntektsperiode().apply {
        fomDato = LocalDate.of(2025, 1, 1)
        tomDato = LocalDate.of(2025, 12, 31)
        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
        avgiftspliktigMndInntekt = Penger(BigDecimal("${15000 + nr * 5000}.00"))
        isArbeidsgiversavgiftBetalesTilSkatt = false
    }

    private fun lagSkatteforhold() = SkatteforholdTilNorge().apply {
        fomDato = LocalDate.of(2025, 1, 1)
        tomDato = LocalDate.of(2025, 12, 31)
        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "test"
    }
}

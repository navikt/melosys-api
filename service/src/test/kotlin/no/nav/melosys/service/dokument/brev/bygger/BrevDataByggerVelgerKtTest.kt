package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.utpeking.UtpekingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BrevDataByggerVelgerKtTest {

    private lateinit var brevDataByggerVelger: BrevDataByggerVelger

    @BeforeEach
    fun setUp() {
        val anmodningsperiodeService = mockk<AnmodningsperiodeService>()
        val avklartefaktaService = mockk<AvklartefaktaService>()
        val landvelgerService = mockk<LandvelgerService>()
        val lovvalgsperiodeService = mockk<LovvalgsperiodeService>()
        val saksopplysningerService = mockk<SaksopplysningerService>()
        val utenlandskMyndighetService = mockk<UtenlandskMyndighetService>()
        val utpekingService = mockk<UtpekingService>()
        val vilkaarsresultatService = mockk<VilkaarsresultatService>()
        val persondataFasade = mockk<PersondataFasade>()
        val mottatteOpplysningerService = mockk<MottatteOpplysningerService>()

        brevDataByggerVelger = BrevDataByggerVelger(
            anmodningsperiodeService, avklartefaktaService,
            landvelgerService, lovvalgsperiodeService, saksopplysningerService, utenlandskMyndighetService,
            utpekingService, vilkaarsresultatService, persondataFasade, mottatteOpplysningerService
        )
    }

    @Test
    fun hent_medAttestA1_girVedleggBygger() {
        testHent(Produserbaredokumenter.ATTEST_A1, BrevDataByggerVedlegg::class.java)
    }

    @Test
    fun hent_medSEDA001_girVedleggBygger() {
        testHent(Produserbaredokumenter.ANMODNING_UNNTAK, BrevDataByggerVedlegg::class.java)
    }

    @Test
    fun hent_InnvilgelsesYrksaktiv_girInnvilgelseBygger() {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, BrevDataByggerInnvilgelse::class.java)
    }

    @Test
    fun hent_medDokumentTypeINNVILGELSE_YRKESAKTIV_FLERE_LAND_girBrevDataByggerInnvilgelseFlereLand() {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV_FLERE_LAND, BrevDataByggerInnvilgelseFlereLand::class.java)
    }

    @Test
    fun hent_InnvilgelsesArbeidsgiver_girInnvilgelseBygger() {
        testHent(Produserbaredokumenter.INNVILGELSE_ARBEIDSGIVER, BrevDataByggerInnvilgelse::class.java)
    }

    @Test
    fun hent_Avslag_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.AVSLAG_YRKESAKTIV, BrevDataByggerAvslagYrkesaktiv::class.java)
    }

    @Test
    fun hent_medDokumentTypeAnmodningUnntak_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, BrevDataByggerAnmodningUnntak::class.java)
    }

    private fun testHent(type: Produserbaredokumenter, forventetKlasse: Class<out BrevDataBygger>) {
        val resultat = brevDataByggerVelger.hent(type, BrevbestillingDto())
        resultat::class.java shouldBe forventetKlasse
    }
}

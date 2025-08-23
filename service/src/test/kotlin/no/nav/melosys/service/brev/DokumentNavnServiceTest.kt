package no.nav.melosys.service.brev

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Mottakerroller.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
class DokumentNavnServiceTest {

    @MockK
    private lateinit var brevmottakerService: BrevmottakerService

    @MockK
    private lateinit var dokgenService: DokgenService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    private val dokumentproduksjonsInfoMapper = DokumentproduksjonsInfoMapper()

    private lateinit var dokumentNavnService: DokumentNavnService

    @BeforeEach
    fun setUp() {
        dokumentNavnService = DokumentNavnService(brevmottakerService, dokgenService, lovvalgsperiodeService, medlemskapsperiodeService)
    }

    @Test
    fun `utledDokumentNavnForProduserbartdokumentOgMottakerrolle ikke Storbritannia skal returnere forventet tittel`() {
        val behandling = lagBehandling()
        every { dokgenService.hentDokumentInfo(INNVILGELSE_FOLKETRYGDLOVEN) } returns dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(
            INNVILGELSE_FOLKETRYGDLOVEN
        )


        val dokumentNavn =
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(behandling, INNVILGELSE_FOLKETRYGDLOVEN, BRUKER)


        dokumentNavn shouldBe INNVILGELSE_FOLKETRYGDLOVEN.beskrivelse
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    fun `utledDokumentNavnForProduserbartdokumentOgMottakerrolle Storbritannia innvilgelse og attest med ulike parametre skal returnere korrekt tittel`(
        skalHaAttest: Boolean,
        erNyVurdering: Boolean,
        mottaker: Mottaker,
        forventetTittel: String
    ) {
        val behandling = lagBehandling()
        behandling.type = if (erNyVurdering) NY_VURDERING else FØRSTEGANG

        if (!mottaker.erUtenlandskMyndighet()) {
            every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lagLovvalsperiode(if (skalHaAttest) UK_ART6_1 else UK_ART8_2)
        }
        every { brevmottakerService.avklarMottaker(TRYGDEAVTALE_GB, Mottaker.medRolle(mottaker.rolle!!), behandling) } returns mottaker
        every { dokgenService.hentDokumentInfo(TRYGDEAVTALE_GB) } returns dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB)


        val dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(behandling, TRYGDEAVTALE_GB, mottaker.rolle!!)


        dokumentNavn shouldBe forventetTittel
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    fun `utledDokumentNavnForProduserbartdokumentOgMottaker Storbritannia innvilgelse og attest med ulike parametre skal returnere korrekt tittel`(
        skalHaAttest: Boolean,
        erNyVurdering: Boolean,
        mottaker: Mottaker,
        forventetTittel: String
    ) {
        val behandling = lagBehandling()
        behandling.type = if (erNyVurdering) NY_VURDERING else FØRSTEGANG

        if (!mottaker.erUtenlandskMyndighet()) {
            every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lagLovvalsperiode(if (skalHaAttest) UK_ART6_1 else UK_ART8_2)
        }
        every { dokgenService.hentDokumentInfo(TRYGDEAVTALE_GB) } returns dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB)


        val dokumentNavn = dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(behandling, TRYGDEAVTALE_GB, mottaker, "")


        dokumentNavn shouldBe forventetTittel
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    fun `utledDokumentNavn Storbritannia innvilgelse og attest med ulike parametre skal returnere korrekt tittel`(
        skalHaAttest: Boolean,
        erNyVurdering: Boolean,
        mottaker: Mottaker,
        forventetTittel: String
    ) {
        val behandling = lagBehandling()
        behandling.type = if (erNyVurdering) NY_VURDERING else FØRSTEGANG

        if (!mottaker.erUtenlandskMyndighet()) {
            every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns lagLovvalsperiode(if (skalHaAttest) UK_ART6_1 else UK_ART8_2)
        }


        val dokumentNavn = dokumentNavnService.utledTittel(
            behandling = behandling,
            produserbartdokument = TRYGDEAVTALE_GB,
            dokumentproduksjonsInfo = dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(TRYGDEAVTALE_GB),
            mottaker = mottaker,
            mottakerRolle = null,
            brevbestilling = null,
            standardTekst = null,
        )


        dokumentNavn shouldBe forventetTittel
    }

    private fun lagBehandling(): Behandling = Behandling.forTest {
        id = 1L
    }

    private fun lagLovvalsperiode(bestemmelse: LovvalgBestemmelse) = Lovvalgsperiode().apply {
        this.bestemmelse = bestemmelse
    }

    fun testparametre() =
        listOf(
            Arguments.of(true, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for medlemskap i folketrygden"),
            Arguments.of(false, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap"),
            Arguments.of(
                true,
                false,
                lagMottaker(ARBEIDSGIVER, null, "1234", null),
                "Kopi av vedtak om medlemskap, Attest for medlemskap i folketrygden"
            ),
            Arguments.of(false, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "1234"), "Attest for medlemskap i folketrygden"),

            Arguments.of(
                true,
                true,
                lagMottaker(BRUKER, "1234", null, null),
                "Vedtak om medlemskap, Attest for medlemskap i folketrygden - endring"
            ),
            Arguments.of(false, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap - endring"),
            Arguments.of(
                true,
                true,
                lagMottaker(ARBEIDSGIVER, null, "1234", null),
                "Kopi av vedtak om medlemskap, Attest for medlemskap i folketrygden - endring"
            ),
            Arguments.of(false, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap - endring"),
            Arguments.of(
                true,
                true,
                lagMottaker(UTENLANDSK_TRYGDEMYNDIGHET, null, null, "1234"),
                "Attest for medlemskap i folketrygden - endring"
            )
        )

    private fun lagMottaker(rolle: Mottakerroller, aktørID: String?, orgnr: String?, institusjonsID: String?): Mottaker =
        Mottaker.medRolle(rolle).apply {
            this.aktørId = aktørID
            this.orgnr = orgnr
            this.institusjonID = institusjonsID
        }
}

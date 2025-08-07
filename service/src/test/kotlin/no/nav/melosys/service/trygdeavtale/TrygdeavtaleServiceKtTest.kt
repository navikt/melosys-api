package no.nav.melosys.service.trygdeavtale

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly as shouldContainExactlyMap
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper.PLIKTIG
import no.nav.melosys.domain.kodeverk.Trygdedekninger.FULL_DEKNING_FTRL
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Tilleggsbestemmelser_trygdeavtale_ca.CAN_ART8
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.*
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.Relasjonsrolle.BARN
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.tilMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import java.time.LocalDate
import java.util.*

class TrygdeavtaleServiceKtTest {

    private val eregFasade = mockk<EregFasade>()
    private val avklarteMedfolgendeFamilieService = mockk<AvklarteMedfolgendeFamilieService>()
    private val avklarteVirksomheterService = mockk<AvklarteVirksomheterService>()
    private val lovvalgsperiodeService = mockk<LovvalgsperiodeService>()
    private val avklartefaktaService = mockk<AvklartefaktaService>()

    private lateinit var trygdeavtaleService: TrygdeavtaleService

    private val avklarteMedfolgendeFamilieSlot = slot<AvklarteMedfolgendeFamilie>()
    private val lovvalgsperioderSlot = slot<Collection<Lovvalgsperiode>>()

    @BeforeEach
    fun init() {
        trygdeavtaleService = TrygdeavtaleService(
            eregFasade,
            avklarteMedfolgendeFamilieService,
            avklarteVirksomheterService,
            lovvalgsperiodeService,
            avklartefaktaService
        )
    }

    @Test
    fun `overførResultat - alt ok - lagres korrekt`() {
        // Arrange
        val trygdeavtaleResultat = lagTrygdeavtaleAltFyltUtResultat()
        every { lovvalgsperiodeService.hentLovvalgsperioder(any<Long>()) } returns emptyList()

        // Act
        trygdeavtaleService.overførResultat(1L, trygdeavtaleResultat)

        // Assert
        verify { avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(eq(1L), capture(avklarteMedfolgendeFamilieSlot)) }
        verify { avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, listOf(ORGNR_1)) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(1L), capture(lovvalgsperioderSlot)) }

        val capturedFamilie = avklarteMedfolgendeFamilieSlot.captured
        capturedFamilie.shouldNotBeNull()
        
        val ikkeOmfattetData = capturedFamilie.familieIkkeOmfattetAvNorskTrygd.flatMap { 
            listOf(it.uuid, it.begrunnelse, it.begrunnelseFritekst) 
        }
        ikkeOmfattetData.shouldContainExactlyInAnyOrder(
            UUID_BARN_1, OVER_18_AR.kode, BEGRUNNELSE_BARN,
            UUID_EKTEFELLE, EGEN_INNTEKT.kode, BEGRUNNELSE_SAMBOER
        )

        capturedFamilie.familieOmfattetAvNorskTrygd.shouldHaveSize(1)
        capturedFamilie.familieOmfattetAvNorskTrygd.map { it.uuid }.shouldContainExactly(UUID_BARN_2)

        val capturedLovvalgsperioder = lovvalgsperioderSlot.captured.toList()
        capturedLovvalgsperioder.shouldHaveSize(1)
        
        val periode = capturedLovvalgsperioder.first()
        periode.id shouldBe null
        periode.fom shouldBe trygdeavtaleResultat.lovvalgsperiodeFom()
        periode.tom shouldBe trygdeavtaleResultat.lovvalgsperiodeTom()
        periode.medlemskapstype shouldBe PLIKTIG
        periode.dekning shouldBe FULL_DEKNING_FTRL
        periode.innvilgelsesresultat shouldBe INNVILGET
        periode.bestemmelse shouldBe LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(trygdeavtaleResultat.bestemmelse())
        periode.lovvalgsland shouldBe Land_iso2.NO
        periode.medlPeriodeID shouldBe null
    }

    @Test
    fun `overførResultat - tilleggsbestemmelse - lagres korrekt`() {
        // Arrange
        val trygdeavtaleResultat = lagTrygdeavtaleMedTilleggsbestemmelse()
        every { lovvalgsperiodeService.hentLovvalgsperioder(any<Long>()) } returns emptyList()

        // Act
        trygdeavtaleService.overførResultat(1L, trygdeavtaleResultat)

        // Assert
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(1L), capture(lovvalgsperioderSlot)) }

        val capturedLovvalgsperioder = lovvalgsperioderSlot.captured.toList()
        capturedLovvalgsperioder.shouldHaveSize(1)
        
        val periode = capturedLovvalgsperioder.first()
        periode.id shouldBe null
        periode.fom shouldBe trygdeavtaleResultat.lovvalgsperiodeFom()
        periode.tom shouldBe trygdeavtaleResultat.lovvalgsperiodeTom()
        periode.medlemskapstype shouldBe PLIKTIG
        periode.dekning shouldBe FULL_DEKNING_FTRL
        periode.innvilgelsesresultat shouldBe INNVILGET
        periode.bestemmelse shouldBe LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(trygdeavtaleResultat.bestemmelse())
        periode.tilleggsbestemmelse shouldBe LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(trygdeavtaleResultat.tilleggsbestemmelse())
        periode.lovvalgsland shouldBe Land_iso2.NO
        periode.medlPeriodeID shouldBe null
    }

    @Test
    fun `overførResultat - lovvalgperiode finnes grunnet ny vurdering - lagres korrekt`() {
        // Arrange
        val trygdeavtaleResultat = lagTrygdeavtaleAltFyltUtResultat()
        every { lovvalgsperiodeService.hentLovvalgsperioder(any<Long>()) } returns listOf(lagLovvalgsperiode())

        // Act
        trygdeavtaleService.overførResultat(1L, trygdeavtaleResultat)

        // Assert
        verify { avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(eq(1L), capture(avklarteMedfolgendeFamilieSlot)) }
        verify { avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, listOf(ORGNR_1)) }
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(1L), capture(lovvalgsperioderSlot)) }

        val capturedFamilie = avklarteMedfolgendeFamilieSlot.captured
        val ikkeOmfattetData = capturedFamilie.familieIkkeOmfattetAvNorskTrygd.flatMap { 
            listOf(it.uuid, it.begrunnelse, it.begrunnelseFritekst) 
        }
        ikkeOmfattetData.shouldContainExactlyInAnyOrder(
            UUID_BARN_1, OVER_18_AR.kode, BEGRUNNELSE_BARN,
            UUID_EKTEFELLE, EGEN_INNTEKT.kode, BEGRUNNELSE_SAMBOER
        )
        capturedFamilie.familieOmfattetAvNorskTrygd.map { it.uuid }.shouldContainExactly(UUID_BARN_2)

        val capturedLovvalgsperioder = lovvalgsperioderSlot.captured.toList()
        capturedLovvalgsperioder.shouldHaveSize(1)
        
        val periode = capturedLovvalgsperioder.first()
        periode.id shouldBe 11L
        periode.fom shouldBe trygdeavtaleResultat.lovvalgsperiodeFom()
        periode.tom shouldBe trygdeavtaleResultat.lovvalgsperiodeTom()
        periode.medlemskapstype shouldBe PLIKTIG
        periode.dekning shouldBe FULL_DEKNING_FTRL
        periode.innvilgelsesresultat shouldBe INNVILGET
        periode.bestemmelse shouldBe LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(trygdeavtaleResultat.bestemmelse())
        periode.lovvalgsland shouldBe Land_iso2.NO
        periode.medlPeriodeID shouldBe 111L
    }

    @Test
    fun `hentResultat - all data - hentes korrekt`() {
        // Arrange
        every { avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any<Long>()) } returns lagAvklartMedfølgendeEktefelle()
        every { avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any<Long>()) } returns lagAvklartMedfølgendeBarn()
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any<Long>()) } returns setOf(ORGNR_1)
        every { lovvalgsperiodeService.hentLovvalgsperioder(any<Long>()) } returns listOf(lagLovvalgsperiode())

        // Act
        val trygdeavtaleResultat = trygdeavtaleService.hentResultat(1L)

        // Assert
        val forventetResultat = lagTrygdeavtaleAltFyltUtResultat()
        trygdeavtaleResultat.familie().familieOmfattetAvNorskTrygd shouldBe forventetResultat.familie().familieOmfattetAvNorskTrygd
        trygdeavtaleResultat.familie().familieIkkeOmfattetAvNorskTrygd shouldBe forventetResultat.familie().familieIkkeOmfattetAvNorskTrygd
        trygdeavtaleResultat.virksomhet() shouldBe forventetResultat.virksomhet()
        trygdeavtaleResultat.bestemmelse() shouldBe forventetResultat.bestemmelse()
        trygdeavtaleResultat.lovvalgsperiodeFom() shouldBe forventetResultat.lovvalgsperiodeFom()
        trygdeavtaleResultat.lovvalgsperiodeTom() shouldBe forventetResultat.lovvalgsperiodeTom()
    }

    @Test
    fun `hentResultat - ingen data - hentes korrekt`() {
        // Arrange
        every { avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(any<Long>()) } returns AvklarteMedfolgendeFamilie(emptySet(), emptySet())
        every { avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(any<Long>()) } returns AvklarteMedfolgendeFamilie(emptySet(), emptySet())
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any<Long>()) } returns emptySet()
        every { lovvalgsperiodeService.hentLovvalgsperioder(any<Long>()) } returns emptyList()

        // Act
        val trygdeavtaleResultat = trygdeavtaleService.hentResultat(1L)

        // Assert
        val tomtTrygdeavtaleResultat = TrygdeavtaleResultat.Builder()
            .familie(AvklarteMedfolgendeFamilie(emptySet(), emptySet()))
            .build()
        
        trygdeavtaleResultat.familie().familieOmfattetAvNorskTrygd shouldBe tomtTrygdeavtaleResultat.familie().familieOmfattetAvNorskTrygd
        trygdeavtaleResultat.familie().familieIkkeOmfattetAvNorskTrygd shouldBe tomtTrygdeavtaleResultat.familie().familieIkkeOmfattetAvNorskTrygd
    }

    @Test
    fun `hentVirksomheter - fra ereg - mappes korrekt`() {
        // Arrange
        val selvstendigForetak = SelvstendigForetak().apply {
            orgnr = ORGNR_1
        }
        val selvstendigArbeidObj = SelvstendigArbeid()
        selvstendigArbeidObj.selvstendigForetak = listOf(selvstendigForetak)

        val juridiskArbeidsgiverNorge = JuridiskArbeidsgiverNorge().apply {
            ekstraArbeidsgivere = listOf(ORGNR_2)
        }

        val behandling = lagBehandlingMedVirksomheter(
            selvstendigArbeidObj,
            juridiskArbeidsgiverNorge,
            emptyList(),
            emptySet()
        )

        every { eregFasade.hentOrganisasjonNavn(ORGNR_1) } returns NAVN_1
        every { eregFasade.hentOrganisasjonNavn(ORGNR_2) } returns NAVN_2

        // Act
        val response = trygdeavtaleService.hentVirksomheter(behandling)

        // Assert
        response.shouldContainExactlyMap(mapOf(ORGNR_1 to NAVN_1, ORGNR_2 to NAVN_2))
    }

    @Test
    fun `hentVirksomheter - fra behandling saksopplysning - mappes korrekt`() {
        // Arrange
        val behandling = lagBehandlingMedVirksomheter(
            SelvstendigArbeid(),
            JuridiskArbeidsgiverNorge(),
            emptyList(),
            setOf(
                lagArbForhSaksopplysning(listOf(ORGNR_1, ORGNR_2)),
                lagOrgSaksopplysning(ORGNR_1, NAVN_1),
                lagOrgSaksopplysning(ORGNR_2, NAVN_2)
            )
        )

        // Act
        val response = trygdeavtaleService.hentVirksomheter(behandling)

        // Assert
        response.shouldContainExactlyMap(mapOf(ORGNR_1 to NAVN_1, ORGNR_2 to NAVN_2))
        response.shouldNotContainKey("OpplysningspliktigID")
    }

    @Test
    fun `hentVirksomheter - fra mottatte opplysninger foretak utland - mappes korrekt`() {
        // Arrange
        val behandling = lagBehandlingMedVirksomheter(
            SelvstendigArbeid(),
            JuridiskArbeidsgiverNorge(),
            lagForetakUtland(mapOf(ORGNR_1 to NAVN_1, ORGNR_2 to NAVN_2)),
            emptySet()
        )

        // Act
        val response = trygdeavtaleService.hentVirksomheter(behandling)

        // Assert
        response.shouldContainExactlyMap(mapOf(ORGNR_1 to NAVN_1, ORGNR_2 to NAVN_2))
    }

    @Test
    fun `hentVirksomheter - ingen virksomheter - tom map`() {
        // Arrange
        val behandling = lagBehandlingMedVirksomheter(
            SelvstendigArbeid(),
            JuridiskArbeidsgiverNorge(),
            emptyList(),
            emptySet()
        )

        // Act
        val response = trygdeavtaleService.hentVirksomheter(behandling)

        // Assert
        response shouldBe emptyMap()
    }

    @Test
    fun `hentFamiliemedlemmer - barn og ektefelle - fylt liste`() {
        // Arrange
        val behandling = lagBehandlingMedFamilie(
            listOf(
                tilMedfolgendeFamilie(UUID_BARN_1, "fnr1", "navn1", BARN),
                tilMedfolgendeFamilie(UUID_BARN_2, "fnr2", "navn2", BARN),
                tilMedfolgendeFamilie(UUID_EKTEFELLE, "fnr3", "navn3", EKTEFELLE_SAMBOER)
            )
        )

        // Act
        val response = trygdeavtaleService.hentFamiliemedlemmer(behandling)

        // Assert
        response.shouldHaveSize(3)
        
        val data = response.flatMap { listOf(it.uuid, it.fnr, it.navn, it.relasjonsrolle) }
        data.shouldContainExactlyInAnyOrder(
            UUID_BARN_1, "fnr1", "navn1", BARN,
            UUID_BARN_2, "fnr2", "navn2", BARN,
            UUID_EKTEFELLE, "fnr3", "navn3", EKTEFELLE_SAMBOER
        )
    }

    @Test
    fun `hentFamiliemedlemmer - ingen familie - tom liste`() {
        // Arrange
        val behandling = lagBehandlingMedFamilie(emptyList())

        // Act
        val response = trygdeavtaleService.hentFamiliemedlemmer(behandling)

        // Assert
        response shouldBe emptyList()
    }

    companion object {
        private const val ORGNR_1 = "11111111111"
        private const val ORGNR_2 = "22222222222"
        private const val NAVN_1 = "Navn 1"
        private const val NAVN_2 = "Navn 2"
        private val UUID_BARN_1 = UUID.randomUUID().toString()
        private val UUID_BARN_2 = UUID.randomUUID().toString()
        private val UUID_EKTEFELLE = UUID.randomUUID().toString()
        private const val BEGRUNNELSE_BARN = "begrunnelse barn"
        private const val BEGRUNNELSE_SAMBOER = "begrunnelse samboer"
        private val PERIODE_FOM: LocalDate = LocalDate.now()
        private val PERIODE_TOM: LocalDate = PERIODE_FOM.plusYears(1)

        private fun lagTrygdeavtaleMedTilleggsbestemmelse(): TrygdeavtaleResultat {
            return TrygdeavtaleResultat.Builder()
                .virksomhet(ORGNR_1)
                .bestemmelse(CAN_ART7.kode)
                .tilleggsbestemmelse(CAN_ART8.kode)
                .familie(
                    AvklarteMedfolgendeFamilie(
                        setOf(OmfattetFamilie(UUID_BARN_2)),
                        setOf(
                            IkkeOmfattetFamilie(UUID_BARN_1, OVER_18_AR.kode, BEGRUNNELSE_BARN),
                            IkkeOmfattetFamilie(UUID_EKTEFELLE, EGEN_INNTEKT.kode, BEGRUNNELSE_SAMBOER)
                        )
                    )
                )
                .lovvalgsperiodeFom(PERIODE_FOM)
                .lovvalgsperiodeTom(PERIODE_TOM)
                .build()
        }

        private fun lagTrygdeavtaleAltFyltUtResultat(): TrygdeavtaleResultat {
            return TrygdeavtaleResultat.Builder()
                .virksomhet(ORGNR_1)
                .bestemmelse(UK_ART6_1.kode)
                .familie(
                    AvklarteMedfolgendeFamilie(
                        setOf(OmfattetFamilie(UUID_BARN_2)),
                        setOf(
                            IkkeOmfattetFamilie(UUID_BARN_1, OVER_18_AR.kode, BEGRUNNELSE_BARN),
                            IkkeOmfattetFamilie(UUID_EKTEFELLE, EGEN_INNTEKT.kode, BEGRUNNELSE_SAMBOER)
                        )
                    )
                )
                .lovvalgsperiodeFom(PERIODE_FOM)
                .lovvalgsperiodeTom(PERIODE_TOM)
                .build()
        }

        private fun lagAvklartMedfølgendeEktefelle(): AvklarteMedfolgendeFamilie {
            return AvklarteMedfolgendeFamilie(
                emptySet(),
                setOf(IkkeOmfattetFamilie(UUID_EKTEFELLE, EGEN_INNTEKT.kode, BEGRUNNELSE_SAMBOER))
            )
        }

        private fun lagAvklartMedfølgendeBarn(): AvklarteMedfolgendeFamilie {
            return AvklarteMedfolgendeFamilie(
                setOf(OmfattetFamilie(UUID_BARN_2)),
                setOf(IkkeOmfattetFamilie(UUID_BARN_1, OVER_18_AR.kode, BEGRUNNELSE_BARN))
            )
        }

        private fun lagLovvalgsperiode(): Lovvalgsperiode {
            return Lovvalgsperiode().apply {
                id = 11L
                bestemmelse = UK_ART6_1
                fom = PERIODE_FOM
                tom = PERIODE_TOM
                medlPeriodeID = 111L
            }
        }

        private fun lagBehandling(): Behandling {
            return BehandlingTestFactory.builderWithDefaults()
                .medId(1L)
                .medFagsak(FagsakTestFactory.lagFagsak())
                .build()
        }

        private fun lagBehandlingMedFamilie(familie: List<MedfolgendeFamilie>): Behandling {
            val personOpplysninger = OpplysningerOmBrukeren()
            personOpplysninger.medfolgendeFamilie = familie

            val mottatteOpplysningerData = MottatteOpplysningerData()
            mottatteOpplysningerData.personOpplysninger = personOpplysninger

            val mottatteOpplysninger = MottatteOpplysninger().apply {
                setMottatteOpplysningerData(mottatteOpplysningerData)
            }

            return BehandlingTestFactory.builderWithDefaults()
                .medMottatteOpplysninger(mottatteOpplysninger)
                .build()
        }

        private fun lagBehandlingMedVirksomheter(
            selvstendigArbeid: SelvstendigArbeid,
            juridiskArbeidsgiverNorge: JuridiskArbeidsgiverNorge,
            foretakUtland: List<ForetakUtland>,
            saksopplysninger: Set<Saksopplysning>
        ): Behandling {
            val mottatteOpplysningerData = MottatteOpplysningerData().apply {
                this.selvstendigArbeid = selvstendigArbeid
                this.juridiskArbeidsgiverNorge = juridiskArbeidsgiverNorge
                this.foretakUtland = foretakUtland
            }

            val mottatteOpplysninger = MottatteOpplysninger().apply {
                setMottatteOpplysningerData(mottatteOpplysningerData)
            }

            return BehandlingTestFactory.builderWithDefaults()
                .medSaksopplysninger(saksopplysninger.toMutableSet())
                .medMottatteOpplysninger(mottatteOpplysninger)
                .build()
        }

        private fun lagForetakUtland(uuidNavn: Map<String, String>): List<ForetakUtland> {
            return uuidNavn.entries.map { (uuid, navn) ->
                ForetakUtland().apply {
                    this.uuid = uuid
                    this.navn = navn
                }
            }
        }

        private fun lagOrgSaksopplysning(orgnr: String, navn: String): Saksopplysning {
            return Saksopplysning().apply {
                id = orgnr.toLong()
                type = SaksopplysningType.ORG
                dokument = lagOrganisasjonsDokument(orgnr, navn)
            }
        }

        private fun lagOrganisasjonsDokument(orgnr: String, navn: String): OrganisasjonDokument {
            return OrganisasjonDokumentTestFactory.builder()
                .orgnummer(orgnr)
                .navn(navn)
                .build()
        }

        private fun lagArbForhSaksopplysning(orgnumre: List<String>): Saksopplysning {
            val arbeidsforholdDokument = ArbeidsforholdDokument().apply {
                arbeidsforhold = orgnumre.map { orgnr ->
                    Arbeidsforhold().apply {
                        arbeidsgivertype = Aktoertype.ORGANISASJON
                        arbeidsgiverID = orgnr
                        opplysningspliktigtype = Aktoertype.ORGANISASJON
                        opplysningspliktigID = "OpplysningspliktigID"
                    }
                }
            }

            return Saksopplysning().apply {
                type = SaksopplysningType.ARBFORH
                dokument = arbeidsforholdDokument
            }
        }
    }
}
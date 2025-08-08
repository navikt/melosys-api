package no.nav.melosys.service.dokument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Mottakerroller.ARBEIDSGIVER
import no.nav.melosys.domain.kodeverk.Mottakerroller.BRUKER
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS as Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.JuridiskArbeidsgiverNorge
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.doksys.DoksysFasade
import no.nav.melosys.integrasjon.doksys.Dokumentbestilling
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import no.nav.melosys.repository.AvklarteFaktaRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.UtenlandskMyndighetRepository
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.dokument.brev.*
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAvslagArbeidsgiver
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerInnvilgelse
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagFactory
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class DokumentServiceKtTest {
    
    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService
    
    @MockK
    private lateinit var dokSysFasade: DoksysFasade
    
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    
    private lateinit var dokumentService: DokumentService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        SpringSubjectHandler.set(TestSubjectHandler())
        
        // Setup DoksysFasade mocks
        every { dokSysFasade.produserIkkeredigerbartDokument(any()) } returns mockk()
        every { dokSysFasade.produserDokumentutkast(any()) } returns null
        
        dokumentService = lagDokumentService(null)
    }

    @Test
    fun `produser innvilgelsesbrev med fullmektig sender til bruker og fullmektig`() {
        val dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock())
        val brevbestilling = DoksysBrevbestilling.Builder().medProduserbartDokument(INNVILGELSE_YRKESAKTIV).build()
        
        dokumentServiceMedMockVelger.produserDokument(
            INNVILGELSE_YRKESAKTIV, 
            Mottaker.medRolle(BRUKER), 
            BEHANDLINGSID, 
            brevbestilling
        )
        
        verify(exactly = 2) { dokSysFasade.produserIkkeredigerbartDokument(any<Dokumentbestilling>()) }
    }

    @Test
    fun `produser avslag arbeidsgiver funker`() {
        val brevbestilling = lagBrevbestillingAvslagArbeidsgiver()
        val arbeidsgivendeOrgnumre = setOf("987654321")
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any<Behandling>()) } returns arbeidsgivendeOrgnumre
        val dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock())
        
        dokumentServiceMedMockVelger.produserDokument(
            AVSLAG_ARBEIDSGIVER, 
            Mottaker.medRolle(ARBEIDSGIVER), 
            BEHANDLINGSID, 
            brevbestilling
        )
        
        verify { dokSysFasade.produserIkkeredigerbartDokument(any<Dokumentbestilling>()) }
    }

    @Test
    fun `produser utkast innvilgelses brev funker`() {
        val brevbestilling = lagBrevBestillingDto(INNVILGELSE_YRKESAKTIV, BRUKER)
        val dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock(brevbestilling))
        
        val resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, brevbestilling)
        
        resultat shouldBe null
        verify { dokSysFasade.produserDokumentutkast(any<Dokumentbestilling>()) }
    }

    @Test
    fun `produser utkast avslag arbeidsgiver funker`() {
        val brevbestilling = lagBrevBestillingDto(AVSLAG_ARBEIDSGIVER, ARBEIDSGIVER)
        val arbeidsgivendeOrgnumre = setOf("987654321")
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any<Behandling>()) } returns arbeidsgivendeOrgnumre
        val dokumentServiceMedMockVelger = lagDokumentService(lagBrevdatabyggerVelgerMock(brevbestilling))
        
        val resultat = dokumentServiceMedMockVelger.produserUtkast(BEHANDLINGSID, brevbestilling)
        
        resultat shouldBe null
        verify { dokSysFasade.produserDokumentutkast(any<Dokumentbestilling>()) }
    }

    @Test
    fun `produser dokument uten behandling kaster unntak`() {
        val unntak = shouldThrow<IkkeFunnetException> {
            dokumentService.produserDokument(
                ATTEST_A1, 
                Mottaker.medRolle(ARBEIDSGIVER), 
                BEHANDLINGSID.inv(), 
                DoksysBrevbestilling.Builder().build()
            )
        }
        
        unntak.shouldBeInstanceOf<IkkeFunnetException>()
        unntak.message shouldContain "finnes ikke"
    }

    @Test
    fun `produser dokument uten dokumenttype kaster unntak`() {
        val unntak = shouldThrow<IllegalArgumentException> {
            dokumentService.produserDokument(
                null, 
                Mottaker.medRolle(ARBEIDSGIVER), 
                BEHANDLINGSID, 
                DoksysBrevbestilling.Builder().build()
            )
        }
        
        unntak.shouldBeInstanceOf<IllegalArgumentException>()
        unntak.message shouldContain "Ingen gyldig"
    }

    companion object {
        private const val BEHANDLINGSID = 13L
        private const val ORGNR = "123456789"
        
        private var idTeller = 1L

        private fun lagBrevBestillingDto(produserbartdokument: Produserbaredokumenter, rolle: Mottakerroller): BrevbestillingDto {
            return BrevbestillingDto().apply {
                produserbardokument = produserbartdokument
                mottaker = rolle
            }
        }

        private fun lagBrevDataInnvilgelse(): BrevData {
            val brevDataA1 = BrevDataA1().apply {
                hovedvirksomhet = AvklartVirksomhet("Virker av og til", "987654321", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
                bostedsadresse = lagStrukturertAdresse()
                yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL
                bivirksomheter = emptyList()
                person = lagPersonopplysninger()
                arbeidssteder = ArrayList()
                arbeidsland = ArrayList()
            }
            
            val arbeidsgiver = AvklartVirksomhet("Virker av og til", "987654321", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
            return BrevDataInnvilgelse(BrevbestillingDto(), "SAKSBEHANDLER").apply {
                vedleggA1 = brevDataA1
                hovedvirksomhet = arbeidsgiver
                lovvalgsperiode = lagLovvalgsperiode()
                avklartMaritimType = Maritimtyper.SKIP
                arbeidsland = "Norway"
                // anmodningsperiodesvar = lagAnmodningsperiodeSvarInnvilgelse() // Private property
                trygdemyndighetsland = "Denmark"
                avklarteMedfolgendeBarn = lagAvklarteMedfølgendeBarn()
            }
        }

        private fun lagBrevDataAvslagArbeidsgiver(): BrevData {
            return BrevDataAvslagArbeidsgiver("Z007").apply {
                person = lagPersonDokument()
                hovedvirksomhet = AvklartVirksomhet("Virker 100%", "987654321", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID)
                lovvalgsperiode = lagLovvalgsperiode()
                arbeidsland = "Test"
                vilkårbegrunnelser121 = hashSetOf()
                vilkårbegrunnelser121VesentligVirksomhet = hashSetOf()
            }
        }

        private fun lagBrevbestillingAvslagArbeidsgiver(): DoksysBrevbestilling {
            return DoksysBrevbestilling.Builder()
                .medProduserbartDokument(AVSLAG_ARBEIDSGIVER)
                .medBehandling(lagBehandling())
                .build()
        }

        private fun lagStrukturertAdresse(): StrukturertAdresse {
            return StrukturertAdresse().apply {
                landkode = "NL"
                poststed = "Sted"
                postnummer = "1234"
                gatenavn = "Gate"
                husnummerEtasjeLeilighet = "1"
            }
        }

        private fun lagPersonDokument(): PersonDokument {
            return PersonDokument().apply {
                kjønn = lagKjoennsType()
                statsborgerskap = Land(Land.BELGIA)
                fornavn = "For"
                etternavn = "Etter"
                sammensattNavn = "For Etter"
                fødselsdato = LocalDate.ofYearDay(1900, 1)
                bostedsadresse = lagBostedsadresse()
            }
        }

        private fun lagKjoennsType(): KjoennsType {
            return KjoennsType("K")
        }

        private fun lagBehandling(): Behandling {
            val aktører = hashSetOf(
                lagAktør(Aktoersroller.BRUKER),
                lagAktør(Aktoersroller.FULLMEKTIG)
            )
            val fagsak = FagsakTestFactory.builder()
                .medGsakSaksnummer()
                .aktører(aktører)
                .build()
            val behandling = BehandlingTestFactory.builderWithDefaults()
                .medId(BEHANDLINGSID)
                .medFagsak(fagsak)
                .medType(Behandlingstyper.KLAGE)
                .build()

            val søknad = Soeknad()
            val foretakUtland = ForetakUtland().apply {
                orgnr = "12345678910"
            }
            søknad.foretakUtland.add(foretakUtland)
            søknad.juridiskArbeidsgiverNorge = JuridiskArbeidsgiverNorge().apply {
                ekstraArbeidsgivere = listOf(ORGNR)
            }
            val mutableOppholdslandkoder = søknad.oppholdUtland.oppholdslandkoder.toMutableList()
            mutableOppholdslandkoder.add("DK")
            søknad.oppholdUtland.oppholdslandkoder = mutableOppholdslandkoder

            val mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = søknad
            }
            behandling.mottatteOpplysninger = mottatteOpplysninger

            val personopplysninger = lagSaksopplysning(SaksopplysningType.PERSOPL, lagPersonDokument())
            behandling.saksopplysninger = mutableSetOf(personopplysninger)
            return behandling
        }

        private fun lagSaksopplysning(type: SaksopplysningType, dokument: SaksopplysningDokument): Saksopplysning {
            return Saksopplysning().apply {
                this.type = type
                this.dokument = dokument
            }
        }

        private fun lagAvklarteFakta(type: Avklartefaktatyper, subjekt: String?): Avklartefakta {
            return lagAvklarteFakta(type, "TRUE", subjekt)
        }

        private fun lagAvklarteFakta(type: Avklartefaktatyper, fakta: String, subjekt: String?): Avklartefakta {
            return Avklartefakta().apply {
                this.subjekt = subjekt
                this.type = type
                this.fakta = fakta
            }
        }

        private fun lagAktør(type: Aktoersroller): Aktoer {
            return Aktoer().apply {
                aktørId = type.name + idTeller++
                aktørId = "123"
                orgnr = "999"
                rolle = type
                if (type == Aktoersroller.FULLMEKTIG) {
                    setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
                }
            }
        }

        private fun lagLovvalgsperiode(): Lovvalgsperiode {
            return Lovvalgsperiode().apply {
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                fom = LocalDate.now()
                tom = LocalDate.now()
                lovvalgsland = Land_iso2.NO
                tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            }
        }

        private fun lagBehandlingsresultat(faktaliste: List<Avklartefakta>): Behandlingsresultat {
            val behandlingsresultat = Behandlingsresultat().apply {
                avklartefakta = hashSetOf(*faktaliste.toTypedArray())
            }
            val periode = lagLovvalgsperiode()
            val perioder = listOf(periode)
            behandlingsresultat.lovvalgsperioder = hashSetOf(*perioder.toTypedArray())
            return behandlingsresultat
        }
    }

    private fun lagDokumentService(brevdatabyggervelger: BrevDataByggerVelger?): DokumentService {
        val aktør = lagAktør(Aktoersroller.BRUKER)
        val behandling = lagBehandling()
        val behandlingService = mockBehandlingService(behandling)
        val persondataFasade = mockPersondataFasade(aktør)
        val arbeidsgiverFaktum = lagAvklarteFakta(Avklartefaktatyper.VIRKSOMHET, ORGNR)
        val yrkesgruppeFaktum = lagAvklarteFakta(Avklartefaktatyper.YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name, null)
        val behandlingsresultat = lagBehandlingsresultat(
            listOf(
                arbeidsgiverFaktum,
                lagAvklarteFakta(Avklartefaktatyper.ARBEIDSLAND, "SE"),
                yrkesgruppeFaktum
            )
        )
        val behandlingsresultatRepository = mockBehandlingsresultatRepo(behandlingsresultat)
        val avklarteFaktaRepository = mockAvklarteFaktaRepository(arbeidsgiverFaktum, yrkesgruppeFaktum)
        val faktaKonverterer = AvklartefaktaDtoKonverterer()
        val avklartefaktaService = AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, faktaKonverterer)

        val finalBrevdatabyggervelger = brevdatabyggervelger ?: lagBrevdataByggerVelger(avklartefaktaService)

        val saksbehandlerService = mockk<SaksbehandlerService> {
            every { hentNavnForIdent(any()) } returns "Bob Lastname"
        }
        
        val utenlandskMyndighetRepository = mockk<UtenlandskMyndighetRepository>()
        val brevDataService = BrevDataService(behandlingsresultatRepository, persondataFasade, saksbehandlerService, utenlandskMyndighetRepository)
        val brevmottakerService = BrevmottakerService(
            avklarteVirksomheterService,
            mockk<UtenlandskMyndighetService>(),
            behandlingsresultatService,
            mockk<LovvalgsperiodeService>()
        )
        
        val kontaktopplysningService = mockk<KontaktopplysningService> {
            every { hentKontaktopplysning(any(), any()) } returns Optional.empty()
        }
        
        return DokumentService(
            behandlingService,
            brevDataService,
            dokSysFasade,
            brevmottakerService,
            finalBrevdatabyggervelger,
            lagBrevinput(avklartefaktaService),
            kontaktopplysningService
        )
    }

    private fun lagBrevinput(avklartefaktaService: AvklartefaktaService): BrevdataGrunnlagFactory {
        val kodeverkRegister = mockKodeverkRegister()
        val kodeverkService = KodeverkService(kodeverkRegister)
        val eregFasade = mockEregFasade()
        val registerOppslagService = OrganisasjonOppslagService(eregFasade)
        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService, 
            registerOppslagService, 
            mockk<BehandlingService>(), 
            mockk<KodeverkService>()
        )
        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(lagBehandling()).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val dataGrunnlag = BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
        return mockk<BrevdataGrunnlagFactory> {
            every { av(any()) } returns dataGrunnlag
        }
    }

    private fun lagBrevdataByggerVelger(avklartefaktaService: AvklartefaktaService): BrevDataByggerVelger {
        val anmodningsperiodeService = mockk<AnmodningsperiodeService>()
        val mottatteOpplysningerService = mockk<MottatteOpplysningerService>()
        val behandlingsresultatService = mockk<BehandlingsresultatService>()
        val landvelgerService = LandvelgerService(avklartefaktaService, behandlingsresultatService, mottatteOpplysningerService)
        val lovvalgsperiodeService = mockk<LovvalgsperiodeService>()
        val saksopplysningerService = mockk<SaksopplysningerService>()
        val utenlandskMyndighetService = mockk<UtenlandskMyndighetService>()
        val utpekingService = mockk<UtpekingService>()
        val vilkaarsresultatService = mockk<VilkaarsresultatService>()
        val persondataFasade = mockk<PersondataFasade>()
        
        return BrevDataByggerVelger(
            anmodningsperiodeService,
            avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            saksopplysningerService,
            utenlandskMyndighetService,
            utpekingService,
            vilkaarsresultatService,
            persondataFasade,
            mottatteOpplysningerService
        )
    }

    private fun lagBrevdatabyggerVelgerMock(): BrevDataByggerVelger {
        return lagBrevdatabyggerVelgerMock(BrevbestillingDto())
    }

    private fun lagBrevdatabyggerVelgerMock(bestillingDto: BrevbestillingDto?): BrevDataByggerVelger {
        val brevDataByggerInnvilgelse = mockk<BrevDataByggerInnvilgelse>()
        val brevDataByggerAvslagArbeidsgiver = mockk<BrevDataByggerAvslagArbeidsgiver>()
        val brevDataByggerVedlegg = mockk<BrevDataByggerVedlegg>()

        val brevdatabyggervelger = mockk<BrevDataByggerVelger>()
        
        if (bestillingDto != null) {
            if (bestillingDto.mottaker == ARBEIDSGIVER) {
                every { brevdatabyggervelger.hent(any(), any<BrevbestillingDto>()) } returns brevDataByggerAvslagArbeidsgiver
                every { brevDataByggerAvslagArbeidsgiver.lag(any(), any()) } returns lagBrevDataAvslagArbeidsgiver()
            } else {
                every { brevdatabyggervelger.hent(any(), any()) } returns brevDataByggerVedlegg
                every { brevdatabyggervelger.hent(eq(INNVILGELSE_YRKESAKTIV), any()) } returns brevDataByggerInnvilgelse
                every { brevdatabyggervelger.hent(eq(AVSLAG_ARBEIDSGIVER), any()) } returns brevDataByggerAvslagArbeidsgiver
                every { brevDataByggerInnvilgelse.lag(any(), any()) } returns lagBrevDataInnvilgelse()
                every { brevDataByggerAvslagArbeidsgiver.lag(any(), any()) } returns lagBrevDataAvslagArbeidsgiver()
                every { brevDataByggerVedlegg.lag(any(), any()) } returns lagBrevDataInnvilgelse()
            }
        } else {
            every { brevdatabyggervelger.hent(any(), any()) } returns brevDataByggerVedlegg
            every { brevDataByggerVedlegg.lag(any(), any()) } returns lagBrevDataInnvilgelse()
        }

        return brevdatabyggervelger
    }

    private fun mockEregFasade(): EregFasade {
        val adresse = SemistrukturertAdresse().apply {
            landkode = "NO"
            adresselinje1 = "Gate 1"
            postnr = "1234"
            gyldighetsperiode = Periode(LocalDate.now().minusYears(10), LocalDate.now().plusYears(10))
        }
        val organisasjonDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(adresse)
            .build()
        val orgDok = OrganisasjonDokumentTestFactory.builder()
            .orgnummer(ORGNR)
            .navn("Virker av og til")
            .organisasjonsDetaljer(organisasjonDetaljer)
            .build()
        
        return mockk<EregFasade> {
            every { hentOrganisasjon(ORGNR) } returns lagSaksopplysning(SaksopplysningType.ORG, orgDok)
        }
    }

    private fun mockKodeverkRegister(): KodeverkRegister {
        val kodeverk = Kodeverk("", emptyMap())
        return mockk<KodeverkRegister> {
            every { hentKodeverk(FellesKodeverk.POSTNUMMER.navn) } returns kodeverk
        }
    }

    private fun mockAvklarteFaktaRepository(arbeidsgiverFaktum: Avklartefakta, yrkesgruppeFaktum: Avklartefakta): AvklarteFaktaRepository {
        return mockk<AvklarteFaktaRepository> {
            every { findByBehandlingsresultatIdAndType(BEHANDLINGSID, Avklartefaktatyper.YRKESGRUPPE) } returns Optional.of(yrkesgruppeFaktum)
            every { findByBehandlingsresultatIdAndTypeAndFakta(BEHANDLINGSID, Avklartefaktatyper.VIRKSOMHET, "TRUE") } returns setOf(arbeidsgiverFaktum)
            every { findAllByBehandlingsresultatIdAndTypeIn(BEHANDLINGSID, any()) } returns emptySet()
        }
    }

    private fun mockBehandlingsresultatRepo(behandlingsresultat: Behandlingsresultat): BehandlingsresultatRepository {
        return mockk<BehandlingsresultatRepository> {
            every { findById(BEHANDLINGSID) } returns Optional.of(behandlingsresultat)
        }
    }

    private fun mockPersondataFasade(aktør: Aktoer): PersondataFasade {
        return mockk<PersondataFasade> {
            every { hentFolkeregisterident(any()) } returns "IDENT${aktør.aktørId}"
            every { hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        }
    }

    private fun mockBehandlingService(behandling: Behandling): BehandlingService {
        return mockk<BehandlingService> {
            every { hentBehandlingMedSaksopplysninger(BEHANDLINGSID) } returns behandling
            every { hentBehandling(BEHANDLINGSID) } returns behandling
            every { hentBehandlingMedSaksopplysninger(not(eq(BEHANDLINGSID))) } throws IkkeFunnetException("Behandling finnes ikke.")
            every { hentBehandling(not(eq(BEHANDLINGSID))) } throws IkkeFunnetException("Behandling finnes ikke.")
        }
    }
}
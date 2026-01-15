package no.nav.melosys.service.kontroll.feature.anmodningomunntak

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.anmodningsperiodeForTest
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.saksopplysning
import no.nav.melosys.integrasjon.medl.GrunnlagMedl
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class AnmodningUnntakKontrollServiceTest {
    @MockK
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    private lateinit var anmodningUnntakKontrollService: AnmodningUnntakKontrollService

    @BeforeEach
    fun setup() {
        every { persondataFasade.hentPerson(any<String>()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(any()) } returns 1

        anmodningUnntakKontrollService = AnmodningUnntakKontrollService(
            anmodningsperiodeService, avklarteVirksomheterService, behandlingService, persondataFasade, organisasjonOppslagService
        )
    }

    @Test
    fun utførKontroller_brukerManglerAdresse_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
        mockAnmodningsperiode()
        every { persondataFasade.hentPerson(any<String>()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE)
    }

    @Test
    fun utførKontroller_periodeOverlapper_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandlingMedMedlemskapDokument()
        mockAnmodningsperiode()
        every { persondataFasade.hentPerson(any<String>()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
    }

    @Test
    fun utførKontroller_fullmektigPersonManglerAdresse_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                medFullmektig {
                    personIdent = "11111111111"
                    setFullmaktstyper(listOf(Fullmaktstype.FULLMEKTIG_SØKNAD))
                }
            }
        }
        mockAnmodningsperiode()
        every { persondataFasade.hentPerson("11111111111") } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    @Test
    fun utførKontroller_fullmektigOrganisasjonManglerAdresse_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                medFullmektig {
                    orgnr = "111111111"
                    setFullmaktstyper(listOf(Fullmaktstype.FULLMEKTIG_SØKNAD))
                }
            }
        }
        mockAnmodningsperiode()
        every { organisasjonOppslagService.hentOrganisasjon(any()) } returns OrganisasjonDokument("", "null", null, OrganisasjonsDetaljer(), "null")

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    @Test
    fun utførKontroller_anmodningsperiodeManglerSluttdato_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
        mockAnmodningsperiode(tom = null)

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO)
    }

    @Test
    fun utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling {
            mottatteOpplysninger {
                soeknad {
                    fysiskeArbeidssted { }
                }
            }
        }
        mockAnmodningsperiode()

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND)
    }

    @Test
    fun utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        // MottatteOpplysningerData og ForetakUtland er Java DTO-objekter uten forTest DSL - bruker .apply
        val mottatteOpplysningerTestData = MottatteOpplysningerData().apply {
            foretakUtland = listOf(ForetakUtland().apply { selvstendigNæringsvirksomhet = false })
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling {
            mottatteOpplysninger {
                mottatteOpplysningerData = mottatteOpplysningerTestData
            }
        }
        mockAnmodningsperiode()

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL)
    }

    @Test
    fun utførKontroller_flereArbeidsgivere_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
        mockAnmodningsperiode()
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(any()) } returns 2

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET)
    }

    @Test
    fun utførKontroller_storbritanniaBestemmelseBruktFørJanuar2024_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns lagBehandling()
        mockAnmodningsperiode(
            fom = LocalDate.parse("2023-12-31"),
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
        )

        val resultat = anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.STORBRITANNIA_KONV_BRUKT_FOR_TIDLIG)
    }

    private fun lagBehandling(
        init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ) = Behandling.forTest {
        id = BEHANDLING_ID
        status = Behandlingsstatus.UNDER_BEHANDLING
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
            medGsakSaksnummer()
        }
        mottatteOpplysninger {
            soeknad { }
        }
        init()
    }

    private fun lagBehandlingMedMedlemskapDokument(): Behandling {
        val testMedlemsperiode = Medlemsperiode(
            23L,
            Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1)),
            null,
            PeriodestatusMedl.GYLD.kode,
            GrunnlagMedl.FO_12_2.kode,
            null, null, null, null, null
        )
        // MedlemskapDokument er et dokument/DTO-objekt uten forTest DSL - bruker .apply
        val medlemskapDokument = MedlemskapDokument().apply {
            medlemsperiode = listOf(testMedlemsperiode)
        }
        return Behandling.forTest {
            id = BEHANDLING_ID
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak {
                medBruker()
                medGsakSaksnummer()
            }
            mottatteOpplysninger {
                soeknad { }
            }
            saksopplysning {
                type = SaksopplysningType.MEDL
                dokument = medlemskapDokument
            }
        }
    }

    private fun mockAnmodningsperiode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now().plusYears(2),
        bestemmelse: no.nav.melosys.domain.kodeverk.LovvalgBestemmelse? = null
    ) {
        val anmodningsperiode = anmodningsperiodeForTest {
            this.fom = fom
            this.tom = tom
            this.bestemmelse = bestemmelse
        }
        every { anmodningsperiodeService.hentFørsteAnmodningsperiode(BEHANDLING_ID) } returns anmodningsperiode
    }

    companion object {
        private const val BEHANDLING_ID = 33L
    }
}

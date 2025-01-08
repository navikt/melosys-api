package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.brev.*
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.*
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoVirksomhet
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest.*
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.InnvilgelseTrygdeavtale
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.Soknad
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
internal class DokgenMalMapperTest {

    @MockK
    private lateinit var mockInnvilgelseFtrlMapper: InnvilgelseFtrlMapper

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockTrygdeavtaleMapper: TrygdeavtaleMapper

    @MockK
    private lateinit var mockOrienteringAnmodningUnntakMapper: OrienteringAnmodningUnntakMapper

    @MockK
    private lateinit var mockOrienteringTilArbeidsgiverOmVedtakMapper: OrienteringTilArbeidsgiverOmVedtakMapper

    @MockK
    private lateinit var mockInnvilgelseEftaStorbritanniaMapper: InnvilgelseEftaStorbritanniaMapper

    @MockK
    private lateinit var mockInnhentingAvInntektsopplysningerMapper: InnhentingAvInntektsopplysningerMapper

    @MockK
    private lateinit var mockÅrsavregningVedtakMapper: ÅrsavregningVedtakMapper

    private lateinit var dokgenMalMapper: DokgenMalMapper

    @BeforeEach
    fun init() {
        dokgenMalMapper = DokgenMalMapper(
            mockDokgenMapperDatahenter,
            mockInnvilgelseFtrlMapper,
            mockInnvilgelseEftaStorbritanniaMapper,
            mockInnhentingAvInntektsopplysningerMapper,
            mockTrygdeavtaleMapper,
            mockOrienteringAnmodningUnntakMapper,
            mockOrienteringTilArbeidsgiverOmVedtakMapper,
            mockÅrsavregningVedtakMapper
        )
    }

    @Test
    fun feilerNårProduserbartDokumentIkkeErStøttet() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()

        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.ATTEST_A1)
            .medBehandling(DokgenTestData.lagBehandling())
            .build()
        AssertionsForClassTypes.assertThatThrownBy {
            dokgenMalMapper.mapBehandling(
                brevbestilling,
                DokgenTestData.lagMottaker(Mottakerroller.BRUKER)
            )
        }
            .isInstanceOf(FunksjonellException::class.java)
            .hasMessageContaining("ProduserbartDokument ATTEST_A1 er ikke støttet av melosys-dokgen")
    }

    @Test
    fun skalMappeMedBrukerAdressePDL() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns PersonopplysningerObjectFactory.lagDonaldDuckPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns PersonopplysningerObjectFactory.lagDonaldDuckPersondata()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(null) } returns null
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.SE.kode) } returns Landkoder.SE.beskrivelse

        val behandling = DokgenTestData.lagBehandling()
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                saksinfo.navnBruker.shouldContain("Duck Donald")
                mottaker.navn.shouldContain("Duck Donald")
                mottaker.postnr.shouldContain(DokgenTestData.POSTNR_BRUKER)
                mottaker.poststed.shouldContain(DokgenTestData.POSTSTED_BRUKER)
                mottaker.region.shouldContain(DokgenTestData.REGION)
            }
    }

    @Test
    fun skalMappeMedBrukerAdresse() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(null) } returns null
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling()
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build()
        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                saksinfo.navnBruker.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.navn.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.postnr.shouldContain(DokgenTestData.POSTNR_BRUKER)
                mottaker.adresselinjer.shouldContain(DokgenTestData.ADRESSELINJE_1_BRUKER)
            }
    }

    @Test
    fun mapping_persondataFraPdl_ok() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(null) } returns null
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling()
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build()
        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                saksinfo.navnBruker.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.navn.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.postnr.shouldContain(DokgenTestData.POSTNR_BRUKER)
                mottaker.adresselinjer.shouldContain(DokgenTestData.ADRESSELINJE_1_BRUKER)
            }
    }

    @Test
    fun mapping_avsenderMyndighet_ok() {
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode("FI") } returns "Finland"
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling()
        val forsendelseMottattDato = LocalDate.of(2022, 1, 19)
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medForsendelseMottatt(forsendelseMottattDato.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
            .medAvsendertype(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET)
            .medAvsenderLand("FI")
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                avsenderTypeSoknad.shouldBe(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
                avsenderLand.shouldBe("Finland")
                datoBehandlingstid.shouldBe(
                    forsendelseMottattDato.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant()
                        .plus(Period.ofWeeks(Saksbehandlingstid.SAKSBEHANDLINGSTID_UKER))
                )
            }
    }

    @Test
    fun skalMappeMedFullmektigAdresse() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(any()) } returns null

        val behandling = DokgenTestData.lagBehandling()
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg())
            .medForsendelseMottatt(Instant.now())
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.ARBEIDSGIVER))
            .shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                saksinfo.navnBruker.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.navn.shouldContain(DokgenTestData.NAVN_ORG)
                mottaker.postnr.shouldContain(DokgenTestData.POSTNR_ORG)
                mottaker.adresselinjer.shouldContain(DokgenTestData.POSTBOKS_ORG)
            }
    }

    @Test
    fun skalMappeMedFullmektigForretningsAdresse() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(null) } returns null
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling()
        val org = DokgenTestData.lagOrg()
        org.organisasjonDetaljer.forretningsadresse = listOf(lagOrgForretningsadresse())
        org.organisasjonDetaljer.postadresse = emptyList()
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medOrg(org)
            .medForsendelseMottatt(Instant.now())
            .build()

        dokgenMalMapper.mapBehandling(
            brevbestilling,
            DokgenTestData.lagMottakerFullmektig(Aktoertype.ORGANISASJON)
        ).shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                saksinfo.navnBruker.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.navn.shouldContain(DokgenTestData.NAVN_ORG)
                mottaker.postnr.shouldContain(DokgenTestData.POSTNR_ORG)
                mottaker.adresselinjer.shouldContain(FORRETNINGSADRESSE_ORG)
            }
    }

    @Test
    fun skalMappeMedFullmektigMedKontaktpersonAdresse() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(null) } returns null
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling()
        val brevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg())
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .build()

        dokgenMalMapper.mapBehandling(
            brevbestilling,
            DokgenTestData.lagMottakerFullmektig(Aktoertype.ORGANISASJON)
        ).shouldBeInstanceOf<SaksbehandlingstidSoknad>()
            .run {
                saksinfo.navnBruker.shouldContain(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                mottaker.navn.shouldContain(DokgenTestData.NAVN_ORG)
                mottaker.postnr.shouldContain(DokgenTestData.POSTNR_ORG)
                mottaker.adresselinjer.containsAll(listOf("Att: " + DokgenTestData.KONTAKT_NAVN, DokgenTestData.POSTBOKS_ORG))
            }
    }

    @Test
    fun skalMappeMangelbrevTilBruker() {
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentVedtaksdato(any()) } returns Instant.now()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MANGELBREV_BRUKER)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg(Landkoder.NO))
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medManglerInfoFritekst("Dummy")
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<MangelbrevBruker>()
            .run {
                innledningFritekst.shouldContain("Dummy")
                manglerInfoFritekst.shouldContain("Dummy")
                datoInnsendingsfrist.truncatedTo(ChronoUnit.DAYS).shouldBe(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS))
            }
    }

    @Test
    fun skalMappeMangelbrevTilArbeidsgiver() {
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Fullmaktstype.FULLMEKTIG_SØKNAD)) } returns "Fullmektig AS"
        every { mockDokgenMapperDatahenter.hentVedtaksdato(any()) } returns Instant.now()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg(Landkoder.NO))
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medManglerInfoFritekst("Dummy")
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.ARBEIDSGIVER))
            .shouldBeInstanceOf<MangelbrevArbeidsgiver>()
            .run {
                innledningFritekst.shouldContain("Dummy")
                manglerInfoFritekst.shouldContain("Dummy")
                navnFullmektig.shouldContain("Fullmektig AS")
                datoInnsendingsfrist.truncatedTo(ChronoUnit.DAYS).shouldBe(Instant.now().plus(Period.ofWeeks(4)).truncatedTo(ChronoUnit.DAYS))
            }
    }

    @Test
    fun skalMappeFtrlInnvilgelsesbrevTilBruker() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse
        every { mockInnvilgelseFtrlMapper.mapYrkesaktivFrivillig(any()) } returns lagInnvilgelseFtrlYrkesaktivFrivillig()

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg())
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .medUkjentSluttdato(true)
            .build()

        dokgenMalMapper.mapBehandling(
            brevbestilling,
            DokgenTestData.lagMottaker(Mottakerroller.BRUKER)
        ).shouldBeInstanceOf<InnvilgelseFtrlYrkesaktivFrivillig>()
    }

    @Test
    fun skalMappePliktigFtrlInnvilgelsesbrevTilBruker() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse
        every { mockInnvilgelseFtrlMapper.mapYrkesaktivPliktig(any()) } returns lagInnvilgelseFtrlPliktig()

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = DokgenBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.PLIKTIG_MEDLEM_FTRL)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg())
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .build()

        dokgenMalMapper.mapBehandling(
            brevbestilling,
            DokgenTestData.lagMottaker(Mottakerroller.BRUKER)
        ).shouldBeInstanceOf<InnvilgelseYrkesaktivPliktigFtrl>()
    }

    @Test
    fun skalMappeVedtakOpphørtMedlemskapTilBrukerMedRiktigOpphørsdato() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = VedtakOpphoertMedlemskapBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg())
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medOpphørtBegrunnelseFritekst("Dummy")
            .medOpphørtDato(LocalDate.now())
            .build()

        dokgenMalMapper.mapBehandling(
            brevbestilling,
            DokgenTestData.lagMottaker(Mottakerroller.BRUKER)
        ).shouldBeInstanceOf<VedtakOpphoertMedlemskap>()
            .opphoertDato.shouldBe(LocalDate.now())
    }

    @Test
    fun skalMappeVarselManglendeOpplysningerTilBrukerMedRiktigMedlemskapstype() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), any()) } returns null
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(any()) } returns Behandlingsresultat().apply {
            medlemskapsperioder.add(Medlemskapsperiode().apply { medlemskapstype = Medlemskapstyper.FRIVILLIG })
        }
        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = VarselbrevManglendeInnbetalingBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING)
            .medBehandling(behandling)
            .medBetalingsstatus(Betalingsstatus.DELVIS_BETALT)
            .medForsendelseMottatt(Instant.now())
            .build()


        dokgenMalMapper.mapBehandling(
            brevbestilling,
            DokgenTestData.lagMottaker(Mottakerroller.BRUKER)
        ).shouldBeInstanceOf<VarselbrevManglendeInnbetaling>()
            .medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG.kode)
    }

    @Test
    fun skalMappeVarselManglendeOpplysningerTilBrukerKasterFeilDersomBehandlingIkkeHarMedlemskapstype() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(any()) } returns Behandlingsresultat()
        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))

        val brevbestilling: DokgenBrevbestilling = VarselbrevManglendeInnbetalingBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING)
            .medBehandling(behandling)
            .medForsendelseMottatt(Instant.now())
            .build()


        shouldThrow<FunksjonellException> {
            dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
        }.message.shouldBe("Forventer at behandling som tilhører varselbrevet har en opprinnelig behandling med medlemskapsperioder")
    }

    @Test
    fun skalMappeStorbritanniabrev() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockTrygdeavtaleMapper.map(any(), eq(Land_iso2.GB)) } returns lagInnvilgelseOgAttestStorbritannia()
        every { mockDokgenMapperDatahenter.hentVedtaksdato(any()) } returns Instant.now()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.TRYGDEAVTALE_GB)
            .medBehandling(behandling)
            .medOrg(DokgenTestData.lagOrg(Landkoder.GB))
            .medKontaktopplysning(DokgenTestData.lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("Dummy")
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<InnvilgelseOgAttestTrygdeavtale>()
            .run {
                innvilgelse.run {
                    innvilgelse.innledningFritekst.shouldBe("Innledning")
                    artikkel.shouldBe(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1)
                    soknad.virksomhetsnavn.shouldBe("Virksomhetsnavn")
                    isVirksomhetArbeidsgiverSkalHaKopi.shouldBeTrue()
                }
                attest.run {
                    arbeidstaker.navn.shouldBe("Nordmann, Ola")
                    arbeidstaker.fnr.shouldBe("01010119901")
                    arbeidsgiverNorge.virksomhetsnavn.shouldBe("Virksomhetsnavn")
                    arbeidsgiverNorge.fullstendigAdresse.containsAll(listOf("Nordmannsveg 200", "Norge"))
                    utsendelse.artikkel.shouldBe(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1)
                    medfolgendeFamiliemedlemmer.ektefelle.navn.shouldBe("Kone")
                    representant.navn.shouldBe("Mrs. London")
                }
                nyVurderingBakgrunn.shouldBe(Nyvurderingbakgrunner.NYE_OPPLYSNINGER.kode)
                isSkalHaInfoOmRettigheter.shouldBeFalse()
            }
    }

    @Test
    fun skalMappeFritekstbrevTilBruker() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Fullmaktstype.FULLMEKTIG_SØKNAD)) } returns "Fullmektig AS"
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medKontaktopplysninger(true)
            .medSaksbehandlerNavn("Fetter Anton")
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .shouldBeInstanceOf<Fritekstbrev>()
            .run {
                fritekstTittel.shouldContain("Min tittel")
                fritekst.shouldContain("Innhold")
                isMedKontaktopplysninger.shouldBeTrue()
                navnFullmektig.shouldContain("Fullmektig AS")
                saksbehandlerNavn.shouldBe("Fetter Anton")
                mottaker.type.shouldBe(Mottakerroller.BRUKER.kode)
            }
    }

    @Test
    fun skalMappeFritekstbrevTilVirksomhet() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns null
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns null
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medOrg(DokgenTestData.lagOrg())
            .medSaksbehandlerNavn("Fetter Anton")
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.VIRKSOMHET))
            .shouldBeInstanceOf<Fritekstbrev>()
            .run {
                fritekstTittel.shouldBe("Min tittel")
                fritekst.shouldBe("Innhold")
                navnFullmektig.shouldBeNull()
                saksbehandlerNavn.shouldBe("Fetter Anton")
                saksinfo.shouldBeInstanceOf<SaksinfoVirksomhet>()
                    .run {
                        saksnummer.shouldBe(DokgenTestData.SAKSNUMMER)
                        navnVirksomhet.shouldBe(DokgenTestData.NAVN_ORG)
                        orgnr.shouldBe(DokgenTestData.ORGNR)
                    }
                mottaker.type.shouldBe(Mottakerroller.VIRKSOMHET.kode)

            }
    }

    @Test
    fun skalMappeFritekstbrevTilArbeidsgiver() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), eq(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)) } returns null
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
            .medBehandling(behandling)
            .medFritekstTittel("Min tittel")
            .medFritekst("Innhold")
            .medOrg(DokgenTestData.lagOrg())
            .medKontaktopplysninger(true)
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.ARBEIDSGIVER))
            .shouldBeInstanceOf<Fritekstbrev>()
            .run {
                fritekstTittel.shouldBe("Min tittel")
                fritekst.shouldBe("Innhold")
                isMedKontaktopplysninger.shouldBeTrue()
                navnFullmektig.shouldBeNull()
                mottaker.type.shouldBe(Mottakerroller.ARBEIDSGIVER.kode)
            }
    }

    @Test
    fun skalMappeAvslagsbrevPgaManglendeOpplysningerTilBruker() {
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentPersonMottaker(any()) } returns DokgenTestData.lagPersondata()
        every { mockDokgenMapperDatahenter.hentNorskPoststed(any()) } returns "Andeby"
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.NO.kode) } returns Landkoder.NO.beskrivelse

        val behandling = DokgenTestData.lagBehandling(DokgenTestData.lagFagsak(true))
        val brevbestilling: DokgenBrevbestilling = AvslagBrevbestilling.Builder()
            .medProduserbartdokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)
            .medBehandling(behandling)
            .build()

        dokgenMalMapper.mapBehandling(brevbestilling, DokgenTestData.lagMottaker(Mottakerroller.BRUKER))
            .mottaker.type.shouldBe(Mottakerroller.BRUKER.kode)
    }

    private fun lagInnvilgelseBrevbestilling() =
        InnvilgelseBrevbestilling.Builder()
            .medInnledningFritekst("Innledning")
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .build()

    private fun lagInnvilgelseFtrlYrkesaktivFrivillig(): InnvilgelseFtrlYrkesaktivFrivillig {
        return InnvilgelseFtrlYrkesaktivFrivillig(
            brevbestilling = lagInnvilgelseFtrlYrkesaktivFrivilligBrevbestilling(),
            behandlingstype = Behandlingstyper.FØRSTEGANG,
            avgiftsperioder = emptyList(),
            medlemskapsperioder = emptyList(),
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
            avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = false,
            avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = false,
            trygdeavgiftMottaker = Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV,
            fullmektigTrygdeavgift = null,
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG,
            begrunnelse = Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP,
            nyVurderingBakgrunn = null,
            begrunnelseFritekst = null,
            innledningFritekst = null,
            trygdeavgiftFritekst = null,
            begrunnelseAnnenGrunnFritekst = null,
            arbeidsgivere = listOf("Egon Olsen AS"),
            flereLandUkjentHvilke = false,
            land = listOf(Land_iso2.US.kode),
            trygdeavtaleLand = emptyList(),
            betalerArbeidsgiveravgift = true
        )
    }

    private fun lagInnvilgelseFtrlPliktig(): InnvilgelseYrkesaktivPliktigFtrl {
        return InnvilgelseYrkesaktivPliktigFtrl(
            brevbestilling = lagInnvilgelseFtrlYrkesaktivPliktig(),
            behandlingstype = Behandlingstyper.FØRSTEGANG,
            avgiftsperioder = emptyList(),
            datoMottatt = LocalDate.now(),
            medlemskapsperiode = MedlemskapsperiodeDto(
                LocalDate.now(),
                LocalDate.now(),
                Trygdedekninger.FULL_DEKNING_FTRL,
                InnvilgelsesResultat.INNVILGET
            ),
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
            trygdeavgiftMottaker = Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV,
            fullmektigTrygdeavgift = null,
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG,
            begrunnelse = Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_MULTINASJONALT_SELSKAP,
            nyVurderingBakgrunn = null,
            begrunnelseFritekst = null,
            innledningFritekst = null,
            trygdeavgiftFritekst = null,
            begrunnelseAnnenGrunnFritekst = null,
            arbeidsgivere = listOf("Egon Olsen AS"),
            flereLandUkjentHvilke = false,
            land = listOf(Land_iso2.US.kode),
            trygdeavtaleLand = emptyList(),
            betalerArbeidsgiveravgift = true,
            harLavSatsPgaAlder = false,
            arbeidssituasjontype = null,
        )
    }

    private fun lagInnvilgelseFtrlYrkesaktivFrivilligBrevbestilling(): InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling {
        return InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling.Builder()
            .medInnledningFritekst("Innledning")
            .medBegrunnelseFritekst("Begrunnelse")
            .medTrygdeavgiftFritekst("Trygdeavgift fritekst")
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .build()
    }

    private fun lagInnvilgelseFtrlYrkesaktivPliktig(): DokgenBrevbestilling {
        return DokgenBrevbestilling.Builder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .build()
    }

    private fun lagInnvilgelseOgAttestStorbritannia(): InnvilgelseOgAttestTrygdeavtale {
        return InnvilgelseOgAttestTrygdeavtale.Builder(lagInnvilgelseBrevbestilling())
            .innvilgelse(lagInnvilgelseStorbritannia())
            .attest(lagAttestStorbritannia())
            .nyVurderingBakgrunn(Nyvurderingbakgrunner.NYE_OPPLYSNINGER.kode)
            .build()
    }

    private fun lagInnvilgelseStorbritannia(): InnvilgelseTrygdeavtale {
        return InnvilgelseTrygdeavtale.Builder()
            .innvilgelse(Innvilgelse.av(lagInnvilgelseBrevbestilling()))
            .artikkel(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1)
            .soknad(
                Soknad(
                    SOKNADSDATO,
                    DokgenTestData.LOVVALGSPERIODE_FOM,
                    DokgenTestData.LOVVALGSPERIODE_TOM,
                    "Virksomhetsnavn",
                    Land_iso2.GB.beskrivelse
                )
            )
            .familie(null)
            .virksomhetArbeidsgiverSkalHaKopi(true)
            .build()
    }

    private fun lagAttestStorbritannia(): AttestTrygdeavtale {
        val dokgenBrevbestillingBuilder = DokgenBrevbestilling()
            .toBuilder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .build()
        return AttestTrygdeavtale.Builder(dokgenBrevbestillingBuilder)
            .medfolgendeFamiliemedlemmer(
                MedfolgendeFamiliemedlemmer(
                    Person(
                        "Kone",
                        FØDSELSDATO,
                        "01010119901",
                        null
                    ), listOf()
                )
            )
            .arbeidsgiverNorge(
                ArbeidsgiverNorge(
                    "Virksomhetsnavn", listOf("Nordmannsveg 200", "Norge")
                )
            )
            .arbeidstaker(
                Arbeidstaker(
                    "Nordmann, Ola",
                    LocalDate.now().minusDays(20),
                    "01010119901", listOf("Nordmannsveg 200", "Norge")
                )
            )
            .representant(
                RepresentantTrygdeavtale(
                    "Mrs. London", listOf("UK Street 1337")
                )
            )
            .utsendelse(
                Utsendelse(
                    Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1, listOf("UK Street 1337"),
                    DokgenTestData.LOVVALGSPERIODE_FOM,
                    DokgenTestData.LOVVALGSPERIODE_TOM
                )
            )
            .build()
    }

    private fun lagOrgForretningsadresse(): GeografiskAdresse {
        val semistrukturertAdresse = SemistrukturertAdresse()
        semistrukturertAdresse.adresselinje1 = FORRETNINGSADRESSE_ORG
        semistrukturertAdresse.postnr = DokgenTestData.POSTNR_ORG
        semistrukturertAdresse.gyldighetsperiode = Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2))
        semistrukturertAdresse.landkode = "NO"
        return semistrukturertAdresse
    }

    companion object {
        const val FORRETNINGSADRESSE_ORG = "Storgata 1"

        @JvmField
        val SOKNADSDATO = LocalDate.of(2000, 1, 1)
        val FØDSELSDATO = LocalDate.of(2000, 1, 1)
    }
}

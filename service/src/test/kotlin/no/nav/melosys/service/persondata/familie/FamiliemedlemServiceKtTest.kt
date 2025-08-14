package no.nav.melosys.service.persondata.familie

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.Sivilstand
import no.nav.melosys.domain.person.familie.Familierelasjon
import no.nav.melosys.integrasjon.pdl.PDLConsumer
import no.nav.melosys.service.SaksbehandlingDataFactory.lagInaktivBehandlingSomIkkeResulterIVedtak
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PdlObjectFactory.lagPerson
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.persondata.familie.FamiliemedlemObjectFactory.*
import no.nav.melosys.service.persondata.familie.medlem.EktefelleEllerPartnerFamiliemedlemFilter
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FamiliemedlemServiceKtTest {

    private val behandlingService: BehandlingService = mockk()
    private val pdlConsumer: PDLConsumer = mockk()
    private val saksopplysningerService: SaksopplysningerService = mockk()

    private lateinit var familiemedlemService: FamiliemedlemService

    @BeforeEach
    fun beforeEach() {
        familiemedlemService = FamiliemedlemService(
            behandlingService,
            saksopplysningerService,
            EktefelleEllerPartnerFamiliemedlemFilter(pdlConsumer),
            pdlConsumer
        )
    }

    @Test
    fun `hentFamiliemedlemmerFraBehandlingID inaktivBehandlingMedTpsData`() {
        val inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak()
        every { behandlingService.hentBehandling(1L) } returns inaktivBehandling
        val sivilstand: Sivilstand = mockk()
        every { sivilstand.kode } returns "BLA"
        every { sivilstand.tilSivilstandstypeFraDomene() } returns mockk()
        every { saksopplysningerService.harTpsPersonopplysninger(1L) } returns true
        every { saksopplysningerService.hentTpsPersonopplysninger(inaktivBehandling.id) } returns lagPersonDokumentMedFamiliemedlemmer(sivilstand)


        val familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(1L)


        familiemedlemmer.run {
            map { it.navn().fornavn() } shouldContain "BARN"
            map { it.navn().fornavn() } shouldContain "NAVN"
            map { it.familierelasjon() } shouldContain Familierelasjon.BARN
            map { it.familierelasjon() } shouldContain Familierelasjon.RELATERT_VED_SIVILSTAND
        }
    }

    @Test
    fun `hentFamiliemedlemmerFraBehandlingID aktivBehandling`() {
        val behandlingID = 1L
        every { behandlingService.hentBehandling(behandlingID) } returns lagBehandling()
        every { pdlConsumer.hentFamilierelasjoner(FagsakTestFactory.BRUKER_AKTØR_ID) } returns lagHovedpersonMedBarn()
        every { pdlConsumer.hentBarn(IDENT_BARN) } returns lagPerson()
        every { pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT) } returns lagPersonGift()


        val familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(behandlingID)


        familiemedlemmer.run {
            map { it.familierelasjon() } shouldContain Familierelasjon.BARN
            map { it.familierelasjon() } shouldContain Familierelasjon.RELATERT_VED_SIVILSTAND
        }
    }

    @Test
    fun `hentFamiliemedlemmerFraBehandlingID aktivBehandling korrigertPåSammeDato`() {
        val behandlingID = 1L
        every { behandlingService.hentBehandling(behandlingID) } returns lagBehandling()
        every { pdlConsumer.hentFamilierelasjoner(FagsakTestFactory.BRUKER_AKTØR_ID) } returns
            lagHovedpersonMedBarn_medKorrigertGiftSeparertSkiltPåSammeDato()
        every { pdlConsumer.hentBarn(IDENT_BARN) } returns lagPerson()
        every { pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT) } returns lagPersonGift()


        val familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(behandlingID)


        familiemedlemmer.run {
            map { it.familierelasjon() } shouldContain Familierelasjon.BARN
            map { it.familierelasjon() } shouldContain Familierelasjon.RELATERT_VED_SIVILSTAND
        }
    }

    @Test
    fun `hentFamiliemedlemmerFraBehandlingID inaktivBehandling`() {
        val inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak()
        every { behandlingService.hentBehandling(1L) } returns inaktivBehandling
        every { saksopplysningerService.harTpsPersonopplysninger(1L) } returns false
        every { saksopplysningerService.hentPdlPersonopplysninger(1L) } returns
            PersonopplysningerObjectFactory.lagPersonopplysningerMedFamilie()


        val familiemedlemmer = familiemedlemService.hentFamiliemedlemmerFraBehandlingID(1L)


        familiemedlemmer.run {
            map { it.familierelasjon() } shouldContain Familierelasjon.BARN
            map { it.familierelasjon() } shouldContain Familierelasjon.RELATERT_VED_SIVILSTAND
        }
    }

    @Test
    fun `hentFamiliemedlemmer dobbeltGiftemålSituasjon forventerEttGiftemål ogEttBarn`() {
        val hovedperson = lagHovedperson()
        val giftPerson = lagPersonGift()
        every { pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT) } returns giftPerson


        val familiemedlemmer = familiemedlemService.hentFamiliemedlemmer(hovedperson)


        familiemedlemmer.shouldNotBeEmpty()
        familiemedlemmer shouldHaveSize 1
        val medlem = familiemedlemmer.first()
        medlem.run {
            erRelatertVedSivilstand() shouldBe true
            navn().harLiktFornavn(PERSON_GIFT_FORNAVN) shouldBe true
        }
        verify(exactly = 1) { pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT) }
    }

    private fun lagPersonDokumentMedFamiliemedlemmer(sivilstand: Sivilstand): PersonDokument {
        return PersonDokument().apply {
            familiemedlemmer = mutableListOf(
                lagFamiliemedlem("NAVN NAVNSEN", "354652678134", no.nav.melosys.domain.dokument.person.Familierelasjon.EKTE, sivilstand),
                lagFamiliemedlem("BARN NAVNSEN", "134354652678", no.nav.melosys.domain.dokument.person.Familierelasjon.BARN, null)
            )
        }
    }

    private fun lagFamiliemedlem(
        navn: String,
        fnr: String,
        familierelasjon: no.nav.melosys.domain.dokument.person.Familierelasjon,
        sivilstand: Sivilstand?
    ): no.nav.melosys.domain.dokument.person.Familiemedlem {
        return no.nav.melosys.domain.dokument.person.Familiemedlem().apply {
            this.fnr = fnr
            this.navn = navn
            this.familierelasjon = familierelasjon
            this.fødselsdato = LocalDate.EPOCH
            this.fnrAnnenForelder = if (familierelasjon == no.nav.melosys.domain.dokument.person.Familierelasjon.BARN) "fnrAnnen" else null
            this.sivilstand = sivilstand
        }
    }
}

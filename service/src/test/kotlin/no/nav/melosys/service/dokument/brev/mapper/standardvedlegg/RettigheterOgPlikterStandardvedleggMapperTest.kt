package no.nav.melosys.service.dokument.brev.mapper.standardvedlegg

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class RettigheterOgPlikterStandardvedleggMapperTest {

    @MockK
    private lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    private lateinit var mapper: RettigheterOgPlikterStandardvedleggMapper

    @BeforeEach
    fun setup() {
        mapper = RettigheterOgPlikterStandardvedleggMapper(mockBehandlingsresultatService)
    }

    @Test
    fun `skal ikke mappe bestemmelse når skalMappeBestemmelse er false`() {
        val resultat = mapper.mapInnvilgelse(1L, false)

        resultat.bestemmelse.shouldBe(null)
    }

    @Test
    fun `skal mappe bestemmelse fra medlemskapsperiode når tilgjengelig`() {
        every { mockBehandlingsresultatService.hentBehandlingsresultat(1L) } returns lagBehandlingsresultat(
            medlemskapBestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A,
            lovvalgBestemmelse =  null
        )

        val resultat = mapper.mapInnvilgelse(1L, true)

        resultat.bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A.kode.toString())
    }

    @Test
    fun `skal mappe bestemmelse fra lovvalgsperiode når medlemskap mangler bestemmelse`() {
        every { mockBehandlingsresultatService.hentBehandlingsresultat(1L) } returns lagBehandlingsresultat(
            medlemskapBestemmelse = null,
            lovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET
        )

        val resultat = mapper.mapInnvilgelse(1L, true)

        resultat.bestemmelse.shouldBe(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET.kode.toString())
    }

    private fun lagBehandlingsresultat(
        medlemskapBestemmelse: Folketrygdloven_kap2_bestemmelser?,
        lovvalgBestemmelse: LovvalgBestemmelse?
    ): Behandlingsresultat {
        return Behandlingsresultat().apply {
            medlemskapsperioder = mutableSetOf(
                Medlemskapsperiode().apply {
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusMonths(1)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    if (medlemskapBestemmelse != null) {
                        bestemmelse = medlemskapBestemmelse
                    }
                }
            )
            lovvalgsperioder = mutableSetOf(
                Lovvalgsperiode().apply {
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusMonths(1)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    bestemmelse = lovvalgBestemmelse
                }
            )
        }
    }
}

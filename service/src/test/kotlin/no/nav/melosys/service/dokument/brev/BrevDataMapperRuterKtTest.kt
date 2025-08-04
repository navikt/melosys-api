package no.nav.melosys.service.dokument.brev

import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.dokument.brev.mapper.AvslagArbeidsgiverMapper
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelsesbrevMapper
import org.junit.jupiter.api.Test

class BrevDataMapperRuterKtTest {

    @Test
    fun oppslagAvInnvilgelseYrkesaktivGirInnvelgelsesbrevMapper() {
        val resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV)
        resultat.shouldBeInstanceOf<InnvilgelsesbrevMapper>()
    }

    @Test
    fun oppslagAvAvslagArbeidsgiverbrevGirAvslagArbeidsgiverMapper() {
        val resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER)
        resultat.shouldBeInstanceOf<AvslagArbeidsgiverMapper>()
    }
}

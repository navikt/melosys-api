package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.felles.Periode
import java.math.BigDecimal


class PermisjonOgPermittering : HarPeriode {
    var permisjonsId: String? = null
    var permisjonsPeriode: Periode? = null
    var permisjonsprosent: BigDecimal? = null
    var permisjonOgPermittering: String? = null

    @JsonIgnore
    override fun getPeriode(): Periode {
        return permisjonsPeriode!!
    }
}


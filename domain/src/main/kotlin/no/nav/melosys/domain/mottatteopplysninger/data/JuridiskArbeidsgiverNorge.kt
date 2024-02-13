package no.nav.melosys.domain.mottatteopplysninger.data

import java.math.BigDecimal
import java.util.stream.Stream


/**
 * Opplysninger om juridiske arbeidsgivere i Norge
 * Opplysningene er for å kunne vurdere vesentlig virksomhet i Norge.
 * De er bare relevant når det gjelder utsendt arbeidstaker og pre-utfyllingen fra informasjon innsendt tidligere (fra samme arbeidsgiver) er eldre enn 12 måneder.
 */
class JuridiskArbeidsgiverNorge {
    var antallAdmAnsatte: Int? = null
    var antallAnsatte: Int? = null
    var antallUtsendte: Int? = null
    var andelOmsetningINorge: BigDecimal? = null
    var andelOppdragINorge: BigDecimal? = null
    var andelKontrakterINorge: BigDecimal? = null
    var andelRekruttertINorge: BigDecimal? = null
    var ekstraArbeidsgivere: List<String> = ArrayList()
    var erOffentligVirksomhet: Boolean? = null
    fun hentManueltRegistrerteArbeidsgiverOrgnumre(): Stream<String> {
        return ekstraArbeidsgivere.stream()
    }
}

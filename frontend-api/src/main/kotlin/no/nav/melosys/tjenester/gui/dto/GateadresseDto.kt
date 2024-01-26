package no.nav.melosys.tjenester.gui.dto

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class GateadresseDto {
    @JvmField
    var gatenavn: String? = null

    var gatenummer: Int? = null

    var husnummer: Int? = null

    var husbokstav: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is GateadresseDto) {
            return false
        }

        val that = o

        return EqualsBuilder().append(gatenavn, that.gatenavn).append(gatenummer, that.gatenummer)
            .append(husnummer, that.husnummer).append(husbokstav, that.husbokstav).isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(gatenavn).append(gatenummer).append(husnummer).append(husbokstav).toHashCode()
    }
}

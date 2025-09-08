package no.nav.melosys.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.*

@Embeddable
class KontaktopplysningID(
    @Column(name = "saksnummer", nullable = false)
    var saksnummer: String? = null, // burde være non-null, men er en issue med dette i Kontaktopplysning

    @Column(name = "orgnr", nullable = false)
    var orgnr: String? = null // kan ikke være non-null pga YrkesaktivEosVedtakIT - innvigelse med bestemmelse FO_883_2004_ART12_1
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KontaktopplysningID) return false
        return saksnummer == other.saksnummer && orgnr == other.orgnr
    }

    override fun hashCode(): Int = Objects.hash(saksnummer, orgnr)
}

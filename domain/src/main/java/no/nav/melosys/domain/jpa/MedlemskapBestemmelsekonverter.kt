package no.nav.melosys.domain.jpa

import jakarta.persistence.AttributeConverter
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser

class MedlemskapBestemmelsekonverter : AttributeConverter<Bestemmelse, String> {
    override fun convertToDatabaseColumn(attribute: Bestemmelse?) =
        attribute?.name()

    override fun convertToEntityAttribute(dbData: String?): Bestemmelse? =
        Folketrygdloven_kap2_bestemmelser.values().firstOrNull { it.name == dbData?.uppercase() }
            ?: Vertslandsavtale_bestemmelser.values().firstOrNull { it.name == dbData?.uppercase() }
}

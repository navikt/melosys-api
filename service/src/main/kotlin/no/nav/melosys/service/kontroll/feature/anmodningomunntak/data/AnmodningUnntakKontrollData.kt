package no.nav.melosys.service.kontroll.feature.anmodningomunntak.data

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.person.Persondata


data class AnmodningUnntakKontrollData(
    val persondata: Persondata,
    val mottatteOpplysningerData: MottatteOpplysningerData,
    val anmodningsperiode: Anmodningsperiode,
    val antallArbeidsgivere: Int,
    val fullmektig: Aktoer?,
    val organisasjonDokument: OrganisasjonDokument?,
    val persondataTilFullmektig: Persondata?,
)

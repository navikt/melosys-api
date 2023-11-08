package no.nav.melosys.service.brev

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.persondata.PersondataFasade
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component


@Component
class TilBrevAdresseService(
    private val persondataFasade: PersondataFasade,
    private val kontaktopplysningService: KontaktopplysningService,
    private val eregFasade: EregFasade
) {
    fun tilBrevAdresse(mottaker: Mottaker, behandling: Behandling): BrevAdresse {
        var persondata: Persondata? = null
        var kontaktopplysning: Kontaktopplysning? = null
        var orgDokument: OrganisasjonDokument? = null

        when (mottaker.rolle) {
            Mottakerroller.BRUKER -> persondata = persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID())
            Mottakerroller.FULLMEKTIG -> {
                if (mottaker.personIdent != null) {
                    persondata = persondataFasade.hentPerson(mottaker.personIdent)
                } else {
                    kontaktopplysning = hentKontaktopplysninger(behandling, mottaker)
                    orgDokument = hentOrganisasjonsDokument(kontaktopplysning, mottaker.orgnr)
                }
            }

            Mottakerroller.VIRKSOMHET, Mottakerroller.ARBEIDSGIVER -> {
                kontaktopplysning = hentKontaktopplysninger(behandling, mottaker)
                orgDokument = hentOrganisasjonsDokument(kontaktopplysning, mottaker.orgnr)
            }

            else -> throw FunksjonellException("Mottakersrolle støttes ikke: " + mottaker.rolle)
        }

        if (orgDokument == null && persondata == null) {
            throw FunksjonellException("Orgdata eller persondata forventes for å sende brev.")
        }

        return BrevAdresse(
            mottakerNavn = DokgenAdresseMapper.mapNavn(orgDokument, persondata),
            orgnr = orgDokument?.orgnummer,
            adresselinjer = DokgenAdresseMapper.mapAdresselinjer(orgDokument, null, kontaktopplysning, persondata)
                ?.filterNot { StringUtils.isEmpty(it) },
            postnr = DokgenAdresseMapper.mapPostnr(orgDokument, persondata),
            poststed = if (orgDokument != null) DokgenAdresseMapper.mapPoststed(orgDokument) else mapPoststed(persondata!!),
            region = DokgenAdresseMapper.mapRegionForAdresse(orgDokument, persondata),
            land = DokgenAdresseMapper.mapLandForAdresse(orgDokument, persondata),
        )
    }

    fun tilBrevAdresse(personIdent: String?, organisasjonsnummer: String?): BrevAdresse {
        var persondata: Persondata? = null
        var orgDokument: OrganisasjonDokument? = null

        if (personIdent != null) {
            persondata = persondataFasade.hentPerson(personIdent)
                ?: throw FunksjonellException("Finner ikke persondata for personIdent.")
        } else if (organisasjonsnummer != null) {
            orgDokument = hentOrganisasjonsDokument(null, organisasjonsnummer)
        } else {
            throw FunksjonellException("Kan ikke finne adresse uten personIdent og organisasjonsnummer")
        }

        return BrevAdresse(
            mottakerNavn = DokgenAdresseMapper.mapNavn(orgDokument, persondata),
            orgnr = orgDokument?.orgnummer,
            adresselinjer = DokgenAdresseMapper.mapAdresselinjer(orgDokument, null, null, persondata)?.filterNot { StringUtils.isEmpty(it) },
            postnr = DokgenAdresseMapper.mapPostnr(orgDokument, persondata),
            poststed = if (orgDokument != null) DokgenAdresseMapper.mapPoststed(orgDokument) else mapPoststed(persondata!!),
            region = DokgenAdresseMapper.mapRegionForAdresse(orgDokument, persondata),
            land = DokgenAdresseMapper.mapLandForAdresse(orgDokument, persondata)
        )
    }

    private fun hentKontaktopplysninger(behandling: Behandling, mottaker: Mottaker): Kontaktopplysning? {
        return kontaktopplysningService.hentKontaktopplysning(behandling.fagsak.saksnummer, mottaker.orgnr).orElse(null)
    }

    private fun hentOrganisasjonsDokument(kontaktopplysning: Kontaktopplysning?, orgnr: String): OrganisasjonDokument {
        val mottakerOrgnr = kontaktopplysning?.kontaktOrgnr ?: orgnr
        return eregFasade.hentOrganisasjon(mottakerOrgnr).dokument as OrganisasjonDokument
    }

    private fun mapPoststed(persondata: Persondata): String? {
        return persondata.hentGjeldendePostadresse()?.poststed
    }
}

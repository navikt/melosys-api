package no.nav.melosys.service.brev.bestilling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.*;

@Component
public class TilBrevAdresseService {
    private final PersondataFasade persondataFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final EregFasade eregFasade;


    public TilBrevAdresseService(PersondataFasade persondataFasade, KontaktopplysningService kontaktopplysningService, EregFasade eregFasade) {
        this.persondataFasade = persondataFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.eregFasade = eregFasade;
    }

    public BrevAdresse tilBrevAdresse(Mottaker mottaker, Behandling behandling) {
        Persondata persondata = null;
        Kontaktopplysning kontaktopplysning = null;
        OrganisasjonDokument orgDokument = null;

        switch (mottaker.getRolle()) {
            case BRUKER -> persondata = persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
            case FULLMEKTIG -> {
                if (mottaker.getPersonIdent() != null) {
                    persondata = persondataFasade.hentPerson(mottaker.getPersonIdent());
                } else {
                    kontaktopplysning = hentKontaktopplysninger(behandling, mottaker);
                    orgDokument = hentOrganisasjonsDokument(kontaktopplysning, mottaker.getOrgnr());
                }
            }
            case VIRKSOMHET, ARBEIDSGIVER -> {
                kontaktopplysning = hentKontaktopplysninger(behandling, mottaker);
                orgDokument = hentOrganisasjonsDokument(kontaktopplysning, mottaker.getOrgnr());
            }
            default -> throw new FunksjonellException("Mottakersrolle støttes ikke: " + mottaker.getRolle());
        }

        if (orgDokument == null && persondata == null) {
            throw new FunksjonellException("Orgdata eller persondata forventes for å sende brev.");
        }

        return new BrevAdresse.Builder()
            .medMottakerNavn(mapNavn(orgDokument, persondata))
            .medOrgnr(orgDokument != null ? orgDokument.getOrgnummer() : null)
            .medAdresselinjer(mapAdresselinjer(orgDokument, null, kontaktopplysning, persondata))
            .medPostnr(mapPostnr(orgDokument, persondata))
            .medPoststed(orgDokument != null ? DokgenAdresseMapper.mapPoststed(orgDokument) : mapPoststed(persondata))
            .medRegion(mapRegionForAdresse(orgDokument, persondata))
            .medLand(mapLandForAdresse(orgDokument, persondata))
            .build();
    }

    public BrevAdresse tilBrevAdresse(String personIdent, String organisasjonsnummer) {
        Persondata persondata = null;
        OrganisasjonDokument orgDokument = null;

        if (personIdent != null) {
            persondata = persondataFasade.hentPerson(personIdent);
        } else if (organisasjonsnummer != null) {
            orgDokument = hentOrganisasjonsDokument(null, organisasjonsnummer);
        }

        if (orgDokument == null && persondata == null) {
            throw new FunksjonellException("Orgdata eller persondata forventes for å finne adresse.");
        }


        return new BrevAdresse.Builder()
            .medMottakerNavn(mapNavn(orgDokument, persondata))
            .medOrgnr(orgDokument != null ? orgDokument.getOrgnummer() : null)
            .medAdresselinjer(mapAdresselinjer(orgDokument, null, null, persondata))
            .medPostnr(mapPostnr(orgDokument, persondata))
            .medPoststed(orgDokument != null ? DokgenAdresseMapper.mapPoststed(orgDokument) : mapPoststed(persondata))
            .medRegion(mapRegionForAdresse(orgDokument, persondata))
            .medLand(mapLandForAdresse(orgDokument, persondata))
            .build();
    }

    private Kontaktopplysning hentKontaktopplysninger(Behandling behandling, Mottaker mottaker) {
        return kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null);
    }

    private OrganisasjonDokument hentOrganisasjonsDokument(Kontaktopplysning kontaktopplysning, String orgnr) {
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null
            ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        return (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
    }

    private String mapPoststed(Persondata persondata) {
        Postadresse postadresse = persondata.hentGjeldendePostadresse();
        if (postadresse == null) {
            return null;
        }
        return postadresse.poststed();
    }
}

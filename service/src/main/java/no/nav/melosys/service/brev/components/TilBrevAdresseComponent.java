package no.nav.melosys.service.brev.components;

import java.util.stream.Stream;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.*;

@Component
class TilBrevAdresseComponent {
    private final PersondataFasade persondataFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final EregFasade eregFasade;

    public TilBrevAdresseComponent(PersondataFasade persondataFasade, KontaktopplysningService kontaktopplysningService, UtenlandskMyndighetService utenlandskMyndighetService, EregFasade eregFasade) {
        this.persondataFasade = persondataFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.eregFasade = eregFasade;
    }

    public BrevAdresse tilBrevAdresse(Aktoer mottaker, Behandling behandling) {
        Persondata persondata = null;
        Kontaktopplysning kontaktopplysning = null;
        OrganisasjonDokument orgDokument = null;

        switch (mottaker.getRolle()) {
            case BRUKER: {
                persondata = persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
                break;
            }
            case REPRESENTANT: {
                if (mottaker.erPerson()) {
                    persondata = persondataFasade.hentPerson(mottaker.getPersonIdent());
                    break;
                }
            }
            case VIRKSOMHET, ARBEIDSGIVER, ETAT: {
                kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null);
                String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
                orgDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
                break;
            }
            case TRYGDEMYNDIGHET: {
                var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(mottaker.hentMyndighetLandkode(), UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);
                return new BrevAdresse.Builder().medMottakerNavn(utenlandskMyndighet.navn).medAdresselinjer(Stream.of(utenlandskMyndighet.gateadresse1, utenlandskMyndighet.gateadresse2).toList()).medPostnr(utenlandskMyndighet.postnummer).medPoststed(utenlandskMyndighet.poststed).medLand(utenlandskMyndighet.land).build();
            }
            default:
                throw new FunksjonellException("Mottakersrolle støttes ikke: " + mottaker.getRolle());
        }

        if (orgDokument == null && persondata == null) {
            throw new FunksjonellException("Orgdata eller persondata forventes for å sende brev.");
        }

        return new BrevAdresse.Builder().medMottakerNavn(mapNavn(orgDokument, persondata)).medOrgnr(orgDokument != null ? orgDokument.getOrgnummer() : null).medAdresselinjer(mapAdresselinjer(orgDokument, null, kontaktopplysning, persondata)).medPostnr(mapPostnr(orgDokument, persondata)).medPoststed(orgDokument != null ? DokgenAdresseMapper.mapPoststed(orgDokument) : mapPoststed(persondata)).medRegion(mapRegionForAdresse(orgDokument, persondata)).medLand(mapLandForAdresse(orgDokument, persondata)).build();
    }

    private String mapPoststed(Persondata persondata) {
        Postadresse postadresse = persondata.hentGjeldendePostadresse();
        if (postadresse == null) {
            return null;
        }
        return postadresse.poststed();
    }
}

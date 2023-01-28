package no.nav.melosys.service.brev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.integrasjon.dokgen.DokgenAdresseMapper.*;

@Service
public class BrevmalListeService {

    private final BrevmottakerService brevmottakerService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final EregFasade eregFasade;

    public BrevmalListeService(BrevmottakerService brevmottakerService,
                               BehandlingService behandlingService,
                               PersondataFasade persondataFasade,
                               KontaktopplysningService kontaktopplysningService,
                               UtenlandskMyndighetService utenlandskMyndighetService,
                               EregFasade eregFasade) {
        this.brevmottakerService = brevmottakerService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.eregFasade = eregFasade;
    }


    @Transactional
    public List<Produserbaredokumenter> hentMuligeProduserbaredokumenter(long behandlingId, Aktoersroller rolle) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        if (behandling.erInaktiv()) {
            return emptyList();
        }

        return switch (rolle) {
            case BRUKER -> {
                List<Produserbaredokumenter> brevmaler = new ArrayList<>();
                if (behandling.getFagsak().getTema() == Sakstemaer.MEDLEMSKAP_LOVVALG && behandling.getType() == Behandlingstyper.FØRSTEGANG) {
                    brevmaler.add(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
                }
                brevmaler.addAll(asList(MANGELBREV_BRUKER, GENERELT_FRITEKSTBREV_BRUKER));
                yield brevmaler;
            }
            case ARBEIDSGIVER -> List.of(MANGELBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER);
            case VIRKSOMHET -> Collections.singletonList(GENERELT_FRITEKSTBREV_VIRKSOMHET);
            case TRYGDEMYNDIGHET -> Collections.singletonList(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);
            case ETAT -> Collections.singletonList(FRITEKSTBREV);
            default -> throw new FunksjonellException("Rollen " + rolle + " kan ikke sende brev gjennom brevmenyen");
        };
    }

    @Transactional
    public List<BrevAdresse> hentBrevAdresseTilMottakere(Aktoersroller aktoersroller, long behandlingId) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var mottakere = brevmottakerService.avklarMottakere(null, Mottaker.av(aktoersroller), behandling, false, false);
        List<BrevAdresse> brevAdresser = new ArrayList<>();

        for (Aktoer mottaker : mottakere) {
            brevAdresser.add(tilBrevAdresse(mottaker, behandling));
        }
        return brevAdresser;
    }

    private BrevAdresse tilBrevAdresse(Aktoer mottaker, Behandling behandling) {
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
            case VIRKSOMHET, ARBEIDSGIVER: {
                kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(),
                    mottaker.getOrgnr()).orElse(null);
                String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
                orgDokument = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument();
                break;
            }
            case TRYGDEMYNDIGHET: {
                var utenlandskMyndighet =
                    utenlandskMyndighetService.hentUtenlandskMyndighet(mottaker.hentMyndighetLandkode(), UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);
                return new BrevAdresse.Builder()
                    .medMottakerNavn(utenlandskMyndighet.navn)
                    .medAdresselinjer(Stream.of(utenlandskMyndighet.gateadresse1, utenlandskMyndighet.gateadresse2).toList())
                    .medPostnr(utenlandskMyndighet.postnummer)
                    .medPoststed(utenlandskMyndighet.poststed)
                    .medLand(utenlandskMyndighet.land)
                    .build();
            }
            default:
                throw new FunksjonellException("Mottakersrolle støttes ikke: " + mottaker.getRolle());
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

    private String mapPoststed(Persondata persondata) {
        Postadresse postadresse = persondata.hentGjeldendePostadresse();
        if (postadresse == null) {
            return null;
        }
        return postadresse.poststed();
    }


}

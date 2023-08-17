package no.nav.melosys.tjenester.gui.kontroll;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.person.adresse.PersonAdresse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.persondata.PersondataService;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.tjenester.gui.dto.kontroller.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/kontroll")
@Api(tags = "kontroll")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class KontrollTjeneste {

    private final PersondataService persondataService;
    private final OrganisasjonOppslagService organisasjonOppslagService;
    private final FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    private final Aksesskontroll aksesskontroll;
    private final EessiService eessiService;
    private final BehandlingService behandlingService;

    public KontrollTjeneste(FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade, Aksesskontroll aksesskontroll,
                            EessiService eessiService, BehandlingService behandlingService, PersondataService persondataService, OrganisasjonOppslagService organisasjonOppslagService) {
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
        this.aksesskontroll = aksesskontroll;
        this.eessiService = eessiService;
        this.behandlingService = behandlingService;
        this.persondataService = persondataService;
        this.organisasjonOppslagService = organisasjonOppslagService;
    }

    @GetMapping("{behandlingId}/erBucAapen")
    public ResponseEntity<Boolean> erBucAapen(@PathVariable("behandlingId") Long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        return ResponseEntity.ok(eessiService.erBucAapen(behandling.getFagsak().getGsakSaksnummer()));
    }

    @PostMapping("/harRegistrertAdresse")
    public ResponseEntity<KontrollerAdresseBrukerFullmektigResponseDto> harRegistrertAdresse(@RequestBody KontrollerAdresseBrukerFullmektigDto kontrollerAdresseBrukerFullmektigDto) {
        AdressesjekkKontekst kontekst = new AdressesjekkKontekst(kontrollerAdresseBrukerFullmektigDto);

        if (kontekst.getBehandlingID() != null) {
            oppdaterKontekstForBehandling(kontekst);
        }

        boolean harRegistrertAdresse;
        if (!kontekst.getBrukerID().isEmpty()) {
            harRegistrertAdresse = sjekkPersonHarRegisteredAddress(kontekst.getBrukerID());
        } else {
            harRegistrertAdresse = sjekkOrgHarAdresse(kontekst.getOrgnr());
        }

        var responseDto = new KontrollerAdresseBrukerFullmektigResponseDto(kontekst.getRolle(), harRegistrertAdresse);
        return ResponseEntity.ok(responseDto);
    }

    private void oppdaterKontekstForBehandling(AdressesjekkKontekst kontekst) {
        var behandling = behandlingService.hentBehandling(kontekst.getBehandlingID());
        var representantBruker = behandling.getFagsak().finnRepresentant(Representerer.BRUKER);
        if (representantBruker.isPresent()) {
            kontekst.oppdaterForRepresentantBruker(representantBruker.get());
        } else {
            kontekst.oppdaterForBruker(behandling.getFagsak().hentBrukersAktørID());
        }
    }

    private boolean sjekkPersonHarRegisteredAddress(String brukerID) {
        var person = persondataService.hentPerson(brukerID);
        return Stream.of(
                person.finnBostedsadresse(),
                person.finnOppholdsadresse(),
                person.finnKontaktadresse())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(PersonAdresse::harRegistrertAdresse);
    }

    private Boolean sjekkOrgHarAdresse(String orgnr) {
        var organisasjon = organisasjonOppslagService.hentOrganisasjon(orgnr);
        var organisasjonHarRegistrertPostadresse = !organisasjon.getPostadresse().erTom() && !organisasjon.getPostadresse().getPostnummer().isBlank();
        var organisasjonHarRegistrertForretningsadresse = !organisasjon.getForretningsadresse().erTom() && !organisasjon.getForretningsadresse().getPostnummer().isBlank();

        return (organisasjonHarRegistrertPostadresse || organisasjonHarRegistrertForretningsadresse);
    }

    @PostMapping("/ferdigbehandling")
    public ResponseEntity<KontrollerFerdigbehandlingDto> kontrollerFerdigbehandling(@RequestBody FerdigbehandlingKontrollerDto ferdigbehandlingKontrollerDto) {

        if (ferdigbehandlingKontrollerDto.vedtakstype() == null) {
            throw new FunksjonellException("Vedtakstype mangler.");
        }
        aksesskontroll.autoriser(
            ferdigbehandlingKontrollerDto.behandlingID(),
            ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres() ? Aksesstype.SKRIV : Aksesstype.LES
        );

        Collection<Kontrollfeil> kontrollfeil = ferdigbehandlingKontrollFacade.kontroller(
            ferdigbehandlingKontrollerDto.behandlingID(),
            ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres(),
            ferdigbehandlingKontrollerDto.behandlingsresultattype(),
            ferdigbehandlingKontrollerDto.kontrollerSomSkalIgnoreres()
        );

        return ResponseEntity.ok(new KontrollerFerdigbehandlingDto(kontrollfeil.stream().map(Kontrollfeil::tilDto).toList()));
    }
}

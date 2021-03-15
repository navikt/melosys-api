package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

@Protected
@RestController
@RequestMapping("/personer")
@Api(tags = {"personer"})
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PersonTjeneste {
    private final RegisterOppslagService registerOppslag;
    private final TilgangService tilgangService;
    private final KodeverkService kodeverkService;

    @Autowired
    public PersonTjeneste(RegisterOppslagService registerOppslag, TilgangService tilgangService, KodeverkService kodeverkService) {
        this.registerOppslag = registerOppslag;
        this.tilgangService = tilgangService;
        this.kodeverkService = kodeverkService;
    }

    @GetMapping("{fnr}")
    @JsonView(DokumentView.FrontendApi.class)
    @ApiOperation(value = "Henter en person fra TPS.", response = PersonDokument.class)
    public ResponseEntity getPerson(@PathVariable("fnr") String personnummer)
        throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        if (personnummer == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        PersonDokument personDokument = registerOppslag.hentPerson(personnummer);
        tilgangService.sjekkFnr(personnummer);
        return ResponseEntity.ok(new PersonDto(personDokument));
    }

    @GetMapping("{fnr}/gjeldeneAdresse")
    @ApiOperation(value = "Henter gjeldene adresse for person fra TPS")
    public ResponseEntity<UstrukturertAdresse> getGjeldeneAdresse(@PathVariable("fnr") String personnummer)
        throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        if (personnummer == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        tilgangService.sjekkFnr(personnummer);

        var gjeldendeAdresse= registerOppslag.hentPerson(personnummer).gjeldendePostadresse;
        gjeldendeAdresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, gjeldendeAdresse.getPostnr(), LocalDate.now()));

        return ResponseEntity.ok(
            UstrukturertAdresse.av(gjeldendeAdresse)
        );
    }
}

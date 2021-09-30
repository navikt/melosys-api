package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import static java.util.stream.Collectors.toList;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AktoerTjeneste {
    private final Aksesskontroll aksesskontroll;
    private final AktoerService aktoerService;
    private final FagsakService fagsakService;

    @Autowired
    public AktoerTjeneste(Aksesskontroll aksesskontroll,
                          AktoerService aktoerService,
                          FagsakService fagsakService) {
        this.aksesskontroll = aksesskontroll;
        this.aktoerService = aktoerService;
        this.fagsakService = fagsakService;
    }

    @GetMapping("/{saksnummer}/aktoerer")
    @ApiOperation(
        value = "Henter aktører knyttet til et gitt saksnummer.",
        response = AktoerDto.class,
        responseContainer = "List")
    public List<AktoerDto> hentAktoerer(@PathVariable("saksnummer") String saksnummer,
                                        @RequestParam(value = "rolleKode", required = false) String rolleKode,
                                        @RequestParam(value = "representerer", required = false) String representerer) {

        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        Aktoersroller rolle = null;
        Representerer representantRepresenterer = null;
        if (StringUtils.isNotEmpty(rolleKode)) {
            rolle = Aktoersroller.valueOf(rolleKode);
        }
        if (StringUtils.isNotEmpty(representerer)) {
            representantRepresenterer = Representerer.valueOf(representerer);
        }

        List<Aktoer> aktører = aktoerService.hentfagsakAktører(fagsak, rolle, representantRepresenterer);
        return aktører.stream().map(this::tilDto).collect(toList());
    }

    @PostMapping("/{saksnummer}/aktoerer")
    @ApiOperation(
        value = "Lagrer/oppdaterer aktør informasjon for et gitt saksnummer.",
        response = AktoerDto.class)
    public ResponseEntity<AktoerDto> lagAktoerer(@PathVariable("saksnummer") String saksnummer,
                                                 @RequestBody AktoerDto aktoerDto)
        {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);
        Long databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto);
        aktoerDto.setDatabaseID(databaseId);
        return ResponseEntity.ok(aktoerDto);
    }

    @DeleteMapping("/aktoerer/{databaseID}")
    @ApiOperation(
        value = "Sletter aktøren med en gitt database-id.",
        response = AktoerDto.class)
    public ResponseEntity<Void> slettAktoer(@PathVariable("databaseID") long databaseID) {
        aktoerService.slettAktoer(databaseID);
        return ResponseEntity.ok().build();
    }

    private AktoerDto tilDto(Aktoer aktoer) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID(aktoer.getAktørId());
        aktoerDto.setInstitusjonsID(aktoer.getInstitusjonId());
        aktoerDto.setOrgnr(aktoer.getOrgnr());
        aktoerDto.setRolleKode(aktoer.getRolle().getKode());
        aktoerDto.setUtenlandskPersonID(aktoer.getUtenlandskPersonId());
        if (aktoer.getRepresenterer() != null) {
            aktoerDto.setRepresentererKode(aktoer.getRepresenterer().getKode());
        }
        aktoerDto.setDatabaseID(aktoer.getId());
        return aktoerDto;
    }
}

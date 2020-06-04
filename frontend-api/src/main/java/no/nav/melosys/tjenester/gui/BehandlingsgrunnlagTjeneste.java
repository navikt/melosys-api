package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsgrunnlagTilleggsData;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagPostDto;
import no.nav.melosys.tjenester.gui.schema.ClasspathSchemaClient;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import no.nav.security.token.support.core.api.Protected;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.stream.Collectors;

@Protected
@RestController
@Api(tags = "behandlingsgrunnlag")
@RequestMapping("/behandlingsgrunnlag")
public class BehandlingsgrunnlagTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(BehandlingsgrunnlagTjeneste.class);

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final RegisterOppslagService registerOppslagService;
    private final TilgangService tilgangService;

    private final static String BEHANDLINGSGRUNNLAG_POST_SCHEMA = "behandlingsgrunnlag-post-schema.json";
    private final Schema behandlingsgrunnlagPostSchema;

    public BehandlingsgrunnlagTjeneste(BehandlingsgrunnlagService behandlingsgrunnlagService, RegisterOppslagService registerOppslagService, TilgangService tilgangService) throws IOException {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.registerOppslagService = registerOppslagService;
        this.tilgangService = tilgangService;

        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), BEHANDLINGSGRUNNLAG_POST_SCHEMA);
        JSONObject rawSchema = new JSONObject(schemaString);
        SchemaLoader loader = SchemaLoader.builder()
            .schemaJson(rawSchema)
            .httpClient(new ClasspathSchemaClient())
            .draftV7Support()
            .useDefaults(true)
            .build();
        behandlingsgrunnlagPostSchema = loader.load().build();
    }

    @GetMapping("/{behandlingID}")
    public ResponseEntity<BehandlingsgrunnlagGetDto> hentBehandlingsgrunnlag(
        @PathVariable(value = "behandlingID") long behandlingID
    ) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        tilgangService.sjekkTilgang(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID);
        return ResponseEntity.ok(new BehandlingsgrunnlagGetDto(behandlingsgrunnlag, hentTilleggsData(behandlingsgrunnlag.getBehandlingsgrunnlagdata())));
    }

    @PostMapping("/{behandlingID}")
    public ResponseEntity<BehandlingsgrunnlagGetDto> oppdaterBehandlingsgrunnlag(
        @PathVariable(value = "behandlingID") long behandlingID,
        @RequestBody BehandlingsgrunnlagPostDto behandlingsgrunnlagPostDto
    ) throws FunksjonellException, TekniskException {
        validerBehandlingsgrunnlagPostData(behandlingsgrunnlagPostDto.getData());
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagPostDto.getData());
        return ResponseEntity.ok(new BehandlingsgrunnlagGetDto(behandlingsgrunnlag, hentTilleggsData(behandlingsgrunnlag.getBehandlingsgrunnlagdata())));
    }

    private BehandlingsgrunnlagTilleggsData hentTilleggsData(BehandlingsgrunnlagData behandlingsgrunnlagData)
        throws IkkeFunnetException, IntegrasjonException {
        return new BehandlingsgrunnlagTilleggsData(
            registerOppslagService.hentOrganisasjoner(behandlingsgrunnlagData.hentAlleOrganisasjonsnumre())
        );
    }

    private void validerBehandlingsgrunnlagPostData(JsonNode behandlingsgrunnlagPostData) throws TekniskException {
        try {
            behandlingsgrunnlagPostSchema.validate(new JSONObject(behandlingsgrunnlagPostData.toString()));
        } catch (ValidationException e) {
            logger.error("Ugyldig behandlingsgrunnlagData: {}", e.getCausingExceptions().stream()
                .map(ValidationException::getMessage).collect(Collectors.joining("\n")));
            throw new TekniskException("Ugyldig behandlingsgrunnlagData: " + e.getCausingExceptions().get(0).getMessage());
        }
    }
}

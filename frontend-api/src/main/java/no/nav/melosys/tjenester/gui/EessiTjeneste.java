package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.Vedlegg;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokumentVisningService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.eessi.*;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/eessi")
@Api(tags = {"eessi"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EessiTjeneste {
    private static final Logger log = LoggerFactory.getLogger(EessiTjeneste.class);

    private final EessiService eessiService;
    private final BehandlingService behandlingService;
    private final DokumentVisningService dokumentVisningService;

    @Autowired
    public EessiTjeneste(EessiService eessiService, BehandlingService behandlingService, DokumentVisningService dokumentVisningService) {
        this.eessiService = eessiService;
        this.behandlingService = behandlingService;
        this.dokumentVisningService = dokumentVisningService;
    }

    @GetMapping("/mottakerinstitusjoner/{bucType}")
    @ApiOperation(
        value = "Henter mottakerinstitusjoner for alle land for den oppgitte BUC-typen.",
        response = Institusjon.class,
        responseContainer = "List"
    )
    public ResponseEntity<List<Institusjon>> hentMottakerinstitusjoner(@PathVariable("bucType") String bucType,
                                                                       @RequestParam(value = "landkode", required = false) String landkode)
        throws MelosysException {
        log.info("Henter mottakerinstitusjoner for BUC {}", bucType);
        return ResponseEntity.ok(eessiService.hentEessiMottakerinstitusjoner(bucType, landkode));
    }

    @PostMapping("/bucer/{behandlingID}/opprett")
    @ApiOperation(
        value = "Oppretter en sak i RINA og sakens første tilgjengelige SED. Returnerer en URL til saken i RINA.",
        response = OpprettBucSvarDto.class
    )
    public ResponseEntity<OpprettBucSvarDto> opprettBuc(@RequestBody BucBestillingDto nyBucDto,
                                                        @PathVariable("behandlingID") long behandlingID)
        throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Collection<Vedlegg> vedlegg = lagVedlegg(behandling.getFagsak().getSaksnummer(), nyBucDto.getVedlegg());

        OpprettBucSvarDto opprettBucSvarDto = new OpprettBucSvarDto(
            eessiService.opprettBucOgSed(behandling, nyBucDto.getBucType(), List.of(nyBucDto.getMottakerId()), vedlegg)
        );

        return ResponseEntity.ok(opprettBucSvarDto);
    }

    private Collection<Vedlegg> lagVedlegg(String saksnummer, Collection<VedleggDto> vedleggDto) throws FunksjonellException, IntegrasjonException {
        Collection<Journalpost> journalposter = dokumentVisningService.hentDokumenter(saksnummer);

        Collection<Vedlegg> vedlegg = new ArrayList<>();
        for (VedleggDto dto : vedleggDto) {
            Journalpost journalpost = hentJournalpostForVedleggDto(dto, journalposter, saksnummer);
            vedlegg.add(lagVedlegg(journalpost, dto.getDokumentID()));
        }

        return vedlegg;
    }

    private Journalpost hentJournalpostForVedleggDto(VedleggDto vedleggDto, Collection<Journalpost> journalposter, String saksnummer) throws FunksjonellException {
        return journalposter.stream()
            .filter(journalpost -> journalpost.getJournalpostId().equals(vedleggDto.getJournalpostID()))
            .findFirst().orElseThrow(() ->
                new FunksjonellException(String.format(
                    "Journalpost %s er ikke knyttet til fagsak %s", vedleggDto.getJournalpostID(), saksnummer)));
    }

    private Vedlegg lagVedlegg(Journalpost journalpost, String dokumentID) throws FunksjonellException {
        final ArkivDokument arkivDokument = journalpost.hentArkivDokument(dokumentID);
        final byte[] pdf = dokumentVisningService.hentDokument(journalpost.getJournalpostId(), dokumentID);

        return new Vedlegg(pdf, arkivDokument.getTittel());
    }

    @GetMapping("/bucer/{behandlingID}")
    @ApiOperation(
        value = "Returnerer en liste av bucer for gjeldende behandling.",
        response = BucerTilknyttetBehandlingDto.class
    )
    public ResponseEntity<BucerTilknyttetBehandlingDto> hentBucer(@PathVariable("behandlingID") long behandlingID,
                                                                  @RequestParam(value = "statuser", required = false) List<String> statuser)
        throws MelosysException {

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        long gsakSaksnummer = behandling.getFagsak().getGsakSaksnummer();

        log.info("Henter tilknyttede bucer for sakID {}", gsakSaksnummer);
        BucerTilknyttetBehandlingDto bucerDto = new BucerTilknyttetBehandlingDto(
            eessiService.hentTilknyttedeBucer(gsakSaksnummer, statuser).stream()
                .map(BucInformasjonDto::av).collect(Collectors.toList())
        );
        return ResponseEntity.ok(bucerDto);
    }
}

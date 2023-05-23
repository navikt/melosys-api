package no.nav.melosys.service.vedtak.publisering;

import java.time.ZoneId;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import no.nav.melosys.service.vedtak.publisering.dto.Sak;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.time.LocalDate.ofInstant;

@Service
public class FattetVedtakService {

    private final FattetVedtakProducer fattetVedtakProducer;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;

    public FattetVedtakService(FattetVedtakProducer fattetVedtakProducer,
                               BehandlingService behandlingService,
                               PersondataFasade persondataFasade) {
        this.fattetVedtakProducer = fattetVedtakProducer;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
    }

    @Transactional
    public void publiserFattetVedtak(long behandlingId) throws IkkeFunnetException {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        fattetVedtakProducer.produserMelding(lagMelding(behandling));
    }

    private FattetVedtak lagMelding(Behandling behandling) throws IkkeFunnetException {
        final var persondata = hentPersondata(behandling);
        return new FattetVedtak(lagSak(behandling, behandling.getFagsak(), persondata));
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }

    private Sak lagSak(Behandling behandling, Fagsak fagsak, Persondata persondata) {
        return new Sak(persondata.hentFolkeregisterident(),
            behandling.getId(),
            fagsak.getSaksnummer(),
            fagsak.getType().getKode(),
            ofInstant(fagsak.getRegistrertDato(), ZoneId.systemDefault())
        );
    }
}

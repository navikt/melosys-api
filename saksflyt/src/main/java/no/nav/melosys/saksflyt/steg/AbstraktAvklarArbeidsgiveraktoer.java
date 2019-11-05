package no.nav.melosys.saksflyt.steg;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktAvklarArbeidsgiveraktoer extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AbstraktAvklarArbeidsgiveraktoer.class);

    private final AktoerService aktoerService;
    private final AvklarteVirksomheterService avklarteVirksomheterSystemService;
    private final BehandlingRepository behandlingRepository;

    public AbstraktAvklarArbeidsgiveraktoer(AktoerService aktoerService,
                                            AvklarteVirksomheterSystemService avklarteVirksomheterSystemService,
                                            BehandlingRepository behandlingRepository) {
        this.aktoerService = aktoerService;
        this.avklarteVirksomheterSystemService = avklarteVirksomheterSystemService;
        this.behandlingRepository = behandlingRepository;
    }

    private static final Function<OrganisasjonDokument, Adresse> INGEN_ADRESSE = org -> null;

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        List<AvklartVirksomhet> avklarteNorskeArbeidsgivere = avklarteVirksomheterSystemService.hentNorskeArbeidsgivere(behandling, INGEN_ADRESSE);
        List<String> norskeOrgnumre = avklarteNorskeArbeidsgivere.stream()
            .map(avklartVirksomhet -> avklartVirksomhet.orgnr)
            .collect(Collectors.toList());

        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, norskeOrgnumre);

        if (avklarteNorskeArbeidsgivere.isEmpty()) {
            log.info("Eksisterende arbeidsgiveraktør fjernet, og ingen nye lagt til for sak {}.", saksnummer);
        } else {
            log.info("Avklart arbeidsgivere lagt til for sak {}.", saksnummer);
        }
    }
}

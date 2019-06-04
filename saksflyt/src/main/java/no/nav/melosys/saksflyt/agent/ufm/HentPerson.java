package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HentPerson extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentPerson.class);

    private final TpsFasade tpsFasade;
    private final FagsakService fagsakService;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public HentPerson(@Qualifier("system") TpsFasade tpsFasade, FagsakService fagsakService, SaksopplysningRepository saksopplysningRepository) {
        this.tpsFasade = tpsFasade;
        this.fagsakService = fagsakService;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_PERSON;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String ident = tpsFasade.hentIdentForAktørId(aktørId);

        fagsakService.leggTilAktør(prosessinstans.getBehandling().getFagsak().getSaksnummer(), Aktoersroller.BRUKER, aktørId);

        prosessinstans.setData(ProsessDataKey.BRUKER_ID, ident);

        Instant nå = Instant.now();

        Saksopplysning saksopplysning = tpsFasade.hentPerson(ident);
        saksopplysning.setBehandling(prosessinstans.getBehandling());
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setEndretDato(nå);
        saksopplysningRepository.save(saksopplysning);

        log.info("Persondokument hentet for behandling {}", prosessinstans.getBehandling().getId());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT);
    }
}

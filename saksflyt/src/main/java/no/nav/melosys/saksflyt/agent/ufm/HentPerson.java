package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HentPerson extends AbstraktStegBehandler {

    private final TpsFasade tpsFasade;
    private final FagsakService fagsakService;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public HentPerson(TpsFasade tpsFasade, FagsakService fagsakService, SaksopplysningRepository saksopplysningRepository) {
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
    @Transactional(rollbackFor = MelosysException.class)
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

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

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT);
    }
}

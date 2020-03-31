package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.stereotype.Component;

@Component
public class OpprettBehandlingsgrunnlag extends AbstraktStegBehandler {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final OpprettSedDokumentFelles opprettSedDokumentFelles;

    public OpprettBehandlingsgrunnlag(BehandlingsgrunnlagService behandlingsgrunnlagService, OpprettSedDokumentFelles opprettSedDokumentFelles) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.opprettSedDokumentFelles = opprettSedDokumentFelles;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_OPPRETT_BEHANDLINGSGRUNNLAG;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        opprettSedDokumentFelles.opprettSedSaksopplysning(melosysEessiMelding, prosessinstans.getBehandling());

        //TODO: hent behandlingsgrunnlag fra melosys-eessi
        behandlingsgrunnlagService.opprettBehandlingsgrunnlag(prosessinstans.getBehandling().getId(), new BehandlingsgrunnlagData());
        prosessinstans.setSteg(ProsessSteg.AFL_REGISTERKONTROLL);
    }
}

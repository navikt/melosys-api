package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnntaksperiodeUnderAvklaring extends AbstraktStegBehandler {

    private final OppdaterMedlFelles felles;
    private final MedlFasade medlFasade;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public UnntaksperiodeUnderAvklaring(OppdaterMedlFelles felles, MedlFasade medlFasade, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.felles = felles;
        this.medlFasade = medlFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_UNDER_AVKLARING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        Behandling behandling = prosessinstans.getBehandling();
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Behandlingsresultat ikke funnet for behandling" + behandling.getId()));

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.isEmpty()) {
            SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(prosessinstans.getBehandling());

            Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
            lovvalgsperiode.setBestemmelse(sedDokument.getLovvalgBestemmelse());
            lovvalgsperiode.setFom(sedDokument.getLovvalgsperiode().getFom());
            lovvalgsperiode.setTom(sedDokument.getLovvalgsperiode().getTom());
            lovvalgsperiode.setLovvalgsland(sedDokument.getLovvalgslandKode());
            lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
            lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
            lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);
            Long medlperiodeId = medlFasade.opprettPeriodeUnderAvklaring(sedDokument.getFnr(), lovvalgsperiode, KildedokumenttypeMedl.SED);
            felles.lagreMedlPeriodeId(medlperiodeId, lovvalgsperiode, behandling.getId());
        }

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}

package no.nav.melosys.saksflyt.steg.aou.ut.svar;

import java.time.LocalDate;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettAnmodningsperiodeSvar implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettAnmodningsperiodeSvar.class);

    private final AnmodningsperiodeService anmodningsperiodeService;

    @Autowired
    public OpprettAnmodningsperiodeSvar(AnmodningsperiodeService anmodningsperiodeService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_ANMODNINGSPERIODESVAR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        AnmodningsperiodeSvar anmodningsperiodeSvar = opprettAnmodningsperiodeSvar(melosysEessiMelding.getSvarAnmodningUnntak());
        anmodningsperiodeService.lagreAnmodningsperiodeSvarForBehandling(prosessinstans.getBehandling().getId(), anmodningsperiodeSvar);
    }

    private AnmodningsperiodeSvar opprettAnmodningsperiodeSvar(SvarAnmodningUnntak svar) {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        Anmodningsperiodesvartyper svarType = hentSvarTypeFraBeslutning(svar.getBeslutning());

        anmodningsperiodeSvar.setRegistrertDato(LocalDate.now());
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(svarType);
        anmodningsperiodeSvar.setBegrunnelseFritekst(svar.getBegrunnelse());

        if (svarType == Anmodningsperiodesvartyper.DELVIS_INNVILGELSE) {
            anmodningsperiodeSvar.setInnvilgetFom(svar.getDelvisInnvilgetPeriode().getFom());
            anmodningsperiodeSvar.setInnvilgetTom(svar.getDelvisInnvilgetPeriode().getTom());
        }

        return anmodningsperiodeSvar;
    }

    private Anmodningsperiodesvartyper hentSvarTypeFraBeslutning(final SvarAnmodningUnntak.Beslutning beslutning) {
        switch (beslutning) {
            case INNVILGELSE:
                return Anmodningsperiodesvartyper.INNVILGELSE;
            case DELVIS_INNVILGELSE:
                return Anmodningsperiodesvartyper.DELVIS_INNVILGELSE;
            case AVSLAG:
                return Anmodningsperiodesvartyper.AVSLAG;
            default:
                throw new IllegalArgumentException("Ukjent beslutning-kode mottatt: " + beslutning);
        }
    }
}

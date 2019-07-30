package no.nav.melosys.saksflyt.steg.aou.svar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kafka.model.SvarAnmodningUnntak;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettAnmodningsperiodeSvar extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettAnmodningsperiodeSvar.class);

    private final AnmodningsperiodeService anmodningsperiodeService;

    @Autowired
    public OpprettAnmodningsperiodeSvar(AnmodningsperiodeService anmodningsperiodeService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_SVAR_OPPRETT_ANMODNINGSPERIODESVAR;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (prosessinstans.getType() != ProsessType.ANMODNING_OM_UNNTAK_SVAR) {
            throw new TekniskException("Prosessinstans ikke av type" + ProsessType.ANMODNING_OM_UNNTAK_SVAR);
        }

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        AnmodningsperiodeSvar anmodningsperiodeSvar = opprettAnmodningsperiodeSvar(melosysEessiMelding.getSvarAnmodningUnntak());

        anmodningsperiodeService.lagreAnmodningsperiodeSvarForBehandling(prosessinstans.getBehandling().getId(), anmodningsperiodeSvar);

        prosessinstans.setSteg(ProsessSteg.AOU_SVAR_OPPDATER_BEHANDLING);
    }

    private AnmodningsperiodeSvar opprettAnmodningsperiodeSvar(SvarAnmodningUnntak svar) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        AnmodningsperiodeSvarType svarType = hentSvarTypeFraBeslutning(svar.getBeslutning());

        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(svarType);
        anmodningsperiodeSvar.setBegrunnelseFritekst(svar.getBegrunnelse());

        if (svarType == AnmodningsperiodeSvarType.DELVIS_INNVILGELSE) {
            anmodningsperiodeSvar.setInnvilgetFom(LocalDate.parse(svar.getDelvisInnvilgetPeriode().getFom(), dateTimeFormatter));
            anmodningsperiodeSvar.setInnvilgetTom(LocalDate.parse(svar.getDelvisInnvilgetPeriode().getTom(), dateTimeFormatter));
        }

        return anmodningsperiodeSvar;
    }

    private AnmodningsperiodeSvarType hentSvarTypeFraBeslutning(final SvarAnmodningUnntak.Beslutning beslutning) {
        switch (beslutning) {
            case INNVILGELSE:
                return AnmodningsperiodeSvarType.INNVILGELSE;
            case DELVIS_INNVILGELSE:
                return AnmodningsperiodeSvarType.DELVIS_INNVILGELSE;
            case AVSLAG:
                return AnmodningsperiodeSvarType.AVSLAG;
            default:
                throw new IllegalArgumentException("Ukjent beslutning-kode mottatt: " + beslutning);
        }
    }
}

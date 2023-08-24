package no.nav.melosys.service.medlemskapsperiode;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadFtrl;

import java.time.LocalDate;
import java.util.Collection;

public class UtledMedlemskapsperiodeNyVurderingRequest extends UtledMedlemskapsperioderRequest {

    private final Collection<Medlemskapsperiode> opprinneligeMedlemskapsperioder;
    private final SoeknadFtrl opprinneligSøknad;

    public UtledMedlemskapsperiodeNyVurderingRequest(ErPeriode søknadsperiode,
                                                     Trygdedekninger trygdedekning,
                                                     LocalDate mottaksdatoSøknad,
                                                     String arbeidsland,
                                                     Collection<Medlemskapsperiode> opprinneligeMedlemskapsperioder,
                                                     SoeknadFtrl opprinneligSøknad) {
        super(søknadsperiode, trygdedekning, mottaksdatoSøknad, arbeidsland);
        this.opprinneligeMedlemskapsperioder = opprinneligeMedlemskapsperioder;
        this.opprinneligSøknad = opprinneligSøknad;
    }

    public Collection<Medlemskapsperiode> getOpprinneligeMedlemskapsperioder() {
        return opprinneligeMedlemskapsperioder;
    }

    public SoeknadFtrl getOpprinneligSøknad() {
        return opprinneligSøknad;
    }
}

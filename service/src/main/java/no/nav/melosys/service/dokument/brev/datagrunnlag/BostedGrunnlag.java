package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.time.LocalDate;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SoeknadUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class BostedGrunnlag {
    private final KodeverkService kodeverkService;

    private final SoeknadDokument søknad;
    private final PersonDokument person;

    public BostedGrunnlag(SoeknadDokument søknad,
                          PersonDokument person,
                          KodeverkService kodeverkService) {
        this.søknad = søknad;
        this.person = person;
        this.kodeverkService = kodeverkService;
    }

    public StrukturertAdresse hentBostedsadresse() throws TekniskException {
        StrukturertAdresse bostedsadresse = SoeknadUtils.hentBostedsadresse(søknad);
        if (bostedsadresse == null) {
            bostedsadresse = hentBostedsadresseFraRegister();
        }
        return bostedsadresse;
    }

    private StrukturertAdresse hentBostedsadresseFraRegister() throws TekniskException {
        Bostedsadresse bostedsadresse = person.bostedsadresse;
        if (StringUtils.isEmpty(bostedsadresse.getLand().getKode())) {
            throw new TekniskException("Bostedsadressen finnes ikke eller mangler landkode");
        }
        bostedsadresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, bostedsadresse.getPostnr(), LocalDate.now()));
        return StrukturertAdresse.av(bostedsadresse);
    }
}

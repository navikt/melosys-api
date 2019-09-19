package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;

public class ArbeidsstedGrunnlag {
    private final Behandling behandling;
    private final SoeknadDokument søknad;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final AvklartefaktaService avklartefaktaService;

    public ArbeidsstedGrunnlag(Behandling behandling, SoeknadDokument søknad,
                               AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag,
                               AvklartefaktaService avklartefaktaService) {
        this.behandling = behandling;
        this.søknad = søknad;
        this.avklarteVirksomheterGrunnlag = avklarteVirksomheterGrunnlag;
        this.avklartefaktaService = avklartefaktaService;
    }

    public List<Arbeidssted> hentArbeidssteder() throws TekniskException {
        List<Arbeidssted> arbeidssteder = hentFysiskearbeidssteder();
        arbeidssteder.addAll(hentMaritimeArbeidssteder());
        return arbeidssteder;
    }

    private List<Arbeidssted> hentFysiskearbeidssteder() throws TekniskException {
        List<Arbeidssted> fysiskeArbeidssteder = søknad.arbeidUtland.stream()
            .map(au -> new FysiskArbeidssted(au.foretakNavn, au.foretakOrgnr, au.adresse))
            .collect(Collectors.toList());

        if (fysiskeArbeidssteder.isEmpty()) {
            avklarteVirksomheterGrunnlag.hentUtenlandskeVirksomheter().stream()
                .filter(uv -> uv.adresseErOgsåArbeidssted)
                .forEach(uv -> fysiskeArbeidssteder.add(utledArbeidsstedFraVirksomhet(uv)));
        }
        return fysiskeArbeidssteder;
    }

    private List<MaritimtArbeidssted> hentMaritimeArbeidssteder() {
        Map<String, AvklartMaritimtArbeid> avklartMaritimtArbeid =
            avklartefaktaService.hentAlleMaritimeAvklartfakta(behandling.getId());

        // Arbeidssted for maritimt arbeid benytter foretakNavn og foretakOrgnr fra søknad, og arbeidsland fra avklartfakta
        return søknad.maritimtArbeid.stream()
            .map(ma -> lagMaritimtArbeidssted(ma, avklartMaritimtArbeid))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private MaritimtArbeidssted lagMaritimtArbeidssted(MaritimtArbeid maritimtArbeid, Map<String, AvklartMaritimtArbeid> alleAvklarteMaritimeArbeid) {
        AvklartMaritimtArbeid avklartMaritimtArbeid = alleAvklarteMaritimeArbeid.get(maritimtArbeid.enhetNavn);
        if (avklartMaritimtArbeid != null) {
            return new MaritimtArbeidssted(maritimtArbeid, avklartMaritimtArbeid);
        }
        return null;
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(AvklartVirksomhet virksomhet) {
        return new FysiskArbeidssted(virksomhet.navn, virksomhet.orgnr, (StrukturertAdresse)virksomhet.adresse);
    }
}

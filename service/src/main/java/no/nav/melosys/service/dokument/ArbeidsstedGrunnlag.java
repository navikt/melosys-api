package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.dokument.brev.datagrunnlag.AvklarteVirksomheterGrunnlag;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FlyvendeArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;

public class ArbeidsstedGrunnlag {
    private final Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeidEtterSubjekt;
    private final AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag;
    private final BehandlingsgrunnlagData grunnlagData;

    public ArbeidsstedGrunnlag(Map<String, AvklartMaritimtArbeid> avklarteMaritimeArbeid,
                        AvklarteVirksomheterGrunnlag avklarteVirksomheterGrunnlag,
                        BehandlingsgrunnlagData grunnlagData) {
        this.avklarteMaritimeArbeidEtterSubjekt = avklarteMaritimeArbeid;
        this.avklarteVirksomheterGrunnlag = avklarteVirksomheterGrunnlag;
        this.grunnlagData = grunnlagData;
    }

    public List<Arbeidssted> hentArbeidssteder() {
        List<Arbeidssted> arbeidssteder = hentFysiskearbeidssteder();
        arbeidssteder.addAll(hentMaritimeArbeidssteder());
        arbeidssteder.addAll(hentFlyvendeArbeidssteder());
        return arbeidssteder;
    }

    private List<Arbeidssted> hentFysiskearbeidssteder() {
        List<Arbeidssted> fysiskeArbeidssteder = grunnlagData.arbeidUtland.stream()
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
        // Arbeidssted for maritimt arbeid benytter foretakNavn og foretakOrgnr fra søknad, og arbeidsland fra avklartfakta
        return grunnlagData.maritimtArbeid.stream()
            .map(this::lagMaritimtArbeidssted)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<FlyvendeArbeidssted> hentFlyvendeArbeidssteder() {
        return grunnlagData.luftfartBaser.stream()
            .map(FlyvendeArbeidssted::new)
            .collect(Collectors.toList());
    }

    private MaritimtArbeidssted lagMaritimtArbeidssted(MaritimtArbeid maritimtArbeid) {
        AvklartMaritimtArbeid avklartMaritimtArbeid = avklarteMaritimeArbeidEtterSubjekt.get(maritimtArbeid.enhetNavn);
        if (avklartMaritimtArbeid != null) {
            return new MaritimtArbeidssted(maritimtArbeid, avklartMaritimtArbeid);
        }
        return null;
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(AvklartVirksomhet virksomhet) {
        return new FysiskArbeidssted(virksomhet.navn, virksomhet.orgnr, (StrukturertAdresse)virksomhet.adresse);
    }
}

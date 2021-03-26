package no.nav.melosys.domain.avgift;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;

public class Trygdeavgiftsgrunnlag extends AbstraktAvgiftsgrunnlag<AvgiftsgrunnlagInfoNorge, AvgiftsgrunnlagInfoUtland> {

    public Trygdeavgiftsgrunnlag(Loenn_forhold lønnsforhold, AvgiftsgrunnlagInfoNorge avgiftsGrunnlagNorge, AvgiftsgrunnlagInfoUtland avgiftsGrunnlagUtland) {
        super(lønnsforhold, avgiftsGrunnlagNorge, avgiftsGrunnlagUtland);
    }

    public static Trygdeavgiftsgrunnlag av(Behandlingsresultat behandlingsresultat) {
        final var avklarteFakta = behandlingsresultat.getAvklartefakta();
        final var lønnsforhold = finnLønnsforholdFakta(avklarteFakta);
        return new Trygdeavgiftsgrunnlag(
            lønnsforhold,
            harLønnsforholdINorge(lønnsforhold) ? lagAvgiftsgrunnlagNorge(avklarteFakta, behandlingsresultat.getMedlemAvFolketrygden()) : null,
            harLønnsforholdIUtlandet(lønnsforhold) ? lagAvgiftsgrunnlagUtland(avklarteFakta, behandlingsresultat.getMedlemAvFolketrygden()) : null
        );
    }

    private static AvgiftsgrunnlagInfoNorge lagAvgiftsgrunnlagNorge(Collection<Avklartefakta> avklartefakta, MedlemAvFolketrygden medlemAvFolketrygden) {
        return new AvgiftsgrunnlagInfoNorge(
            finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_NORGE_SKATTEPLIKTIG_NORGE).filter(Avklartefakta::erValgtFakta).isPresent(),
            finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_NORGE_ARBEIDSGIVERAVGIFT).filter(Avklartefakta::erValgtFakta).isPresent(),
            finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE).filter(Avklartefakta::erValgtFakta)
                .map(Avklartefakta::getSubjekt).map(Saerligeavgiftsgrupper::valueOf).orElse(null),
            Optional.ofNullable(medlemAvFolketrygden).map(MedlemAvFolketrygden::getVurderingTrygdeavgiftNorskInntekt).orElse(null)
        );
    }

    private static AvgiftsgrunnlagInfoUtland lagAvgiftsgrunnlagUtland(Collection<Avklartefakta> avklartefakta, MedlemAvFolketrygden medlemAvFolketrygden) {
        return new AvgiftsgrunnlagInfoUtland(
            finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_UTL_SKATTEPLIKTIG_NORGE).filter(Avklartefakta::erValgtFakta).isPresent(),
            finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_UTL_ARBEIDSGIVERAVGIFT).filter(Avklartefakta::erValgtFakta).isPresent(),
            finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE).filter(Avklartefakta::erValgtFakta)
                .map(Avklartefakta::getSubjekt).map(Saerligeavgiftsgrupper::valueOf).orElse(null),
            Optional.ofNullable(medlemAvFolketrygden).map(MedlemAvFolketrygden::getVurderingTrygdeavgiftUtenlandskInntekt).orElse(null)
        );
    }

    private static Loenn_forhold finnLønnsforholdFakta(Collection<Avklartefakta> avklartefakta) {
        return finnAvklartefakta(avklartefakta, Avklartefaktatyper.LØNN_FORHOLD_VIRKSOMHET)
            .map(Avklartefakta::getFakta)
            .map(Loenn_forhold::valueOf)
            .orElse(null);
    }

    private static Optional<Avklartefakta> finnAvklartefakta(Collection<Avklartefakta> avklartefakta, Avklartefaktatyper avklartefaktatype) {
        return avklartefakta.stream().filter(a -> a.getType() == avklartefaktatype).findFirst();
    }
}

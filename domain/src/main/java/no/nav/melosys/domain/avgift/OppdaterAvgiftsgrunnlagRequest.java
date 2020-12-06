package no.nav.melosys.domain.avgift;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.avklartefakta.Avklartefakta.IKKE_VALGT_FAKTA;
import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;

public class OppdaterAvgiftsgrunnlagRequest extends AbstraktAvgiftsgrunnlag<AvgiftsgrunnlagInfo, AvgiftsgrunnlagInfo> {

    public OppdaterAvgiftsgrunnlagRequest(Loenn_forhold lønnsforhold,
                                          AvgiftsgrunnlagInfo avgiftsGrunnlagNorge,
                                          AvgiftsgrunnlagInfo avgiftsGrunnlagUtland) {
        super(lønnsforhold, avgiftsGrunnlagNorge, avgiftsGrunnlagUtland);
    }

    public Collection<Avklartefakta> tilAvklartefakta() throws FunksjonellException {
        if (getLønnsforhold() == null) {
            throw new FunksjonellException("Lønnsforhold er ikke satt!");
        }

        Set<Avklartefakta> avklartefakta = new HashSet<>();
        avklartefakta.add(lagLønnsforhold());

        if (getLønnsforhold() == Loenn_forhold.LØNN_FRA_NORGE) {
            avklartefakta.addAll(lagAvgiftsgrunnlagNorge());
        } else if (getLønnsforhold() == Loenn_forhold.LØNN_FRA_UTLANDET) {
            avklartefakta.addAll(lagAvgiftsgrunnlagUtland());
        } else {
            avklartefakta.addAll(lagAvgiftsgrunnlagNorge());
            avklartefakta.addAll(lagAvgiftsgrunnlagUtland());
        }

        return avklartefakta;
    }

    private Collection<Avklartefakta> lagAvgiftsgrunnlagNorge() {
        return Set.of(
            lag(Avklartefaktatyper.LØNN_NORGE_ARBEIDSGIVERAVGIFT, avgiftsGrunnlagNorge::getBetalerArbeidsgiverAvgift),
            lag(Avklartefaktatyper.LØNN_NORGE_SKATTEPLIKTIG_NORGE, avgiftsGrunnlagNorge::getErSkattepliktig),
            lag(Avklartefaktatyper.LØNN_NORGE_SÆRLIG_AVGIFTS_GRUPPE, avgiftsGrunnlagNorge::getSærligAvgiftsgruppe)
        );
    }

    private Collection<Avklartefakta> lagAvgiftsgrunnlagUtland() {
        return Set.of(
            lag(Avklartefaktatyper.LØNN_UTL_ARBEIDSGIVERAVGIFT, avgiftsGrunnlagUtland::getBetalerArbeidsgiverAvgift),
            lag(Avklartefaktatyper.LØNN_UTL_SKATTEPLIKTIG_NORGE, avgiftsGrunnlagUtland::getErSkattepliktig),
            lag(Avklartefaktatyper.LØNN_UTL_SÆRLIG_AVGIFTS_GRUPPE, avgiftsGrunnlagUtland::getSærligAvgiftsgruppe)
        );
    }

    private Avklartefakta lagLønnsforhold() {
        return lag(Avklartefaktatyper.LØNN_FORHOLD_VIRKSOMHET, lønnsforhold.getKode());
    }

    private Avklartefakta lag(Avklartefaktatyper type, Supplier<Saerligeavgiftsgrupper> saerligeavgiftsgruppe) {
        return lag(type, tilFakta(saerligeavgiftsgruppe.get() != null), saerligeavgiftsgruppe.get() != null ? saerligeavgiftsgruppe.get().getKode() : null);
    }

    private Avklartefakta lag(Avklartefaktatyper type, BooleanSupplier fakta) {
        return lag(type, tilFakta(fakta.getAsBoolean()));
    }

    private Avklartefakta lag(Avklartefaktatyper type, String fakta) {
        return lag(type, fakta, null);
    }

    private Avklartefakta lag(Avklartefaktatyper type, String fakta, String subjekt) {
        var avklartefakta = new Avklartefakta();
        avklartefakta.setType(type);
        avklartefakta.setFakta(fakta);
        avklartefakta.setSubjekt(subjekt);
        return avklartefakta;
    }

    private String tilFakta(Boolean fakta) {
        return Boolean.TRUE.equals(fakta) ? VALGT_FAKTA : IKKE_VALGT_FAKTA;
    }

}

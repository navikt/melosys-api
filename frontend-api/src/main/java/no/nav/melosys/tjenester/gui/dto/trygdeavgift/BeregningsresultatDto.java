package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;

public class BeregningsresultatDto extends OppdaterBeregningsgrunnlagDto {

    private final Collection<AvgiftsperiodeDto> avgiftsperioderNorge;
    private final Collection<AvgiftsperiodeDto> avgiftsperioderUtland;

    public BeregningsresultatDto(Long avgiftspliktigLønnNorge,
                                 Long avgiftspliktigLønnUtland,
                                 Collection<AvgiftsperiodeDto> avgiftsperioderNorge,
                                 Collection<AvgiftsperiodeDto> avgiftsperioderUtland) {
        super(avgiftspliktigLønnNorge, avgiftspliktigLønnUtland);
        this.avgiftsperioderNorge = avgiftsperioderNorge;
        this.avgiftsperioderUtland = avgiftsperioderUtland;
    }

    public Collection<AvgiftsperiodeDto> getAvgiftsperioderNorge() {
        return avgiftsperioderNorge;
    }

    public Collection<AvgiftsperiodeDto> getAvgiftsperioderUtland() {
        return avgiftsperioderUtland;
    }

    public static BeregningsresultatDto av(Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat) {
        final Collection<AvgiftsperiodeDto> avgiftsperioderNorge = new ArrayList<>();
        final Collection<AvgiftsperiodeDto> avgiftsperioderUtland = new ArrayList<>();
        trygdeavgiftsberegningsresultat.getAvgiftsperioder()
            .forEach(a -> {
                if (a.isForNorskInntekt()) {
                    avgiftsperioderNorge.add(AvgiftsperiodeDto.av(a));
                } else {
                    avgiftsperioderUtland.add(AvgiftsperiodeDto.av(a));
                }
            }
        );

        return new BeregningsresultatDto(
            trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge(),
            trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland(),
            avgiftsperioderNorge,
            avgiftsperioderUtland
        );
    }
}

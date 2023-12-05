package no.nav.melosys.tjenester.gui.dto;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto;
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.VirksomheterDto;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING;

public class AvklartefaktaOppsummeringDto {

    private VirksomheterDto virksomheter;
    private ArbeidslandDto arbeidsland;
    private Boolean fullstendigManglendeInnbetaling;

    public VirksomheterDto getVirksomheter() {
        return virksomheter;
    }

    public void setVirksomheter(VirksomheterDto virksomheter) {
        this.virksomheter = virksomheter;
    }

    public static AvklartefaktaOppsummeringDto av(Set<AvklartefaktaDto> avklartefakta) {
        AvklartefaktaOppsummeringDto avklartefaktaOppsummeringDto = new AvklartefaktaOppsummeringDto();
        avklartefaktaOppsummeringDto.setVirksomheter(VirksomheterDto.av(avklartefakta));
        avklartefaktaOppsummeringDto.setArbeidsland(ArbeidslandDto.av(avklartefakta));
        avklartefaktaOppsummeringDto.setFullstendigManglendeInnbetaling(hentFullstendigManglendeInnbetaling(avklartefakta));
        return avklartefaktaOppsummeringDto;
    }

    public static Boolean hentFullstendigManglendeInnbetaling(Set<AvklartefaktaDto> avklartefaktas) {
        Optional<AvklartefaktaDto> avklartefaktaDto = avklartefaktas.stream()
            .filter(avklartefakta -> FULLSTENDIG_MANGLENDE_INNBETALING.getKode().equals(avklartefakta.getReferanse())
                && FULLSTENDIG_MANGLENDE_INNBETALING.equals(avklartefakta.getAvklartefaktaType()))
            .limit(1)
            .findFirst();
        Boolean fullstendigManglendeInnbetaling = null;
        if (avklartefaktaDto.isPresent()) {
            fullstendigManglendeInnbetaling = Boolean.parseBoolean(avklartefaktaDto.get().getFakta().get(0));
        }
        return fullstendigManglendeInnbetaling;
    }

    public ArbeidslandDto getArbeidsland() {
        return arbeidsland;
    }

    public void setArbeidsland(ArbeidslandDto arbeidsland) {
        this.arbeidsland = arbeidsland;
    }

    public Boolean getFullstendigManglendeInnbetaling() {
        return fullstendigManglendeInnbetaling;
    }

    public void setFullstendigManglendeInnbetaling(Boolean fullstendigManglendeInnbetaling) {
        this.fullstendigManglendeInnbetaling = fullstendigManglendeInnbetaling;
    }
}

package no.nav.melosys.integrasjon.eessi.dto;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class VedleggDto {

    private final byte[] innhold;
    private final String tittel;

    public VedleggDto(byte[] innhold, String tittel) {
        this.innhold = innhold;
        this.tittel = tittel;
    }

    public byte[] getInnhold() {
        return innhold;
    }

    public String getTittel() {
        return tittel;
    }

    public boolean erGyldig() {
        return ArrayUtils.isNotEmpty(innhold) && StringUtils.isNotEmpty(tittel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VedleggDto that = (VedleggDto) o;
        return Arrays.equals(innhold, that.innhold) &&
            Objects.equals(tittel, that.tittel);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(tittel);
        result = 31 * result + Arrays.hashCode(innhold);
        return result;
    }
}

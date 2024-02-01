package no.nav.melosys.tjenester.gui.dto.brev

/**
 * FeltValgDto inneholder en liste over {@param valgAlternativer} brukeren må foreta. Feltet som bruker disse valgene
 * vil være usynlig med mindre brukeren velger et alternativ som har [FeltvalgAlternativDto.isVisFelt] = true.
 */
class FeltValgDto(@JvmField var valgAlternativer: List<FeltvalgAlternativDto>, @JvmField var valgType: FeltValgType)

ALTER TABLE inntektsperiode ADD er_maanedsbelop number(1);

ALTER TABLE inntektsperiode RENAME COLUMN avgiftspliktig_inntekt_mnd_verdi TO avgiftspliktig_inntekt_verdi;

ALTER TABLE inntektsperiode RENAME COLUMN avgiftspliktig_inntekt_mnd_valuta TO avgiftspliktig_inntekt_valuta;

UPDATE inntektsperiode
SET er_maanedsbelop = 1;

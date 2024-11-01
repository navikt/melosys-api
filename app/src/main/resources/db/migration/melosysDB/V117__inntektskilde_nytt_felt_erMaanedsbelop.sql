ALTER TABLE inntektsperiode ADD er_maanedsbelop number(1);
ALTER TABLE inntektsperiode ADD avgiftspliktig_inntekt_total_verdi DECIMAL(12, 2);
ALTER TABLE inntektsperiode ADD avgiftspliktig_inntekt_total_valuta VARCHAR(3);

UPDATE inntektsperiode
SET er_maanedsbelop = 1;

ALTER TABLE medlemskapsperiode
    ADD bestemmelse VARCHAR2(99);

UPDATE medlemskapsperiode
SET bestemmelse = (SELECT bestemmelse
                   FROM medlem_av_folketrygden
                   WHERE medlemskapsperiode.medlem_av_folketrygden_id = medlem_av_folketrygden.id);

ALTER TABLE medlemskapsperiode
    MODIFY bestemmelse VARCHAR2(99) NOT NULL;

ALTER TABLE medlem_av_folketrygden
    DROP COLUMN bestemmelse;

CREATE TABLE tidligere_medlemsperiode (
    behandling_id   NUMBER(19) NOT NULL,
    periode_id      NUMBER(19) NOT NULL,
    CONSTRAINT medlemsperiode_behandling_pk PRIMARY KEY (behandling_id, periode_id),
    CONSTRAINT medlemsperiode_behandling_fk FOREIGN KEY (behandling_id) REFERENCES behandling
);

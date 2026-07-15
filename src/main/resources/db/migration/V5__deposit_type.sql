CREATE TABLE deposit_type(
    id    SERIAL PRIMARY KEY,
    name  VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO deposit_type(name) VALUES ('CASH'), ('CHECK');

ALTER TABLE transactions ADD COLUMN deposit_type_id INTEGER REFERENCES deposit_type(id);
ALTER TABLE transactions ADD COLUMN items_count INTEGER;
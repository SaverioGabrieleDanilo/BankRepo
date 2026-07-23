UPDATE transaction_type SET name = 'DEPOSIT' WHERE name = 'VERSAMENTO';
UPDATE transaction_type SET name = 'WITHDRAWAL' WHERE name = 'PRELIEVO';
UPDATE transaction_type SET name = 'BANK_TRANSFER' WHERE name = 'BONIFICO';
UPDATE transaction_type SET name = 'INTERNAL_TRANSFER' WHERE name = 'GIROCONTO';

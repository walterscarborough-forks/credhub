DROP FUNCTION IF EXISTS uuid_of;

CREATE FUNCTION uuid_of(uuid BINARY(16))
  RETURNS VARCHAR(36)
  RETURN LOWER(CONCAT(
    SUBSTR(HEX(uuid), 1, 8), '-',
    SUBSTR(HEX(uuid), 9, 4), '-',
    SUBSTR(HEX(uuid), 13, 4), '-',
    SUBSTR(HEX(uuid), 17, 4), '-',
    SUBSTR(HEX(uuid), 21)
  ));

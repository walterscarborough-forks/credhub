DROP FUNCTION IF EXISTS uuid_of(uuid);

CREATE FUNCTION uuid_of(uuid uuid)
  RETURNS uuid AS
$func$
BEGIN
  RETURN uuid;
END;
$func$

  LANGUAGE plpgsql;

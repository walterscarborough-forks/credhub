package io.pivotal.security.helper;

public class TestConstants {
  // generate with ./build/credhub n -t ssh -n foo -k 4096
  public static String PUBLIC_KEY_OF_LENGTH_4096 = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDs8lQIdJ+tnc9jufX9wLzCPVS1utoTJwzQO2NS5F07OEWXnR94XtYY3KtBLu10LHjZzH5maxdWYkyb4GgYSwV+6ln+Txn79LQT8gStbK+mJBFWnGplHNU+loHdHkKckOVihBgDfjsW58s46X9HmKAiUetXBazQX2pVOhOKBETgEstVKB1CoN0fP98mbergW+THHxDpbtodep1EoWZePn/Qe/jly7joL8HZuVAwzunmBsrrm0B1cRF3mG4/XZDdHqbz1humoz/8V8KMBuC899XhN1yZvmdZqe3OhpENr8O3e26p7xxTyCyOs5kk2Myv7YqWOyr43obFIzGUcLLMj3p1SDukgzpxCHPmiZ72zO/hZ+HkB6319iZPsZgrR8vapQsJY5MfYJO9KPj0BKlFdi9y578VCj1pw6OYz7fuRrSfu/W0S1l9FLI450aFsNSji5ZX7elJ5A0qDQaFblECAsmbMjT9MCDyJDjZfmtb9UY4j/ywFeYP26RLqbdWMZBYgukVg+isCyxJczecaJKRWBnUryz5sSvbsOC38rdu7LAl/vxf8m2ZY6d/TZ2SgTEDgD4YxOG6WZEm2z2JGpgGtQcVO4ulfSa/xqovvidLc/kTWR15dVts+r1Uv7Btaax7XqTKqBkrxjhbpXD2RVQAeZhBOQ80pPbFtvUPN1pAdgc14w==";
  public static String PUBLIC_KEY_OF_LENGTH_4096_WITH_COMMENT = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDs8lQIdJ+tnc9jufX9wLzCPVS1utoTJwzQO2NS5F07OEWXnR94XtYY3KtBLu10LHjZzH5maxdWYkyb4GgYSwV+6ln+Txn79LQT8gStbK+mJBFWnGplHNU+loHdHkKckOVihBgDfjsW58s46X9HmKAiUetXBazQX2pVOhOKBETgEstVKB1CoN0fP98mbergW+THHxDpbtodep1EoWZePn/Qe/jly7joL8HZuVAwzunmBsrrm0B1cRF3mG4/XZDdHqbz1humoz/8V8KMBuC899XhN1yZvmdZqe3OhpENr8O3e26p7xxTyCyOs5kk2Myv7YqWOyr43obFIzGUcLLMj3p1SDukgzpxCHPmiZ72zO/hZ+HkB6319iZPsZgrR8vapQsJY5MfYJO9KPj0BKlFdi9y578VCj1pw6OYz7fuRrSfu/W0S1l9FLI450aFsNSji5ZX7elJ5A0qDQaFblECAsmbMjT9MCDyJDjZfmtb9UY4j/ywFeYP26RLqbdWMZBYgukVg+isCyxJczecaJKRWBnUryz5sSvbsOC38rdu7LAl/vxf8m2ZY6d/TZ2SgTEDgD4YxOG6WZEm2z2JGpgGtQcVO4ulfSa/xqovvidLc/kTWR15dVts+r1Uv7Btaax7XqTKqBkrxjhbpXD2RVQAeZhBOQ80pPbFtvUPN1pAdgc14w== dan@foo";
  public static String PRIVATE_KEY_OF_LENGTH_4096 = "-----BEGIN RSA PRIVATE KEY-----\n" +
    "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\n" +
    "-----END RSA PRIVATE KEY-----";
}

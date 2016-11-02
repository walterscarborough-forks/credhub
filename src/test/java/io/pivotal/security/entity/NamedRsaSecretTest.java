package io.pivotal.security.entity;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.CredentialManagerTestContextBootstrapper;
import io.pivotal.security.data.SecretDataService;
import io.pivotal.security.fake.FakeEncryptionService;
import io.pivotal.security.service.EncryptionService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.BootstrapWith;

import java.time.Instant;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

@RunWith(Spectrum.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@BootstrapWith(CredentialManagerTestContextBootstrapper.class)
@ActiveProfiles({"unit-test", "FakeEncryptionService"})
public class NamedRsaSecretTest {
  @Autowired
  SecretDataService secretDataService;

  @Autowired
  EncryptionService encryptionService;

  private NamedRsaSecret subject;

  {
    wireAndUnwire(this);

    beforeEach(() -> {
      subject = new NamedRsaSecret("Foo");
      ((FakeEncryptionService) encryptionService).resetEncryptionCount();
    });

    it("returns type rsa", () -> {
      assertThat(subject.getSecretType(), equalTo("rsa"));
    });

    it("sets a public key", () -> {
      subject
          .setPublicKey("my-public-key");
      secretDataService.save(subject);
      NamedRsaSecret result = (NamedRsaSecret) secretDataService.findByUuid(subject.getUuid());
      assertThat(result.getPublicKey(), equalTo("my-public-key"));
    });

    it("sets an encrypted private key", () -> {
      subject
          .setPrivateKey("some-private-value");
      secretDataService.save(subject);

      NamedRsaSecret result = (NamedRsaSecret) secretDataService.findByUuid(subject.getUuid());

      assertThat(result.getPrivateKey(), equalTo("some-private-value"));
    });

    it("updates the private key value with the same name when overwritten", () -> {
      subject.setPrivateKey("first");
      subject = (NamedRsaSecret) secretDataService.save(subject);

      subject.setPrivateKey("second");
      subject = (NamedRsaSecret) secretDataService.save(subject);

      NamedRsaSecret result = (NamedRsaSecret) secretDataService.findByUuid(subject.getUuid());
      assertThat(result.getPrivateKey(), equalTo("second"));
    });

    describe("#getKeyLength", () -> {
      it("should return the length of the public key", () -> {
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAoRIqdibiYHKZhyH91xYR\n" +
            "Tpz728+A8d/t2U2e8OIhNqI7pjh5uKnbmeoAXdZAbGN3TW7xArdMAUOSRhELH0Gc\n" +
            "8XGz6ZnY+KGuTnmBO+ZamE3kltwqJBfxwV2UGV5bJIVVToLpLa1GDF4p7g8I8W/a\n" +
            "KURKCgMNlRQw38Wi8yuEWyCHWHrqon8CcA5ovUg1pyrpR9i+5NTCRadDf1JIQfKB\n" +
            "Mt/gA/s6+ZaWOB6mbWv67OUS5wHWe0tcX2g4KK3IDlkzKQulSHQoIPEf+7l+vJEJ\n" +
            "KT+C2cI+pl/qLVtbY+jsNr8acxja0ri4pUGEQPKP5009qisloEDlQMb/gMT5aHoF\n" +
            "8GORc1EloUG4CpnPUe0L63Q3uSZkLSPAiYqwCi7Wu/L7aVeynGk3CFIPALyh/hIi\n" +
            "SCOX6Jc81o9hZLADEFx4o4qaK4/MQczLaPkESO2578MI+eNwV3d02CIaUeSzK91b\n" +
            "ZlAsqUUXaxxOQ+0WcJpE1O+IUXoBJ7XSZAqfdogLVUM0A+wW8Duxthuh1j7z284B\n" +
            "NjWi9nPZnD3KT0vLv8KbwrW0XgiMzsaAdZKlexKZQuuzAOVNHb0hd3H36lBqAOPg\n" +
            "G0S+H7L3o8XAPcqkke2xs/tcfF05DX+kpD2xdeDWs9MK39FnGtYp8gTKoDkzf0vp\n" +
            "o2oUFe5cAKZHziOqNuoc7SUCAwEAAQ==\n" +
            "-----END PUBLIC KEY-----";
        subject.setPublicKey(publicKey);

        assertThat(subject.getKeyLength(), equalTo(4096));
      });

      it("should return 0 if the private key has not been set", () -> {
        assertThat(subject.getKeyLength(), equalTo(0));
      });
    });

    describe("#copyInto", () -> {
      it("should copy the correct properties into the other object", () -> {
        Instant frozenTime = Instant.ofEpochSecond(1400000000L);

        subject = new NamedRsaSecret("foo");
        subject.setPublicKey("fake-public-key");
        subject.setPrivateKey("fake-private-key");
        subject.setUuid("fake-uuid");
        subject.setUpdatedAt(frozenTime);

        NamedRsaSecret copy = new NamedRsaSecret();
        subject.copyInto(copy);

        assertThat(copy.getName(), equalTo("foo"));
        assertThat(copy.getPublicKey(), equalTo("fake-public-key"));
        assertThat(copy.getPrivateKey(), equalTo("fake-private-key"));

        assertThat(copy.getUuid(), not(equalTo("fake-uuid")));
        assertThat(copy.getUpdatedAt(), not(equalTo(frozenTime)));
      });
    });
  }
}
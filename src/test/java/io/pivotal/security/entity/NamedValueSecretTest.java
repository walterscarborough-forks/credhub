package io.pivotal.security.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.fake.FakeEncryptionService;
import io.pivotal.security.service.EncryptionService;
import io.pivotal.security.util.DatabaseProfileResolver;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.itThrows;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(Spectrum.class)
@ActiveProfiles(value = {"unit-test", "FakeEncryptionService"}, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
public class NamedValueSecretTest {
  @Autowired
  public ObjectMapper objectMapper;

  @Autowired
  EncryptionService encryptionService;

  NamedStringSecret subject;

  {
    wireAndUnwire(this, false);

    beforeEach(() -> {
      subject = new NamedValueSecret("Foo");
      ((FakeEncryptionService) encryptionService).resetEncryptionCount();
    });

    it("returns type value", () -> {
      assertThat(subject.getSecretType(), equalTo("value"));
    });

    describe("with or without alternative names", () -> {
      beforeEach(() -> {
        subject = new NamedValueSecret("foo");
      });

      it("only encrypts the value once for the same secret", () -> {
        subject.setValue("my-value");
        assertThat(((FakeEncryptionService) encryptionService).getEncryptionCount(), equalTo(1));

        subject.setValue("my-value");
        assertThat(((FakeEncryptionService) encryptionService).getEncryptionCount(), equalTo(1));
      });

      it("sets the nonce and the encrypted value", () -> {
        subject.setValue("my-value");
        assertThat(subject.getEncryptedValue(), notNullValue());
        assertThat(subject.getNonce(), notNullValue());
      });

      it("can decrypt values", () -> {
        subject.setValue("my-value");
        assertThat(subject.getValue(), equalTo("my-value"));
      });

      itThrows("when setting a value that is null", IllegalArgumentException.class, () -> {
        subject.setValue(null);
      });
    });
  }
}

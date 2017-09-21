package io.pivotal.security.domain;

import com.google.common.net.InetAddresses;
import io.pivotal.security.exceptions.ParameterizedValidationException;
import io.pivotal.security.request.CertificateGenerationParameters;
import io.pivotal.security.util.CertificateReader;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.springframework.util.StringUtils;

import java.util.List;
import javax.security.auth.x500.X500Principal;

import static com.google.common.collect.Lists.newArrayList;
import static io.pivotal.security.request.CertificateGenerationParameters.CLIENT_AUTH;
import static io.pivotal.security.request.CertificateGenerationParameters.CODE_SIGNING;
import static io.pivotal.security.request.CertificateGenerationParameters.CRL_SIGN;
import static io.pivotal.security.request.CertificateGenerationParameters.DATA_ENCIPHERMENT;
import static io.pivotal.security.request.CertificateGenerationParameters.DECIPHER_ONLY;
import static io.pivotal.security.request.CertificateGenerationParameters.DIGITAL_SIGNATURE;
import static io.pivotal.security.request.CertificateGenerationParameters.EMAIL_PROTECTION;
import static io.pivotal.security.request.CertificateGenerationParameters.ENCIPHER_ONLY;
import static io.pivotal.security.request.CertificateGenerationParameters.KEY_AGREEMENT;
import static io.pivotal.security.request.CertificateGenerationParameters.KEY_CERT_SIGN;
import static io.pivotal.security.request.CertificateGenerationParameters.KEY_ENCIPHERMENT;
import static io.pivotal.security.request.CertificateGenerationParameters.NON_REPUDIATION;
import static io.pivotal.security.request.CertificateGenerationParameters.SERVER_AUTH;
import static io.pivotal.security.request.CertificateGenerationParameters.TIMESTAMPING;
import static org.apache.commons.lang3.StringUtils.join;

public class CertificateParameters {

  private int keyLength;
  private int duration;
  private boolean selfSigned = false;
  private String caName;
  private boolean isCa = false;

  private X500Principal x500Principal;
  private GeneralNames alternativeNames;

  private ExtendedKeyUsage extendedKeyUsage;

  private KeyUsage keyUsage;

  public CertificateParameters(CertificateGenerationParameters generationParameters) {
    this.keyUsage = buildKeyUsage(generationParameters);
    this.x500Principal = buildDn(generationParameters);
    this.alternativeNames = buildAlternativeNames(generationParameters);
    this.extendedKeyUsage = buildExtendedKeyUsage(generationParameters);
    this.caName = generationParameters.getCaName();
    this.selfSigned = generationParameters.isSelfSigned();
    this.duration = generationParameters.getDuration();
    this.keyLength = generationParameters.getKeyLength();
    this.isCa = generationParameters.isCa();
  }


  public CertificateParameters(CertificateReader certificateReader, String caName){
    this.keyUsage = certificateReader.getKeyUsage();
    this.x500Principal = certificateReader.getSubjectName();
    this.alternativeNames = certificateReader.getAlternativeNames();
    this.extendedKeyUsage = certificateReader.getExtendedKeyUsage();
    this.caName = caName;
    this.selfSigned = certificateReader.isSelfSigned();
    this.duration = certificateReader.getDurationDays();
    this.keyLength = certificateReader.getKeyLength();
    this.isCa = certificateReader.isCa();
  }

  public int getKeyLength() {
    return keyLength;
  }

  public int getDuration() {
    return duration;
  }

  public String getCaName() {
    return caName;
  }

  public boolean isSelfSigned() {
    return selfSigned;
  }

  public boolean isCa() {
    return isCa;
  }

  public X500Principal getX500Principal() {
    return x500Principal;
  }

  public GeneralNames getAlternativeNames() {
    return alternativeNames;
  }

  public ExtendedKeyUsage getExtendedKeyUsage() {
    return extendedKeyUsage;
  }

  public KeyUsage getKeyUsage() {
    return keyUsage;
  }

  private KeyUsage buildKeyUsage(CertificateGenerationParameters keyUsageList) {
    if (keyUsageList.getKeyUsage() == null){
      return null;
    }
    int bitmask = 0;
    for (String keyUsage : keyUsageList.getKeyUsage()) {
      switch (keyUsage) {
        case DIGITAL_SIGNATURE:
          bitmask |= KeyUsage.digitalSignature;
          break;
        case NON_REPUDIATION:
          bitmask |= KeyUsage.nonRepudiation;
          break;
        case KEY_ENCIPHERMENT:
          bitmask |= KeyUsage.keyEncipherment;
          break;
        case DATA_ENCIPHERMENT:
          bitmask |= KeyUsage.dataEncipherment;
          break;
        case KEY_AGREEMENT:
          bitmask |= KeyUsage.keyAgreement;
          break;
        case KEY_CERT_SIGN:
          bitmask |= KeyUsage.keyCertSign;
          break;
        case CRL_SIGN:
          bitmask |= KeyUsage.cRLSign;
          break;
        case ENCIPHER_ONLY:
          bitmask |= KeyUsage.encipherOnly;
          break;
        case DECIPHER_ONLY:
          bitmask |= KeyUsage.decipherOnly;
          break;
        default:
          throw new ParameterizedValidationException("error.invalid_key_usage", keyUsage);
      }
    }
    return new KeyUsage(bitmask);
  }

  private X500Principal buildDn(CertificateGenerationParameters params) {
    if (this.x500Principal != null) {
      return this.x500Principal;
    }

    List<String> rdns = newArrayList();

    if (!StringUtils.isEmpty(params.getLocality())) {
      rdns.add("L=" + params.getLocality());
    }
    if (!StringUtils.isEmpty(params.getOrganization())) {
      rdns.add("O=" + params.getOrganization());
    }
    if (!StringUtils.isEmpty(params.getState())) {
      rdns.add("ST=" + params.getState());
    }
    if (!StringUtils.isEmpty(params.getCountry())) {
      rdns.add("C=" + params.getCountry());
    }
    if (!StringUtils.isEmpty(params.getOrganizationUnit())) {
      rdns.add("OU=" + params.getOrganizationUnit());
    }
    if (!StringUtils.isEmpty(params.getCommonName())) {
      rdns.add("CN=" + params.getCommonName());
    }
    return new X500Principal(join(rdns, ","));
  }

  private GeneralNames buildAlternativeNames(CertificateGenerationParameters params) {
    String[] alternativeNamesList = params.getAlternativeNames();
    if (alternativeNamesList == null){
      return null;
    }
    GeneralNamesBuilder builder = new GeneralNamesBuilder();

    for (String name :alternativeNamesList) {
      if (InetAddresses.isInetAddress(name)) {
        builder.addName(new GeneralName(GeneralName.iPAddress, name));
      } else  {
        builder.addName(new GeneralName(GeneralName.dNSName, name));
      }
    }
    return builder.build();
  }

  private ExtendedKeyUsage buildExtendedKeyUsage(CertificateGenerationParameters params) {
    String[] extendedKeyUsageList = params.getExtendedKeyUsage();
    if (extendedKeyUsageList == null){
      return null;
    }
    KeyPurposeId[] keyPurposeIds = new KeyPurposeId[extendedKeyUsageList.length];
    for (int i = 0; i < extendedKeyUsageList.length; i++) {
      switch (extendedKeyUsageList[i]) {
        case SERVER_AUTH:
          keyPurposeIds[i] = KeyPurposeId.id_kp_serverAuth;
          break;
        case CLIENT_AUTH:
          keyPurposeIds[i] = KeyPurposeId.id_kp_clientAuth;
          break;
        case CODE_SIGNING:
          keyPurposeIds[i] = KeyPurposeId.id_kp_codeSigning;
          break;
        case EMAIL_PROTECTION:
          keyPurposeIds[i] = KeyPurposeId.id_kp_emailProtection;
          break;
        case TIMESTAMPING:
          keyPurposeIds[i] = KeyPurposeId.id_kp_timeStamping;
          break;
        default:
          throw new ParameterizedValidationException("error.invalid_extended_key_usage", extendedKeyUsageList[i]);
      }
    }
    return new ExtendedKeyUsage(keyPurposeIds);
  }

}

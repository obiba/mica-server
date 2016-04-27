package org.obiba.mica.web.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
class MicaConfigDtos {
  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @NotNull
  Mica.MicaConfigDto asDto(@NotNull MicaConfig config) {
    Mica.MicaConfigDto.Builder builder = Mica.MicaConfigDto.newBuilder() //
      .setName(config.getName()) //
      .setDefaultCharSet(config.getDefaultCharacterSet())//
      .setOpenAccess(config.isOpenAccess());
    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));

    if(!Strings.isNullOrEmpty(config.getPublicUrl())) {
      builder.setPublicUrl(config.getPublicUrl());
    }

    builder.setOpal(config.getOpal());
    builder.setPrivacyThreshold(config.getPrivacyThreshold());

    if(config.getMicaVersion() != null) {
      builder.setVersion(config.getMicaVersion().toString());
    }

    builder.addAllRoles(config.getRoles());

    builder.setIsFsNotificationsEnabled(config.isFsNotificationsEnabled());

    if(config.getFsNotificationSubject() != null) builder.setFsNotificationsSubject(config.getFsNotificationSubject());

    builder.setIsCommentNotificationsEnabled(config.isCommentNotificationsEnabled());

    if(config.getCommentNotiticationsSubject() != null) builder.setCommentNotificationsSubject(config.getCommentNotiticationsSubject());

    builder.setIsNetworkNotificationsEnabled(config.isNetworkNotificationsEnabled());

    if(config.getNetworkNotificationSubject() != null) builder.setNetworkNotificationsSubject(config.getNetworkNotificationSubject());

    builder.setIsStudyNotificationsEnabled(config.isStudyNotificationsEnabled());

    if(config.getStudyNotificationSubject() != null) builder.setStudyNotificationsSubject(config.getStudyNotificationSubject());

    builder.setIsStudyDatasetNotificationsEnabled(config.isStudyDatasetNotificationsEnabled());

    if(config.getStudyDatasetNotificationSubject() != null) builder.setStudyDatasetNotificationsSubject(config.getStudyDatasetNotificationSubject());

    builder.setIsHarmonizationDatasetNotificationsEnabled(config.isHarmonizationDatasetNotificationsEnabled());

    if(config.getHarmonizationDatasetNotificationSubject() != null) builder.setHarmonizationDatasetNotificationsSubject(config.getHarmonizationDatasetNotificationSubject());

    builder.setIsSingleNetworkEnabled(config.isSingleNetworkEnabled());
    builder.setIsSingleStudyEnabled(config.isSingleStudyEnabled());
    builder.setIsNetworkEnabled(config.isNetworkEnabled());
    builder.setIsStudyDatasetEnabled(config.isStudyDatasetEnabled());
    builder.setIsHarmonizationDatasetEnabled(config.isHarmonizationDatasetEnabled());

    if(config.hasStyle()) builder.setStyle(config.getStyle());

    if(config.hasTranslations()) builder.addAllTranslations(localizedStringDtos.asDto(config.getTranslations()));

    return builder.build();
  }

  @NotNull
  MicaConfig fromDto(@NotNull Mica.MicaConfigDtoOrBuilder dto) {
    MicaConfig config = new MicaConfig();
    config.setName(dto.getName());
    config.setDefaultCharacterSet(dto.getDefaultCharSet());
    config.setOpenAccess(dto.getOpenAccess());

    if(dto.hasPublicUrl()) config.setPublicUrl(dto.getPublicUrl());

    dto.getLanguagesList().forEach(lang -> config.getLocales().add(new Locale(lang)));
    config.setOpal(dto.getOpal());
    if (dto.hasPrivacyThreshold()) config.setPrivacyThreshold(dto.getPrivacyThreshold());

    config.setRoles(dto.getRolesList());
    config.setFsNotificationsEnabled(dto.getIsFsNotificationsEnabled());
    if(dto.hasFsNotificationsSubject()) config.setFsNotificationSubject(dto.getFsNotificationsSubject());
    config.setCommentNotificationsEnabled(dto.getIsCommentNotificationsEnabled());
    if(dto.hasCommentNotificationsSubject()) config.setCommentNotiticationsSubject(dto.getCommentNotificationsSubject());
    config.setStudyNotificationsEnabled(dto.getIsStudyNotificationsEnabled());
    if(dto.hasStudyNotificationsSubject()) config.setStudyNotificationSubject(dto.getStudyNotificationsSubject());
    config.setNetworkNotificationsEnabled(dto.getIsNetworkNotificationsEnabled());
    if(dto.hasNetworkNotificationsSubject()) config.setNetworkNotificationSubject(dto.getNetworkNotificationsSubject());
    config.setStudyDatasetNotificationsEnabled(dto.getIsStudyDatasetNotificationsEnabled());
    if(dto.hasStudyDatasetNotificationsSubject()) config.setStudyDatasetNotificationSubject(dto.getStudyDatasetNotificationsSubject());
    config.setHarmonizationDatasetNotificationsEnabled(dto.getIsHarmonizationDatasetNotificationsEnabled());
    if(dto.hasHarmonizationDatasetNotificationsSubject()) config.setHarmonizationDatasetNotificationSubject(dto.getHarmonizationDatasetNotificationsSubject());

    config.setSingleNetworkEnabled(dto.getIsSingleNetworkEnabled());
    config.setSingleStudyEnabled(dto.getIsSingleStudyEnabled());
    config.setNetworkEnabled(dto.getIsNetworkEnabled());
    config.setStudyDatasetEnabled(dto.getIsStudyDatasetEnabled());
    config.setHarmonizationDatasetEnabled(dto.getIsHarmonizationDatasetEnabled());

    if(dto.hasStyle()) config.setStyle(dto.getStyle());

    if(dto.getTranslationsCount() > 0) config.setTranslations(localizedStringDtos.fromDto(dto.getTranslationsList()));

    return config;
  }

  @NotNull
  Mica.OpalCredentialDto asDto(@NotNull OpalCredential credential) {
    Mica.OpalCredentialDto.Builder builder = Mica.OpalCredentialDto.newBuilder().setType(
      credential.getAuthType() == AuthType.USERNAME
        ? Mica.OpalCredentialType.USERNAME
        : Mica.OpalCredentialType.PUBLIC_KEY_CERTIFICATE).setOpalUrl(credential.getOpalUrl());

    if(!Strings.isNullOrEmpty(credential.getUsername())) builder.setUsername(credential.getUsername());

    return builder.build();
  }

  @NotNull
  Mica.DataAccessFormDto asDto(@NotNull DataAccessForm dataAccessForm) {
    Mica.DataAccessFormDto.Builder builder = Mica.DataAccessFormDto.newBuilder() //
      .setDefinition(dataAccessForm.getDefinition()) //
      .setSchema(dataAccessForm.getSchema()) //
      .addAllPdfTemplates(
        dataAccessForm.getPdfTemplates().values().stream().map(p -> attachmentDtos.asDto(p)).collect(toList())) //
      .addAllProperties(asDtoList(dataAccessForm.getProperties()));

    if(dataAccessForm.hasTitleFieldPath()) {
      builder.setTitleFieldPath(dataAccessForm.getTitleFieldPath());
    }

    if(dataAccessForm.hasIdPrefix()) {
      builder.setIdPrefix(dataAccessForm.getIdPrefix());
    }
    builder.setIdLength(dataAccessForm.getIdLength()) //
      .setNotifySubmitted(dataAccessForm.isNotifySubmitted()) //
      .setNotifyReviewed(dataAccessForm.isNotifyReviewed()) //
      .setNotifyApproved(dataAccessForm.isNotifyApproved()) //
      .setNotifyRejected(dataAccessForm.isNotifyRejected()) //
      .setNotifyReopened(dataAccessForm.isNotifyReopened()) //
      .setNotifyCommented(dataAccessForm.isNotifyCommented()) //
      .setWithReview(dataAccessForm.isWithReview()) //
      .setApprovedFinal(dataAccessForm.isApprovedFinal()) //
      .setRejectedFinal(dataAccessForm.isRejectedFinal());

    if(dataAccessForm.getSubmittedSubject() != null) builder.setSubmittedSubject(dataAccessForm.getSubmittedSubject());

    if(dataAccessForm.getReviewedSubject() != null) builder.setReviewedSubject(dataAccessForm.getReviewedSubject());

    if(dataAccessForm.getApprovedSubject() != null) builder.setApprovedSubject(dataAccessForm.getApprovedSubject());

    if(dataAccessForm.getRejectedSubject() != null) builder.setRejectedSubject(dataAccessForm.getRejectedSubject());

    if(dataAccessForm.getReopenedSubject() != null) builder.setReopenedSubject(dataAccessForm.getReopenedSubject());

    if(dataAccessForm.getCommentedSubject() != null) builder.setCommentedSubject(dataAccessForm.getCommentedSubject());

    return builder.build();
  }

  @NotNull
  DataAccessForm fromDto(@NotNull Mica.DataAccessFormDto dto) {
    DataAccessForm dataAccessForm = new DataAccessForm();
    dataAccessForm.setSchema(dto.getSchema());
    dataAccessForm.setDefinition(dto.getDefinition());

    dataAccessForm.setProperties(dto.getPropertiesList().stream()
      .collect(toMap(e -> e.getName(), e -> localizedStringDtos.fromDto(e.getValueList()))));

    dataAccessForm.setPdfTemplates(
      dto.getPdfTemplatesList().stream().map(t -> attachmentDtos.fromDto(t)).collect(toMap(a -> a.getLang(), x -> x)));

    if(dto.hasTitleFieldPath()) {
      dataAccessForm.setTitleFieldPath(dto.getTitleFieldPath());
    }

    if(dto.hasIdPrefix()) {
      dataAccessForm.setIdPrefix(dto.getIdPrefix());
    }
    dataAccessForm.setIdLength(dto.getIdLength());

    dataAccessForm.setNotifySubmitted(dto.getNotifySubmitted());
    dataAccessForm.setSubmittedSubject(dto.getSubmittedSubject());

    dataAccessForm.setNotifyReviewed(dto.getNotifyReviewed());
    dataAccessForm.setReviewedSubject(dto.getReviewedSubject());

    dataAccessForm.setNotifyApproved(dto.getNotifyApproved());
    dataAccessForm.setApprovedSubject(dto.getApprovedSubject());

    dataAccessForm.setNotifyRejected(dto.getNotifyRejected());
    dataAccessForm.setRejectedSubject(dto.getRejectedSubject());

    dataAccessForm.setNotifyReopened(dto.getNotifyReopened());
    dataAccessForm.setReopenedSubject(dto.getReopenedSubject());

    dataAccessForm.setNotifyCommented(dto.getNotifyCommented());
    dataAccessForm.setCommentedSubject(dto.getCommentedSubject());

    dataAccessForm.setWithReview(dto.getWithReview());
    dataAccessForm.setApprovedFinal(dto.getApprovedFinal());
    dataAccessForm.setRejectedFinal(dto.getRejectedFinal());

    return dataAccessForm;
  }

  @NotNull
  List<Mica.DataAccessFormDto.LocalizedPropertyDto> asDtoList(@NotNull Map<String, LocalizedString> properties) {
    return properties.entrySet().stream().map(
      e -> Mica.DataAccessFormDto.LocalizedPropertyDto.newBuilder().setName(e.getKey())
        .addAllValue(localizedStringDtos.asDto(e.getValue())).build()).collect(toList());
  }
}

package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
class NetworkSummaryDtos {

  private static final Logger log = LoggerFactory.getLogger(NetworkSummaryDtos.class);

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private NetworkService networkService;

  @Inject
  private SubjectAclService subjectAclService;

  @NotNull
  public Mica.NetworkSummaryDto.Builder asDtoBuilder(@NotNull String id, boolean asDraft) {
    NetworkState networkState = networkService.getEntityState(id);
    Network network = networkService.findById(id);
    Mica.NetworkSummaryDto.Builder builder = Mica.NetworkSummaryDto.newBuilder();

    builder.setId(id).addAllAcronym(localizedStringDtos.asDto(network.getAcronym())) //
      .addAllName(localizedStringDtos.asDto(network.getName())) //
      .setPublished(networkState.isPublished());

    if(asDraft) {
      builder.setTimestamps(TimestampsDtos.asDto(network));
    }

    network.getStudyIds().stream()
      .filter(sId -> asDraft && subjectAclService.isPermitted("/draft/study", "VIEW", sId)
          || subjectAclService.isAccessible("/study", sId))
      .forEach(sId -> {
        try {
          builder.addStudyIds(sId);
        } catch(NoSuchEntityException e) {
          log.warn("Study not found in network {}: {}", network.getId(), sId);
          // ignore
        }
      });

    network.getNetworkIds().stream()
      .filter(nId -> asDraft && subjectAclService.isPermitted("/draft/network", "VIEW", nId)
          || subjectAclService.isAccessible("/network", nId))
      .forEach(nId -> {
        try {
          builder.addNetworkIds(nId);
        } catch(NoSuchEntityException e) {
          log.warn("Network not found in network {}: {}", network.getId(), nId);
          // ignore
        }
      });

    return builder;
  }
}

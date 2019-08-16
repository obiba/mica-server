package org.obiba.mica.core.service;

import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Convenient service for server specific config queries
 */
@Service
public class AgateServerConfigService {


  protected final MicaConfigService micaConfigService;

  @Inject
  public AgateServerConfigService(MicaConfigService micaConfigService) {
    this.micaConfigService = micaConfigService;
  }

  public String getServiceKey() {
    return micaConfigService.getServiceKey();
  }

  public String getServiceName() {
    return micaConfigService.getServiceName();
  }

  public String getAgateUrl() {
    return micaConfigService.getAgateUrl();
  }

  String buildToken() {
    return getServiceName() + ":" + getServiceKey();
  }

  boolean isSecured() {
    return getAgateUrl().toLowerCase().startsWith("https://");
  }
}

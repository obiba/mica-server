/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.obiba.magma.support.Initialisables;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.opal.core.cfg.NoSuchTaxonomyException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.rest.client.magma.OpalJavaClient;
import org.obiba.opal.rest.client.magma.RestDatasource;
import org.obiba.opal.rest.client.magma.RestDatasourceFactory;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.obiba.security.KeyStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OpalService implements EnvironmentAware {

  public static final String OPAL_KEYSTORE = "opal";

  private static final Logger log = LoggerFactory.getLogger(OpalService.class);

  private Map<String, Pair<OpalCredential, RestDatasource>> cachedDatasources = new HashMap<>();

  private RelaxedPropertyResolver opalPropertyResolver;

  private OpalJavaClient opalJavaClient;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private KeyStoreService keyStoreService;

  @Inject
  private OpalCredentialService opalCredentialService;

  @Override
  public void setEnvironment(Environment environment) {
    opalPropertyResolver = new RelaxedPropertyResolver(environment, "opal.");
  }

  /**
   * Get the datasource from the provided Opal server url.
   *
   * @param opalUrl if null, default Opal server url will be used.
   * @param project
   * @return
   */
  public synchronized RestDatasource getDatasource(@Nullable String opalUrl, String project) {
    final String projectUrl = getOpalProjectUrl(opalUrl, project);
    opalUrl = Strings.isNullOrEmpty(opalUrl) ? getDefaultOpal() : opalUrl;

    OpalCredential opalCredential = opalCredentialService.findOpalCredentialById(opalUrl).orElse(
      new OpalCredential(getDefaultOpal(), AuthType.USERNAME, getOpalUsername(), getOpalPassword()));

    if (cachedDatasources.containsKey(projectUrl))
    {
      Pair<OpalCredential, RestDatasource> p = cachedDatasources.get(projectUrl);

      if (p.getLeft().equals(opalCredential)) {
        log.debug("Using cached rest datasource to " + projectUrl);
        return p.getRight();
      }

      log.debug("Opal credential changed, evicting rest datasource for " + projectUrl);

      cachedDatasources.remove(projectUrl); //opal credential changed
    }

    RestDatasource datasource = createRestDatasource(opalCredential, projectUrl, opalUrl, project);
    Initialisables.initialise(datasource);
    cachedDatasources.put(projectUrl, Pair.of(opalCredential, datasource));

    log.debug("Initializaed rest datasource for " + projectUrl);

    return datasource;
  }

  private RestDatasource createRestDatasource(OpalCredential opalCredential, String projectUrl,
    String opalUrl, String project) {
    if(opalCredential.getAuthType() == AuthType.CERTIFICATE) {
      KeyStoreManager kms = keyStoreService.getKeyStore(OPAL_KEYSTORE);

      if(!kms.aliasExists(opalCredential.getOpalUrl()))
        throw new IllegalStateException("Trying to use opal certificate credential but could not be found in keystore.");

      return (RestDatasource) new RestDatasourceFactory(projectUrl, opalUrl, kms.getKeyStore(), opalUrl,
        micaConfigService.getConfig().getSecretKey(), project).create();
    }

    return (RestDatasource) new RestDatasourceFactory(projectUrl, opalUrl, opalCredential.getUsername(),
        opalCredential.getPassword(), project).create();
  }

  /**
   * Get a {@link org.obiba.opal.rest.client.magma.RestDatasource} from the default Opal server.
   *
   * @param project
   * @return
   */
  public RestDatasource getDatasource(String project) {
    return getDatasource(getDefaultOpal(), project);
  }

  private String getOpalProjectUrl(String opalUrl, String project) {
    String baseUrl = opalUrl == null ? getDefaultOpal() : opalUrl;

    return String.format("%s/ws/datasource/%s", StringUtils.stripEnd(baseUrl, "/"), project);
  }

  /**
   * Get the url of the default Opal server as defined in the configuration.
   *
   * @return
   */
  public String getDefaultOpal() {
    return opalPropertyResolver.getProperty("url");
  }

  //
  // Taxonomies
  //

  public List<Taxonomy> getTaxonomies() {
    return getTaxonomyDtos().stream().map(Dtos::fromDto).collect(Collectors.toList());
  }

  public List<Opal.TaxonomyDto> getTaxonomyDtos() {
    try {
      OpalJavaClient opalClient = getOpalJavaClient();
      URI uri = opalClient.newUri().segment("system", "conf", "taxonomies").build();
      return opalClient.getResources(Opal.TaxonomyDto.class, uri, Opal.TaxonomyDto.newBuilder());
    } catch(URISyntaxException e) {
      log.error("Malformed URI to Opal: " + getDefaultOpal(), e);
      throw new NoSuchElementException();
    }
  }

  /**
   * Get a summary of the {@link org.obiba.opal.core.domain.taxonomy.Taxonomy}s available from Opal master.
   *
   * @return
   */
  public Opal.TaxonomiesDto getTaxonomySummaryDtos() {
    try {
      OpalJavaClient opalClient = getOpalJavaClient();
      URI uri = opalClient.newUri().segment("system", "conf", "taxonomies", "summaries").build();
      return opalClient.getResource(Opal.TaxonomiesDto.class, uri, Opal.TaxonomiesDto.newBuilder());
    } catch(URISyntaxException e) {
      log.error("Malformed URI to Opal: " + getDefaultOpal(), e);
      throw new NoSuchElementException();
    }
  }

  /**
   * Get the {@link org.obiba.opal.core.domain.taxonomy.Taxonomy} from Opal master.
   *
   * @param name
   * @return
   * @throws org.obiba.opal.core.cfg.NoSuchTaxonomyException
   */
  public Taxonomy getTaxonomy(String name) {
    return Dtos.fromDto(getTaxonomyDto(name));
  }

  /**
   * Get the {@link org.obiba.opal.core.domain.taxonomy.Taxonomy} as a Dto from Opal master.
   *
   * @param name
   * @return
   * @throws org.obiba.opal.core.cfg.NoSuchTaxonomyException
   */
  public Opal.TaxonomyDto getTaxonomyDto(String name) {
    try {
      OpalJavaClient opalClient = getOpalJavaClient();
      URI uri = opalClient.newUri().segment("system", "conf", "taxonomy", name).build();
      return opalClient.getResource(Opal.TaxonomyDto.class, uri, Opal.TaxonomyDto.newBuilder());
    } catch(URISyntaxException e) {
      log.error("Malformed URI to Opal: " + getDefaultOpal(), e);
      throw new NoSuchTaxonomyException(name);
    }
  }

  //
  // Private methods
  //

  private String getOpalUsername() {
    return opalPropertyResolver.getProperty("username");
  }

  private String getOpalPassword() {
    return opalPropertyResolver.getProperty("password");
  }

  private OpalJavaClient getOpalJavaClient() throws URISyntaxException {
    if(opalJavaClient != null) return opalJavaClient;

    String opalUrl = getDefaultOpal();
    while(opalUrl.endsWith("/")) {
      opalUrl = opalUrl.substring(0, opalUrl.length() - 1);
    }
    if(!opalUrl.endsWith("/ws")) {
      opalUrl = opalUrl + "/ws";
    }
    return opalJavaClient = new OpalJavaClient(opalUrl, getOpalUsername(), getOpalPassword());
  }
}

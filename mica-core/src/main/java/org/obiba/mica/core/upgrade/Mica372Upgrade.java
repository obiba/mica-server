package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.study.service.StudyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class Mica372Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica372Upgrade.class);


  private final MongoTemplate mongoTemplate;

  private final EventBus eventBus;

  private final TaxonomyConfigService taxonomyConfigService;

  private final StudyService studyService;

  private final NetworkService networkService;

  @Inject
  public Mica372Upgrade(MongoTemplate mongoTemplate,
    EventBus eventBus,
    TaxonomyConfigService taxonomyConfigService,
    StudyService studyService, NetworkService networkService) {
    this.mongoTemplate = mongoTemplate;
    this.eventBus = eventBus;
    this.taxonomyConfigService = taxonomyConfigService;
    this.studyService = studyService;
    this.networkService = networkService;
  }


  @Override
  public String getDescription() {
    return "Upgrade data to 3.7.2";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 7, 2);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.7.2");

    try {
      logger.info("Checking presence of \"start-range\" vocabulary in current Study taxonomy");
      mongoTemplate.execute(db -> db.eval(addStartRangeVocabularyToStudyTaxonomyIfMissing()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addStartRangeVocabularyToStudyTaxonomyIfMissing.", e);
    }

    try {
      logger.info("Indexing Study Taxonomy");
      Taxonomy studyTaxonomy = taxonomyConfigService.findByTarget(TaxonomyTarget.STUDY);
      eventBus.post(new TaxonomiesUpdatedEvent(studyTaxonomy.getName(), TaxonomyTarget.STUDY));
    } catch(Exception e) {
      logger.error("Failed to index Taxonomy", e);
    }

    try {
      logger.info("Updating studies and networks with their membership sort orders.");
      mongoTemplate.execute(db -> db.eval(setMemberShipSortOrder()));

      studyService.findAllStates().stream().filter(state -> !state.hasRevisionsAhead() && state.isPublished()).forEach(state -> {
        studyService.save(studyService.findStudy(state.getId()), "Updating membership sort orders.");
        studyService.publish(state.getId(), true, PublishCascadingScope.NONE);
      });

      networkService.findAllStates().stream().filter(state -> !state.hasRevisionsAhead() && state.isPublished()).forEach(state -> {
        networkService.save(networkService.findById(state.getId()), "Updating membership sort orders.");
        networkService.publish(state.getId(), true, PublishCascadingScope.NONE);
      });
    } catch (Exception e) {
      logger.error("Failed to update studies and networks with their membership sort orders.", e);
    }
  }

  private String addStartRangeVocabularyToStudyTaxonomyIfMissing() {
    return
      "var startRangeVocabulary = {\n" +
        "  \"repeatable\": false,\n" +
        "  \"terms\": [\n" +
        "    {\n" +
        "      \"name\": \"*:1950\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"Before 1950\",\n" +
        "        \"fr\": \"Avant 1950\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"Before 1950\",\n" +
        "        \"fr\": \"Avant 1950\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1950:1960\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1950 to 1959\",\n" +
        "        \"fr\": \"1950 à 1959\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"1950 to 1959\",\n" +
        "        \"fr\": \"1950 à 1959\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1960:1970\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1960 to 1969\",\n" +
        "        \"fr\": \"1960 à 1969\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"1960 to 1969\",\n" +
        "        \"fr\": \"1960 à 1969\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1970:1980\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1970 to 1979\",\n" +
        "        \"fr\": \"1970 à 1979\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"1970 to 1979\",\n" +
        "        \"fr\": \"1970 à 1979\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1980:1990\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1980 to 1989\",\n" +
        "        \"fr\": \"1980 à 1989\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"1980 to 1989\",\n" +
        "        \"fr\": \"1980 à 1989\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"1990:2000\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"1990 to 1999\",\n" +
        "        \"fr\": \"1990 à 1999\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"1990 to 1999\",\n" +
        "        \"fr\": \"1990 à 1999\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"2000:2010\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"2000 to 2009\",\n" +
        "        \"fr\": \"2000 à 2009\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"2000 to 2009\",\n" +
        "        \"fr\": \"2000 à 2009\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"2010:*\",\n" +
        "      \"title\": {\n" +
        "        \"en\": \"2010 and later\",\n" +
        "        \"fr\": \"2010 et plus tard\"\n" +
        "      },\n" +
        "      \"description\": {\n" +
        "        \"en\": \"2010 and later\",\n" +
        "        \"fr\": \"2010 et plus tard\"\n" +
        "      },\n" +
        "      \"keywords\": {},\n" +
        "      \"attributes\": {}\n" +
        "    }\n" +
        "  ],\n" +
        "  \"name\": \"start-range\",\n" +
        "  \"title\": {\n" +
        "    \"en\": \"Start year (ranges)\",\n" +
        "    \"fr\": \"Année de début (intervalles)\"\n" +
        "  },\n" +
        "  \"description\": {\n" +
        "    \"en\": \"Year in which the study has started.\",\n" +
        "    \"fr\": \"Année à laquelle l'étude a commencé.\"\n" +
        "  },\n" +
        "  \"keywords\": {},\n" +
        "  \"attributes\": {\n" +
        "    \"type\": \"integer\",\n" +
        "    \"field\": \"model.startYear\",\n" +
        "    \"alias\": \"model-startYear-range\",\n" +
        "    \"range\": \"true\"\n" +
        "  }\n" +
        "};\n" +
        "\n" +
        "if (db.getCollection('taxonomyEntityWrapper').find({\n" +
        "    $and: [\n" +
        "        {_id: 'study', \"taxonomy.vocabularies\": {$elemMatch: {\"name\": \"start\"}}},\n" +
        "        {_id: 'study', \"taxonomy.vocabularies\": {$elemMatch: {\"attributes.field\": \"model.startYear\"}}},\n" +
        "        {_id: 'study', \"taxonomy.vocabularies\": {$not: {$elemMatch: {\"name\": \"start-range\"}}}}\n" +
        "    ]\n" +
        " }).count() > 0) {\n" +
        "    db.taxonomyEntityWrapper.update({\"_id\": \"study\"}, {$push: {\"taxonomy.vocabularies\": startRangeVocabulary}})\n" +
        "}\n";
  }

  private String setMemberShipSortOrder() {
    return
          "db.study.find({}).forEach(function (study) { \n"
        + "  var roles = Object.keys(study.memberships); \n"
        + "  var membershipSortOrder = {}; \n"
        + "  roles.forEach(function (role) { \n"
        + "    var memberships = study.memberships[role].map(function (membership) { \n"
        + "      return membership.person.$id.str; \n"
        + "    }); membershipSortOrder[role] = memberships; }); \n"
        + "    db.study.update({_id: study._id}, {$set: {membershipSortOrder: membershipSortOrder}}); \n"
        + "});\n"
        + "\n"
        + "db.network.find({}).forEach(function (network) { \n"
        + "  var roles = Object.keys(network.memberships); \n"
        + "  var membershipSortOrder = {}; \n"
        + "  roles.forEach(function (role) { \n"
        + "    var memberships = network.memberships[role].map(function (membership) { \n"
        + "      return membership.person.$id.str; \n"
        + "    }); membershipSortOrder[role] = memberships; }); \n"
        + "    db.network.update({_id: network._id}, {$set: {membershipSortOrder: membershipSortOrder}}); \n"
        + "});";
  }
}

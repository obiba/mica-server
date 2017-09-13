package org.obiba.mica.core.upgrade;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.git.CommitInfo;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.CollectionStudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Component
public class Mica310Upgrade implements UpgradeStep {

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private CollectionDatasetService collectionDatasetService;

  @Inject
  private CollectionStudyService collectionStudyService;

  private static final Logger logger = LoggerFactory.getLogger(Mica310Upgrade.class);

  @Override
  public String getDescription() {
    return "Migrate data to mica 3.1.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 1, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.3.0");

    setupDatasetOrders();

    republishStudiesWithInvalidContent();
  }

  private void republishStudiesWithInvalidContent() {

    List<Study> publishedStudies = collectionStudyService.findAllPublishedStudies();

    for (Study publishedStudy : publishedStudies) {

      if (!containsInvalidData(publishedStudy))
        continue;

      publishedStudy = transformToValidStudy(publishedStudy);

      EntityState studyState = collectionStudyService.getEntityState(publishedStudy.getId());
      if (studyState.getRevisionsAhead() == 0) {
        collectionStudyService.save(publishedStudy);
        collectionStudyService.publish(publishedStudy.getId(), true);
      } else {
        Study draftStudy = collectionStudyService.findStudy(publishedStudy.getId());
        draftStudy = transformToValidStudy(draftStudy);
        collectionStudyService.save(publishedStudy);
        collectionStudyService.publish(publishedStudy.getId(), true);
        collectionStudyService.save(draftStudy);
      }
    }

    List<String> publishedStudiesIds = publishedStudies.stream().map(AbstractGitPersistable::getId).collect(toList());
    collectionStudyService.findAllDraftStudies().stream()
      .filter(unknownStateStudy -> !publishedStudiesIds.contains(unknownStateStudy.getId()))
      .filter(this::containsInvalidData)
      .map(this::transformToValidStudy)
      .forEach(collectionStudyService::save);
  }

  private boolean containsInvalidData(Study study) {
    return containsInvalidMethodsDesign(study)
      || containsInvalidExistingStudies(study)
      || !containsMethods(study, "recruitments")
      || !containsMethods(study, "otherDesign")
      || !containsMethods(study, "followUpInfo")
      || !containsMethods(study, "otherRecruitment")
      || !containsMethods(study, "info");
  }

  private boolean containsInvalidMethodsDesign(Study study) {
    try {
      Map<String, Object> methods = getModelMethods(study);
      if (methods.containsKey("designs") && !methods.containsKey("design"))
        return true;
    } catch (RuntimeException ignore) {
    }
    return false;
  }

  private boolean containsMethods(Study study, String methodsAttribute) {
    try {
      return (getModelMethods(study)).get(methodsAttribute) != null;
    } catch (RuntimeException ignore) {
      return false;
    }
  }

  private boolean containsRecruitment(JSONObject json) {
    try {
      JSONArray jsonArray = json.getJSONObject("methods").getJSONArray("recruitments");
      return jsonArray.length() > 0;
    } catch (RuntimeException | JSONException ignore) {
      return false;
    }
  }

  private boolean containsInvalidExistingStudies(Study study) {
    if (study.getPopulations() == null)
      return false;

    for (Population population : study.getPopulations()) {
      try {
        if (((List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources")).contains("existing_studies"))
          return true;
      } catch (RuntimeException ignore) {
      }
    }
    return false;
  }

  private Study transformToValidStudy(Study study) {
    transformMethodsDesign(study);
    transformExistingStudies(study);
    addLostMethods(study);
    return study;
  }

  private void transformExistingStudies(Study study) {
    try {
      for (Population population : study.getPopulations()) {
        List<String> dataSources = (List<String>) ((Map<String, Object>) population.getModel().get("recruitment")).get("dataSources");
        dataSources.remove("existing_studies");
        dataSources.add("exist_studies");
      }
    } catch (RuntimeException ignore) {
    }
  }

  private void transformMethodsDesign(Study study) {
    try {
      if (containsInvalidMethodsDesign(study)) {
        Map<String, Object> methods = getModelMethods(study);
        String methodsDesign = ((List<String>) methods.get("designs")).get(0);
        methods.put("design", methodsDesign);
      }
    } catch (RuntimeException ignore) {
    }
  }

  private Map<String, Object> getModelMethods(Study study) {
    return (Map<String, Object>) study.getModel().get("methods");
  }

  private void setupDatasetOrders() {
    try {
      logger.info("Updating \"study\" populations and data collection events by adding a weight property based on current index.");
      mongoTemplate.execute(db -> db.eval(addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex.", e);
    }

    logger.info("Indexing all Collected Datasets");
    collectionDatasetService.indexAll();
  }

  private void addLostMethods(Study study) {

    Iterable<CommitInfo> commitInfos = collectionStudyService.getCommitInfos(study);

    List<JSONObject> history = StreamSupport.stream(commitInfos.spliterator(), false)
      .map(commit -> collectionStudyService.getFromCommitAsJson(study, commit.getCommitId()))
      .collect(toList());

    addRecruitmentsIfMissing(study, history);
    addMethodsIfMissing(study, history, "otherDesign");
    addMethodsIfMissing(study, history, "followUpInfo");
    addMethodsIfMissing(study, history, "otherRecruitment");
    addMethodsIfMissing(study, history, "info");
  }

  private void addRecruitmentsIfMissing(Study study, List<JSONObject> history) {
    try {
      if (!containsRecruitments(study)) {
        Optional<List<String>> optionalRecruitments = history.stream()
          .filter(this::containsRecruitment)
          .findFirst()
          .map(this::extractRecruitments);

        optionalRecruitments.ifPresent(recruitments -> (getModelMethods(study)).put("recruitments", recruitments));
      }
    } catch (RuntimeException ignore) {
    }
  }

  private void addMethodsIfMissing(Study study, List<JSONObject> history, String methodsAttribute) {

    try {
      if (!containsRecruitments(study)) {
        Optional<Object> optionalMethodsAttributeValue = history.stream()
          .filter(t -> this.containsMethods(study, methodsAttribute))
          .filter(Objects::nonNull)
          .map(t -> this.extractMethodsLocalizedString(t, methodsAttribute))
          .findFirst();

        optionalMethodsAttributeValue.ifPresent(
          methodsAttributeValue -> (getModelMethods(study)).put(methodsAttribute, methodsAttributeValue));
      }
    } catch (RuntimeException ignore) {
    }
  }

  private boolean containsRecruitments(Study study) {
    return study.getModel() != null
      && study.getModel().containsKey("methods")
      && (getModelMethods(study)).containsKey("recruitments");
  }

  private List<String> extractRecruitments(JSONObject jsonStudy) {
    try {
      List<String> recruitments = new ArrayList<>();
      JSONArray jsonArray = jsonStudy.getJSONObject("methods").getJSONArray("recruitments");
      for (int i = 0; i < jsonArray.length(); i++) {
        recruitments.add(jsonArray.getString(i));
      }
      return recruitments;
    } catch (JSONException ignore) {
      return new ArrayList<>(0);
    }
  }

  private Object extractMethodsLocalizedString(JSONObject jsonStudy, String methodsAttribute) {
    try {
      return jsonStudy.getJSONObject("methods").get(methodsAttribute);
    } catch (JSONException ignore) {
      return null;
    }
  }

  private String addPopulationAndDataCollectionEventWeightPropertyBasedOnIndex() {
    return
      "db.study.find({}).forEach(function (study) {\n" +
        "  study.populations.forEach(function (population, populationIndex) {\n" +
        "    population.weight = populationIndex;\n" +
        "    population.dataCollectionEvents.forEach(function (dataCollectionEvent, dceIndex) {\n" +
        "      dataCollectionEvent.weight = dceIndex;\n" +
        "    });\n" +
        "  });\n" +
        "\n" +
        "  db.study.update(\n" +
        "    {_id: study._id},\n" +
        "    {$set: study}\n" +
        "  );\n" +
        "});";
  }
}

package org.obiba.mica.study;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.obiba.core.util.FileUtil;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.impl.GridFsService;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.mongodb.Mongo;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.obiba.mica.core.domain.LocalizedString.en;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { StudyServiceTest.Config.class, JsonConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StudyServiceTest {

//  private static final Logger log = LoggerFactory.getLogger(StudyServiceTest.class);
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Inject
  private StudyService studyService;

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private MongoTemplate mongoTemplate;

  @BeforeClass
  public static void init() {
    SecurityUtils.setSecurityManager(new DefaultSecurityManager());
  }

  @Before
  public void clearDatabase() {
    mongoTemplate.getDb().dropDatabase();
    reset(eventBus);
  }

  @Test
  public void test_create_and_load_new_study() throws Exception {

    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    studyService.save(study);

    List<StudyState> studyStates = studyStateRepository.findAll();
    assertThat(studyStates).hasSize(1);

    StudyState studyState = studyStates.get(0);
    assertThat(studyState.getId()) //
        .isNotEmpty() //
        .isEqualTo(study.getId());
    assertThat(studyState.getName()).isEqualTo(study.getName());

    verify(eventBus).post(any(DraftStudyUpdatedEvent.class));

    Study retrievedStudy = studyService.findDraft(study.getId());
    assertThat(retrievedStudy).areFieldsEqualToEachOther(study);
  }

  @Test
  public void testCreateStudyWithContacts() throws Exception {
    Study study = new Study();
    study.setId("test");

    Person person = new Person();
    person.setEmail("test@test.com");
    List<Person> persons = Lists.newArrayList();
    persons.add(person);
    study.setContacts(persons);

    studyService.save(study);
    Study retrievedStudy = studyService.findDraft(study.getId());

    assertThat(retrievedStudy.getContacts()).contains(person);
  }

  @Test
  public void test_update_study() throws Exception {
    Study study = new Study();
    study.setName(en("name en to update").forFr("name fr to update"));
    studyService.save(study);

    study.setName(en("new name en").forFr("new name fr"));
    studyService.save(study);

    List<StudyState> studyStates = studyStateRepository.findAll();
    assertThat(studyStates).hasSize(1);

    StudyState studyState = studyStates.get(0);
    assertThat(studyState.getId()) //
        .isNotEmpty() //
        .isEqualTo(study.getId());
    assertThat(studyState.getName()).isEqualTo(study.getName());

    verify(eventBus, times(2)).post(any(DraftStudyUpdatedEvent.class));

    Study retrievedStudy = studyService.findDraft(study.getId());
    assertThat(retrievedStudy).areFieldsEqualToEachOther(study);
  }

  @Test
  public void test_publish_current() throws Exception {
    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    studyService.save(study);

    assertThat(studyService.findAllStates()).hasSize(1);
    assertThat(studyService.findPublishedStates()).isEmpty();

    studyService.publish(study.getId(), true);
    List<StudyState> publishedStates = studyService.findPublishedStates();
    assertThat(publishedStates).hasSize(1);
    StudyState publishedState = publishedStates.get(0);
    assertThat(publishedState.getId()).isEqualTo(study.getId());
    assertThat(publishedState.getPublishedTag()).isEqualTo("1");

    Study draft = studyService.findDraft(study.getId());
    draft.setName(en("new name en").forFr("new name fr"));
    studyService.save(draft);

    assertThat(studyService.findDraft(study.getId())).areFieldsEqualToEachOther(draft);
  }

  @Test
  public void test_find_all_draft_studies() {
    Stream.of("cancer", "gout", "diabetes").forEach(name -> {
      Study draft = new Study();
      draft.setName(en(name +" en").forFr(name + " fr"));
      studyService.save(draft);
    });

    List<Study> drafts = studyService.findAllDraftStudies();
    assertThat(drafts.size()).isEqualTo(3);
    assertThat(drafts.get(2).getName().get("en")).isEqualTo("diabetes en");
  }

  @Test
  public void test_loosing_git_base_repo() throws IOException {
    Study study = new Study();
    Stream.of("a", "b", "c").forEach(name -> {
      study.setName(en(name+ " en").forFr(name + " fr"));
      studyService.save(study);
      studyService.publish(study.getId(), true);
    });

    FileUtil.delete(Config.BASE_REPO);
    Study draft = studyService.findDraft(study.getId());
    draft.setName(en("d en").forFr("d fr"));
    studyService.save(draft);
    studyService.publish(draft.getId(), true);
    StudyState studyState = studyService.findStateById(draft.getId());
    assertThat(studyState.isPublished()).isTrue();
    assertThat(studyState.getPublishedTag()).isEqualTo("4");
  }

  @Test
  public void test_loosing_git_clone_repo() throws IOException {
    Study study = new Study();
    Stream.of("a", "b", "c").forEach(name -> {
      study.setName(en(name+ " en").forFr(name + " fr"));
      studyService.save(study);
      studyService.publish(study.getId(), true);
    });

    FileUtil.delete(Config.BASE_CLONE);
    Study draft = studyService.findDraft(study.getId());
    draft.setName(en("d en").forFr("d fr"));
    studyService.save(draft);
    studyService.publish(draft.getId(), true);
    StudyState studyState = studyService.findStateById(draft.getId());
    assertThat(studyState.isPublished()).isTrue();
    assertThat(studyState.getPublishedTag()).isEqualTo("4");
  }

  @Test
  public void test_loosing_git_base_and_clone_repos() throws IOException {
    Study study = new Study();
    Stream.of("a", "b", "c").forEach(name -> {
      study.setName(en(name+ " en").forFr(name + " fr"));
      studyService.save(study);
      studyService.publish(study.getId(), true);
    });

    FileUtil.delete(Config.BASE_REPO);
    FileUtil.delete(Config.BASE_CLONE);

    Study draft = studyService.findDraft(study.getId());
    draft.setName(en("d en").forFr("d fr"));
    studyService.save(draft);
    studyService.publish(draft.getId(), true);
    StudyState studyState = studyService.findStateById(draft.getId());
    assertThat(studyState.isPublished()).isTrue();
    assertThat(studyState.getPublishedTag()).isEqualTo("1");
  }

  @Test
  public void test_delete_study() {
    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    studyService.save(study);

    assertThat(studyStateRepository.findAll()).hasSize(1);

    studyService.delete(study.getId());

    assertThat(studyRepository.findAll()).hasSize(0);
    assertThat(studyStateRepository.findAll()).hasSize(0);
  }

  @Test
  public void test_delete_study_conflict() {
    Study study = new Study();
    study.setName(en("name en").forFr("name fr"));
    studyService.save(study);
    Network network = new Network();
    network.setId("test");
    network.setStudyIds(new ArrayList() {{ add(study.getId()); }});
    networkRepository.save(network);

    assertThat(studyStateRepository.findAll()).hasSize(1);

    exception.expect(ConstraintException.class);

    studyService.delete(study.getId());
  }

  @After
  public void cleanup() throws IOException {
    FileUtil.delete(Config.BASE_REPO);
    FileUtil.delete(Config.BASE_CLONE);
    FileUtil.delete(Config.TEMP);
  }

  @Configuration
  @EnableMongoRepositories("org.obiba.mica")
  static class Config extends AbstractMongoConfiguration {

    static final File BASE_REPO = Files.createTempDir();

    static final File BASE_CLONE = Files.createTempDir();

    static final File TEMP = Files.createTempDir();

    static {
      BASE_REPO.deleteOnExit();
      BASE_CLONE.deleteOnExit();
      TEMP.deleteOnExit();
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
      return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public StudyService studyService() {
      return new StudyService();
    }

    @Bean
    public TempFileService tempFileService() {
      TempFileService tempFileService = new TempFileService();
      tempFileService.setTmpRoot(TEMP);
      return tempFileService;
    }

    @Bean
    public GitService gitService() throws IOException {
      GitService gitService = new GitService();
      gitService.setRepositoriesRoot(BASE_REPO);
      gitService.setClonesRoot(BASE_CLONE);
      return gitService;
    }

    @Bean
    public GitCommandHandler gitCommandHandler() throws IOException {
      return new GitCommandHandler();
    }

    @Bean
    public PublishedStudyService publishedStudyService() {
      return mock(PublishedStudyService.class);
    }

    @Bean
    public FileStoreService fsService() {
      return mock(GridFsService.class);
    }

    @Bean
    public FileSystemService fileSystemService() {
      return mock(FileSystemService.class);
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
      return mock(AttachmentRepository.class);
    }

    @Bean
    public AttachmentStateRepository attachmentStateRepository() {
      return mock(AttachmentStateRepository.class);
    }

    @Bean
    public GridFsOperations gridFsOperations() {
      return mock(GridFsOperations.class);
    }

    @Bean
    public EventBus eventBus() {
      return mock(EventBus.class);
    }

    @Override
    protected String getDatabaseName() {
      return "mica-test";
    }

    @Override
    public Mongo mongo() throws IOException {
      return MongodForTestsFactory.with(Version.Main.PRODUCTION).newMongo();
    }

    @Override
    @Bean
    public CustomConversions customConversions() {
      return new CustomConversions(
          Lists.newArrayList(new MongoDbConfiguration.LocalizedStringWriteConverter(),
              new MongoDbConfiguration.LocalizedStringReadConverter()));
    }

    @Override
    protected String getMappingBasePackage() {
      return "org.obiba.mica";
    }

  }

}

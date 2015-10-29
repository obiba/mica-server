package org.obiba.mica.dataset.service;

import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.obiba.opal.rest.client.magma.RestDatasource;

import com.google.common.eventbus.EventBus;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StudyDatasetServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private StudyDatasetService studyDatasetService;

  @Mock
  private StudyService studyService;

  @Mock
  private OpalService opalService;

  @Mock
  private StudyDatasetRepository studyDatasetRepository;

  @Mock
  private EventBus eventBus;

  @Mock
  private GitService gitService;

  @Mock
  private StudyDatasetStateRepository studyDatasetStateRepository;

  private Study study;

  private StudyDataset dataset;

  private StudyDatasetState state;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    study = buildStudy();
    dataset = buildStudyDataset();
    state = buildStudyDatasetState(dataset);
    doNothing().when(gitService).save(any(StudyDataset.class), anyString());
    when(gitService.hasGitRepository(any(StudyDataset.class))).thenReturn(true);
  }

  @Test
  public void testDatasourceConnectionErrorIsIgnoredForDraft() {
    RestDatasource r = mock(RestDatasource.class);
    when(r.getValueTable(anyString())).thenThrow(new MagmaRuntimeException());
    when(opalService.getDatasource(anyString(), anyString())).thenReturn(r);
    when(studyService.findDraft(anyString())).thenReturn(study);
    when(studyDatasetStateRepository.findOne(anyString())).thenReturn(state);

    studyDatasetService.save(dataset);
  }

  @Test
  public void testInvalidValueTableInDataset() {
    RestDatasource r = mock(RestDatasource.class);
    when(r.getValueTable(anyString())).thenThrow(NoSuchValueTableException.class);
    when(opalService.getDatasource(anyString(), anyString())).thenReturn(r);
    when(studyService.findDraft(anyString())).thenReturn(study);
    when(studyDatasetStateRepository.findOne(anyString())).thenReturn(state);
    dataset.setPublished(true);

    exception.expect(InvalidDatasetException.class);

    studyDatasetService.save(dataset);
  }

  private StudyDataset buildStudyDataset() {
    StudyDataset ds = new StudyDataset();
    StudyTable st = new StudyTable();
    st.setProject("proj");
    st.setTable("tab");
    ds.setStudyTable(st);
    ds.setName(new LocalizedString(Locale.CANADA, "test"));

    return ds;
  }

  private StudyDatasetState buildStudyDatasetState(StudyDataset dataset) {
    StudyDatasetState state = new StudyDatasetState();
    state.setId(dataset.getId());

    return state;
  }

  private Study buildStudy() {
    Study s = new Study();
    s.setOpal("opal");

    return s;
  }
}

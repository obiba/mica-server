package org.obiba.mica.study.search;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;

public class StudyIndexConfigurationTest {

  @Test
  public void test() throws IOException {
    XContentBuilder mapping = XContentFactory.jsonBuilder()
      .startObject()
        .startObject(StudyIndexer.STUDY_TYPE)
        .endObject()
      .endObject();
    System.out.println(mapping.string());
  }

}

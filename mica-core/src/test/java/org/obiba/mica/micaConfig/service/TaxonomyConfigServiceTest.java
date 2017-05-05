package org.obiba.mica.micaConfig.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.util.Maps;
import org.junit.Test;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class TaxonomyConfigServiceTest {

  @Test(expected = VocabularyDuplicateAliasException.class)
  public void validateTaxonomyDuplicateAlias() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        null,
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc").build()));

    taxonomy.addVocabulary(
      createVocabulary("voc002",
        null,
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc").build()));

    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test(expected = VocabularyDuplicateAliasException.class)
  public void validateTaxonomyDuplicateAliasRange() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").build()));

    taxonomy.addVocabulary(
      createVocabulary("voc002",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").build()));

    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test
  public void validateRangeTaxonomyWithTerms() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test(expected = VocabularyMissingRangeTermsException.class)
  public void validateRangeTaxonomyWithoutTerms() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
      null,
      AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").range("true").type("integer").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test
  public void validateRangeTaxonomyWithoutTermsAndRange() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
      null,
      AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").type("integer").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  @Test(expected = VocabularyMissingRangeAttributeException.class)
  public void validateRangeTaxonomyWithoutRangeAttribute() {
    Taxonomy taxonomy = new Taxonomy("tax001");
    taxonomy.addVocabulary(
      createVocabulary("voc001",
        createTerms("term001", "term002"),
        AttributeBuilder.newBuilder().field("tax.voc").alias("tax-voc-range").type("integer").build()));
    new TaxonomyConfigService().validateTaxonomy(taxonomy);
  }

  private Vocabulary createVocabulary(String name, List<Term> terms, Map<String,String> attributes) {
    Vocabulary vocabulary = new Vocabulary(name);
    vocabulary.setTitle(LocalizedString.en(name + "-title"));
    vocabulary.setDescription(LocalizedString.en(name + "-desc"));
    if (terms != null) vocabulary.setTerms(terms);
    if (attributes != null) vocabulary.setAttributes(attributes);
    return vocabulary;
  }

  private List<Term> createTerms(String... names) {
    return Arrays.stream(names).map(n -> createTerm(n, n +"-title", n + "-desc")).collect(Collectors.toList());
  }

  private Term createTerm(String name, String title, String desc) {
    Term term = new Term(name);
    term.setTitle(LocalizedString.en(title));
    term.setDescription(LocalizedString.en(desc));
    return term;
  }

  private static class AttributeBuilder {
    private Map<String, String> attributes = Maps.newHashMap();

    static AttributeBuilder newBuilder() {
      return new AttributeBuilder();
    }

    AttributeBuilder field(String value) {
      attributes.put("field", value);
      return this;
    }

    AttributeBuilder alias(String value) {
      attributes.put("alias", value);
      return this;
    }

    AttributeBuilder type(String value) {
      attributes.put("type", value);
      return this;
    }

    AttributeBuilder range(String value) {
      attributes.put("range", value);
      return this;
    }

    public Map<String, String> build() {
      return attributes;
    }
  }
}

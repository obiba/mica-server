/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.rql;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.elasticsearch.common.Strings;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

import sun.util.locale.LanguageTag;

public class RqlFieldResolver {

  private static final String TAXO_SEPARATOR = ".";

  private static final String DEFAULT_TAXO_PREFIX = "Mica_";

  private final List<Taxonomy> taxonomies;

  private final String defaultTaxonomyName;

  private final String locale;

  public RqlFieldResolver(List<Taxonomy> taxonomies, String locale) {
    this.taxonomies = taxonomies;
    this.locale = locale;
    defaultTaxonomyName = taxonomies.stream().filter(t -> t.getName().startsWith(DEFAULT_TAXO_PREFIX))
      .map(TaxonomyEntity::getName).findFirst().orElse("");
  }

  private String localize(Vocabulary vocabulary, String field, String locale) {
    boolean hasTerms = vocabulary != null && vocabulary.hasTerms();

    if (!hasTerms) {
      Pattern pattern = Pattern.compile("\\." + LanguageTag.UNDETERMINED + "$");
      Matcher matcher = pattern.matcher(field);

      if (matcher.find()) {
        field = field.replace(LanguageTag.UNDETERMINED, locale);
      }

      return field + ".analyzed";
    }

    return field;
  }

  public FieldData resolveField(String rqlField) {
    String field = rqlField;

    // normalize field name
    if(!field.contains(TAXO_SEPARATOR)) {
      field = defaultTaxonomyName + TAXO_SEPARATOR + field;
    }

    int idx = field.indexOf(TAXO_SEPARATOR);
    if(idx < 1) return FieldData.newBuilder().field(rqlField).build();

    FieldData data = resolveFieldByVocabulary(field.substring(0, idx), field.substring(idx + 1, field.length()));

    return data == null ? FieldData.newBuilder().field(rqlField).build() : data;
  }

  @Nullable
  public FieldData resolveFieldByVocabulary(String taxonomyName, String vocabularyName) {
    String field = null;
    FieldData.Builder builder = FieldData.newBuilder();
    Optional<Taxonomy> taxonomy = taxonomies.stream().filter(t -> t.getName().equals(taxonomyName)).findFirst();
    if(taxonomy.isPresent() && taxonomy.get().hasVocabularies()) {
      builder.taxonomy(taxonomy.get());
      Optional<Vocabulary> vocabulary = taxonomy.get().getVocabularies().stream()
        .filter(v -> v.getName().equals(vocabularyName)).findFirst();
      if(vocabulary.isPresent()) {
        builder.vocabulary(vocabulary.get());
        String f = vocabulary.get().getAttributeValue("field");
        if(!Strings.isNullOrEmpty(f)) field = localize(vocabulary.get(), f, locale);
        else field = localize(vocabulary.get(), vocabulary.get().getName(), locale);
      } else {
        field = localize(null, vocabularyName, locale);
      }
    }
    return field == null ? null : builder.field(field).build();
  }

  public List<Taxonomy> getTaxonomies() {
    return taxonomies;
  }

  static class FieldData {
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_DECIMAL = "decimal";

    private Taxonomy taxonomy;
    private Vocabulary vocabulary;
    private String field;

    static Builder newBuilder() {
      return new Builder();
    }

    public Taxonomy getTaxonomy() {
      return taxonomy;
    }

    public Vocabulary getVocabulary() {
      return vocabulary;
    }

    public String getField() {
      return field;
    }

    public boolean isNumeric() {
      String type = getType();
      return TYPE_INTEGER.equals(type) || TYPE_DECIMAL.equals(type);
    }

    public boolean isRange() {
      return vocabulary != null && vocabulary.hasTerms() && isNumeric();
    }

    public String getType() {
      if (vocabulary == null) return TYPE_STRING;
      String type = vocabulary.getAttributeValue("type");
      return Strings.isNullOrEmpty(type)? TYPE_STRING : type.toLowerCase();
    }

    static class Builder {
      FieldData data = new FieldData();

      Builder taxonomy(Taxonomy value) {
        data.taxonomy = value;
        return this;
      }

      Builder vocabulary(Vocabulary value) {
        data.vocabulary = value;
        return this;
      }

      Builder field(String value) {
        data.field = value;
        return this;
      }

      FieldData build() {
        return data;
      }

    }
  }
}

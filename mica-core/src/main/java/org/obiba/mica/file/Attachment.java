package org.obiba.mica.file;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.LocalizedString;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document
public class Attachment extends AbstractAuditableDocument implements AttributeAware {

  private static final long serialVersionUID = 7881381748865114007L;

  @JsonIgnore
  private boolean justUploaded;

  @NotNull
  private String name;

  private String type;

  private LocalizedString description;

  private Locale lang;

  private long size;

  private String md5;

  private Attributes attributes = new Attributes();

  @JsonIgnore
  private String path;

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public Locale getLang() {
    return lang;
  }

  public void setLang(Locale lang) {
    this.lang = lang;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public boolean isJustUploaded() {
    return justUploaded;
  }

  public void setJustUploaded(boolean justUploaded) {
    this.justUploaded = justUploaded;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;

    if(obj == null || getClass() != obj.getClass()) return false;

    return Objects.equals(getId(), ((Attachment) obj).getId());
  }

  @Override
  public void addAttribute(Attribute attribute) {
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    attributes.removeAttribute(attribute);
  }

  @Override
  public void removeAllAttributes() {
    attributes.removeAllAttributes();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes.hasAttribute(attName, namespace);
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void setAttributes(@NotNull Attributes attributes) {
    this.attributes = attributes;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}

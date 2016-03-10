package org.obiba.mica.study.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.jvnet.hk2.internal.Collector;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.PersonAware;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.support.UidGenerator;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.date.PersitableYear;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static java.util.stream.Collectors.toList;

/**
 * A Study.
 */
public class Study extends AbstractGitPersistable implements AttributeAware, PersonAware {

  private static final long serialVersionUID = 6559914069652243954L;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private Attachment logo;

  private List<Person> investigators = Lists.newArrayList();

  private List<Person> contacts = Lists.newArrayList();

  private Map<String, List<Membership>> memberships = new HashMap<String, List<Membership>>() {
    {
      put(Membership.CONTACT, Lists.newArrayList());
      put(Membership.INVESTIGATOR, Lists.newArrayList());
    }
  };

  private LocalizedString objectives;

  @URL
  private String website;

  private Authorization specificAuthorization;

  private Authorization maelstromAuthorization;

  private StudyMethods methods;

  private NumberOfParticipants numberOfParticipants;

  private PersitableYear start;

  private PersitableYear end;

  private List<String> access;

  private LocalizedString otherAccess;

  private String markerPaper;

  //TODO add pubmedId validator
  //@PubmedId
  private String pubmedId;

  @Transient
  private List<Attachment> attachments = Lists.newArrayList();

  private LocalizedString info;

  private SortedSet<Population> populations = Sets.newTreeSet();

  @URL
  private String opal;

  private Attributes attributes;

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getAcronym() {
    return acronym;
  }

  public void setAcronym(LocalizedString acronym) {
    this.acronym = acronym;
  }

  public Attachment getLogo() {
    return logo;
  }

  public boolean hasLogo() {
    return logo != null;
  }

  public void setLogo(Attachment logo) {
    this.logo = logo;
  }

  @JsonIgnore
  public List<Person> getInvestigators() {
    if(!investigators.isEmpty()) {
      setInvestigators(investigators);
      investigators.clear();
    }

    return memberships.getOrDefault(Membership.INVESTIGATOR, Lists.newArrayList()).stream().map(m -> m.getPerson())
      .collect(toList());
  }

  @JsonProperty
  public void setInvestigators(List<Person> investigators) {
    if(investigators == null) investigators = Lists.newArrayList();

    replaceExisting(investigators);

    memberships.put(Membership.INVESTIGATOR,
      investigators.stream().map(p -> new Membership(p, Membership.INVESTIGATOR)).collect(toList()));
  }

  @JsonProperty("dceIds")
  public List<String> getDataColelctionEventIds() {
    List<String> dceIds = Lists.newArrayList();

    if (populations != null) {
      populations.forEach(p -> p.getDataCollectionEvents()
        .forEach(d -> dceIds.add(UidGenerator.getUId(Arrays.asList(getId(), p.getId(), d.getId())))));
    }

    return dceIds;
  }

  @JsonIgnore
  public List<Person> getContacts() {
    if(!contacts.isEmpty()) {
      setContacts(contacts);
      contacts.clear();
    }

    return memberships.getOrDefault(Membership.CONTACT, Lists.newArrayList()).stream().map(m -> m.getPerson())
      .collect(toList());
  }

  @Override
  public void addToPerson(Membership membership) {
    membership.getPerson().addStudy(this, membership.getRole());
  }

  @Override
  public void removeFromPerson(Membership membership) {
    membership.getPerson().removeStudy(this, membership.getRole());
  }

  @Override
  public void removeFromPerson(Person person) {
    person.removeStudy(this);
  }

  @JsonProperty
  public void setContacts(List<Person> contacts) {
    if(contacts == null) contacts = Lists.newArrayList();

    replaceExisting(contacts);

    memberships
      .put(Membership.CONTACT, contacts.stream().map(p -> new Membership(p, Membership.CONTACT)).collect(toList()));
  }

  public LocalizedString getObjectives() {
    return objectives;
  }

  public void setObjectives(LocalizedString objectives) {
    this.objectives = objectives;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
    if(!Strings.isNullOrEmpty(website) && !website.startsWith("http")) {
      this.website = "http://" + website;
    }
  }

  public String getOpal() {
    return opal;
  }

  public void setOpal(String opal) {
    this.opal = opal;
  }

  public Authorization getSpecificAuthorization() {
    return specificAuthorization;
  }

  public void setSpecificAuthorization(Authorization specificAuthorization) {
    this.specificAuthorization = specificAuthorization;
  }

  public Authorization getMaelstromAuthorization() {
    return maelstromAuthorization;
  }

  public void setMaelstromAuthorization(Authorization maelstromAuthorization) {
    this.maelstromAuthorization = maelstromAuthorization;
  }

  public StudyMethods getMethods() {
    return methods;
  }

  public void setMethods(StudyMethods methods) {
    this.methods = methods;
  }

  public NumberOfParticipants getNumberOfParticipants() {
    return numberOfParticipants;
  }

  public void setNumberOfParticipants(NumberOfParticipants numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  public Integer getStart() {
    return start == null ? null : start.getYear();
  }

  public void setStart(Integer value) {
    if(value == null) return;
    if(start == null) start = new PersitableYear();
    start.setYear(value);
  }

  public Integer getEnd() {
    return end == null ? null : end.getYear();
  }

  public void setEnd(Integer value) {
    if(value == null) return;
    if(end == null) end = new PersitableYear();
    end.setYear(value);
  }

  public List<String> getAccess() {
    return access;
  }

  public void addAccess(@NotNull String anAccess) {
    if(access == null) access = new ArrayList<>();
    access.add(anAccess);
  }

  public void setAccess(List<String> access) {
    this.access = access;
  }

  public LocalizedString getOtherAccess() {
    return otherAccess;
  }

  public void setOtherAccess(LocalizedString otherAccess) {
    this.otherAccess = otherAccess;
  }

  public String getMarkerPaper() {
    return markerPaper;
  }

  public void setMarkerPaper(String markerPaper) {
    this.markerPaper = markerPaper;
  }

  public String getPubmedId() {
    return pubmedId;
  }

  public void setPubmedId(String pubmedId) {
    this.pubmedId = pubmedId;
  }

  @Deprecated
  @JsonIgnore
  public List<Attachment> getAttachments() {
    return attachments;
  }

  @Deprecated
  @JsonProperty
  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments == null ? Lists.newArrayList() : attachments;
  }

  public LocalizedString getInfo() {
    return info;
  }

  public void setInfo(LocalizedString info) {
    this.info = info;
  }

  public SortedSet<Population> getPopulations() {
    return populations;
  }

  public void addPopulation(@NotNull Population population) {
    if(populations == null) populations = new TreeSet<>();
    if(population.isNew()) {
      String newId = population.getName().asAcronym().asString().toLowerCase();
      if(hasPopulation(newId)) {
        for(int i = 1; i < 1000; i++) {
          if(!hasPopulation(newId + "_" + i)) {
            population.setId(newId + "_" + i);
            break;
          }
        }
      } else population.setId(newId);
    }
    populations.add(population);
  }

  public boolean hasPopulation(String populationId) {
    if(populations == null) return false;
    for(Population population : populations) {
      if(population.getId().equals(populationId)) return true;
    }
    return false;
  }

  public boolean hasPopulations() {
    return populations != null && !populations.isEmpty();
  }

  public void setPopulations(SortedSet<Population> newPopulations) {
    if(newPopulations == null) {
      // during serialization input can be null
      populations = newPopulations;
      return;
    }

    // make sure we don't keep old entries
    populations = new TreeSet<>();
    newPopulations.forEach(this::addPopulation);
  }

  /**
   * For each {@link org.obiba.mica.core.domain.Person} and investigators: trim strings, make sure institution is
   * not repeated in contact name etc.
   */
  public void cleanContacts() {
    cleanContacts(contacts);
    cleanContacts(investigators);
  }

  public Set<String> membershipRoles() {
    return this.memberships.keySet();
  }

  private void cleanContacts(List<Person> contactList) {
    if(contactList == null) return;
    contactList.forEach(Person::cleanPerson);
  }

  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name);
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attributes == null) attributes = new Attributes();
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    if(attributes != null) {
      attributes.removeAttribute(attribute);
    }
  }

  @Override
  public void removeAllAttributes() {
    if(attributes != null) attributes.removeAllAttributes();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes != null && attributes.hasAttribute(attName, namespace);
  }

  @Override
  public String pathPrefix() {
    return "studies";
  }

  @Override
  public Map<String, Serializable> parts() {
    Study self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  @Override
  public List<Person> getAllPersons() {
    return getMemberships().values().stream().flatMap(List::stream).map(Membership::getPerson).distinct()
      .collect(toList());
  }

  @Override
  public List<Membership> getAllMemberships() {
    return getMemberships().values().stream().flatMap(List::stream).collect(toList());
  }

  public Map<String, List<Membership>> getMemberships() {
    if(!contacts.isEmpty()) {
      setContacts(contacts);
      contacts.clear();
    }

    if(!investigators.isEmpty()) {
      setInvestigators(investigators);
      investigators.clear();
    }

    return memberships;
  }

  public void setMemberships(Map<String, List<Membership>> memberships) {
    this.memberships = memberships;
    Map<String, Person> seen = Maps.newHashMap();

    this.memberships.entrySet().forEach(e -> e.getValue().forEach(m -> {
      if(seen.containsKey(m.getPerson().getId())) {
        m.setPerson(seen.get(m.getPerson().getId()));
      } else if(!m.getPerson().isNew()) {
        seen.put(m.getPerson().getId(), m.getPerson());
      }
    }));
  }

  private void replaceExisting(List<Person> persons) {
    List<Person> existing = this.memberships.values().stream().flatMap(List::stream).map(Membership::getPerson)
      .collect(toList());

    ImmutableList.copyOf(persons).forEach(p -> {
      if(existing.contains(p)) {
        int idx = persons.indexOf(p);
        persons.remove(p);
        persons.add(idx, existing.get(existing.indexOf(p)));
      }
    });
  }

  public List<Person> removeRole(String role) {
    List<Membership> members = this.memberships.getOrDefault(role, Lists.newArrayList());
    this.memberships.remove(role);
    return members.stream().map(m -> {
      m.getPerson().removeStudy(this, role);
      return m.getPerson();
    }).collect(toList());
  }

  public static class StudyMethods implements Serializable {

    private static final long serialVersionUID = 5984119393358199672L;

    private List<String> designs;

    private LocalizedString otherDesign;

    private LocalizedString followUpInfo;

    private List<String> recruitments;

    private LocalizedString otherRecruitment;

    private LocalizedString info;

    public List<String> getDesigns() {
      return designs;
    }

    public void addDesign(@NotNull String design) {
      if(designs == null) designs = new ArrayList<>();
      designs.add(design);
    }

    public void setDesigns(List<String> designs) {
      this.designs = designs;
    }

    public LocalizedString getOtherDesign() {
      return otherDesign;
    }

    public void setOtherDesign(LocalizedString otherDesign) {
      this.otherDesign = otherDesign;
    }

    public LocalizedString getFollowUpInfo() {
      return followUpInfo;
    }

    public void setFollowUpInfo(LocalizedString followUpInfo) {
      this.followUpInfo = followUpInfo;
    }

    public List<String> getRecruitments() {
      return recruitments;
    }

    public void addRecruitment(@NotNull String recruitment) {
      if(recruitments == null) recruitments = new ArrayList<>();
      recruitments.add(recruitment);
    }

    public void setRecruitments(List<String> recruitments) {
      this.recruitments = recruitments;
    }

    public LocalizedString getOtherRecruitment() {
      return otherRecruitment;
    }

    public void setOtherRecruitment(LocalizedString otherRecruitment) {
      this.otherRecruitment = otherRecruitment;
    }

    public LocalizedString getInfo() {
      return info;
    }

    public void setInfo(LocalizedString info) {
      this.info = info;
    }
  }

}

<!-- Datasets page macros -->

<!-- Datasets in table model templates -->
<#macro datasetTableHeadModel>
  <tr>
    <th><@message "acronym"/></th>
    <th><@message "name"/></th>
    <th><@message "description"/></th>
    <#if showTypeColumn>
      <th><@message "type"/></th>
    </#if>
  </tr>
</#macro>

<#macro datasetTableRowModel dataset>
  <tr>
    <td><a href="${contextPath}/dataset/${dataset.id}">${localize(dataset.acronym)}</a></td>
    <td><small>${localize(dataset.name)}</small></td>
    <td class="marked"><template><small>${localize(dataset.description)?trim?truncate_w(100, "...")}</small></template></td>
    <#if showTypeColumn>
      <td>
        <#if dataset.class.simpleName == "HarmonizationDataset">
          <@message "harmonized"/>
        <#else>
          <@message "collected"/>
        </#if>
      </td>
    </#if>
  </tr>
</#macro>

<!-- Datasets in lines model template -->
<#macro datasetLineModel dataset>
  <div class="col-lg-3 col-sm-12">
    <#if dataset.class.simpleName == "HarmonizationDataset">
      <div class="text-black-50 text-center mt-5">
        <i class="${harmoDatasetIcon} fa-3x"></i>
      </div>
    <#else>
      <div class="text-black-50 text-center mt-4">
        <i class="${datasetIcon} fa-3x"></i>
      </div>
    </#if>
  </div>
  <div class="col-lg-9 col-sm-12">
    <h2 class="lead"><b>${localize(dataset.acronym)}</b></h2>
    <p class="text-muted text-sm">${localize(dataset.name)}</p>
    <div class="marked">
      <template>${localize(dataset.description)?trim?truncate_w(200, "...")}</template>
    </div>
    <div class="mt-2">
      <a href="${contextPath}/dataset/${dataset.id}" class="btn btn-sm btn-outline-info">
        <@message "global.read-more"/>
      </a>
    </div>
  </div>
</#macro>

<!-- Datasets in cards model template -->
<#macro datasetCardModel>

  <!-- Macro variables -->
  <#if !type??>
    <#assign className = "Study,HarmonizationStudy">
  <#elseif type == "Harmonized">
    <#assign className = "HarmonizationStudy">
  <#else>
    <#assign className = "Study">
  </#if>

<div v-show="loading" class="spinner-border spinner-border-sm" role="status"></div>
<div v-show="!loading && entities && entities.length > 0" v-cloak></div>
<div id="datasets-card" class="card card-primary card-outline">

  <div class="card-header">
    <div class="row">
      <div class="col-3">
        <h3 class="card-title pt-1">{{total | localize-number}} <@message "datasets"/></h3>
      </div>
      <div class="col-6">
        <typeahead @typing="onType" @select="onSelect" :items="suggestions" :external-text="initialFilter"></typeahead>
      </div>
      <#if searchDatasetListDisplay>
        <div class="col-3">
          <a href="/search#lists?type=datasets&query=study(in(Mica_study.className,(${className})))" class="btn btn-sm btn-primary float-right">
            <@message "global.search"/> <i class="fas fa-search"></i>
          </a>
        </div>
      </#if>
    </div>
  </div><!-- /.card-header -->

  <div class="card-body pb-0">
    <div class="row">
      <div class="col-12">
        <div class="d-inline-flex float-right">
          <sorting @sort-update="onSortUpdate" :initial-choice="initialSort" :options-translations="sortOptionsTranslations"></sorting>
          <span class="ml-2 mr-1">
                    <select class="custom-select" id="obiba-page-size-selector-top"></select>
                  </span>
          <nav id="obiba-pagination-top" aria-label="Top pagination" class="mt-0">
            <ul class="pagination mb-0"></ul>
          </nav>
        </div>
      </div>
    </div>
  </div>

  <div class="card-body pb-0">
    <div class="tab-pane">
      <div class="row d-flex align-items-stretch">
        <div class="col-md-12 col-lg-6 d-flex align-items-stretch" v-for="dataset in entities" v-bind:key="dataset.id">
          <div v-if="dataset.id === ''" class="card w-100">
            <div class="card-body pt-0 bg-light">
            </div>
          </div>
          <div v-else class="card w-100">
            <div class="card-body">
              <div class="row h-100">
                <div class="col-12">
                  <h4 class="lead">
                    <a v-bind:href="'${contextPath}/dataset/' + dataset.id" class="mt-2">
                      <b>{{dataset.name | localize-string}}</b>
                    </a>
                  </h4>
                  <span class="marked"><small :inner-html.prop="dataset.description | localize-string | ellipsis(300) | markdown"></small></span>
                </div>
              </div>
            </div>
            <div class="card-footer py-1">
              <div class="row pt-1 row-cols-3">
                <template v-if="hasStats(dataset)">
                  <stat-item
                          v-bind:count="dataset['obiba.mica.CountStatsDto.datasetCountStats'].networks"
                          v-bind:singular="'<@message "network"/>'"
                          v-bind:plural="'<@message "networks"/>'"
                          v-bind:url="networks(dataset.id)">
                  </stat-item>
                  <stat-item
                          v-bind:count="dataset['obiba.mica.CountStatsDto.datasetCountStats'].studies"
                          v-bind:singular="'<@message "study"/>'"
                          v-bind:plural="'<@message "studies"/>'"
                          v-bind:url="studies(dataset.id)">
                  </stat-item>
                  <stat-item
                          v-bind:count="dataset['obiba.mica.CountStatsDto.datasetCountStats'].variables"
                          v-bind:singular="'<@message "variable"/>'"
                          v-bind:plural="'<@message "variables"/>'"
                          v-bind:url="variables(dataset.id)">
                  </stat-item>
                </template>
                <template v-else>
                  <!-- HACK used 'datasetsWithVariables' with opacity ZERO to have the same height as the longest stat item -->
                  <a href="url" class="btn btn-sm btn-link col text-left" style="opacity: 0">
                    <span class="h6 pb-0 mb-0 d-block">0</span>
                    <span class="text-muted"><small><@message "analysis.empty"/></small></span>
                  </a>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="d-inline-flex card-body pt-0 ml-auto">
            <span class="mr-1">
                <select class="custom-select" id="obiba-page-size-selector-bottom"></select>
            </span>
    <nav id="obiba-pagination-bottom" aria-label="Bottom pagination" class="mt-0">
      <ul class="pagination"></ul>
    </nav>
  </div>
</div>
</#macro>

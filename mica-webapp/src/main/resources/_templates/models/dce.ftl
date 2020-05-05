<!-- DCE macros -->

<!-- DCE model -->
<#macro dceModel dce>
  <dl class="row">
    <#if dce.model.dataSources?? && dce.model.dataSources?size != 0>
      <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.description"/>">
        <@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.title"/>
      </dt>
      <dd class="col-sm-8">
        <ul class="pl-3">
          <#list dce.model.dataSources as item>
            <li>
              <#assign txt = "study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.term." + item + ".title"/>
              <@message txt/>
              <#if item == "others" && dce.model.otherDataSources??>
                : ${localize(dce.model.otherDataSources)}
              </#if>
            </li>
          </#list>
        </ul>
      </dd>
    </#if>

    <#if dce.model.bioSamples?? && dce.model.bioSamples?size != 0>
      <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-bioSamples.description"/>">
          <@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-bioSamples.title"/>
      </dt>
      <dd class="col-sm-8">
        <ul class="pl-3">
          <#list dce.model.bioSamples as item>
            <li>
              <#assign txt = "study_taxonomy.vocabulary.populations-dataCollectionEvents-bioSamples.term." + item + ".title"/>
              <@message txt/>
              <#if item == "tissues" && dce.model.tissueTypes??>
                : ${localize(dce.model.tissueTypes)}
              <#elseif item == "others" && dce.model.otherBioSamples??>
                : ${localize(dce.model.otherBioSamples)}
              </#if>
            </li>
          </#list>
        </ul>
      </dd>
    </#if>

    <#if dce.model.administrativeDatabases?? && dce.model.administrativeDatabases?size != 0>
      <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-administrativeDatabases.description"/>">
          <@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-administrativeDatabases.title"/>
      </dt>
      <dd class="col-sm-8">
        <ul class="pl-3">
          <#list dce.model.administrativeDatabases as item>
            <li>
              <#assign txt = "study_taxonomy.vocabulary.populations-dataCollectionEvents-administrativeDatabases.term." + item + ".title"/>
              <@message txt/>
            </li>
          </#list>
        </ul>
      </dd>
    </#if>
  </dl>
</#macro>

<!-- DCE modal dialog -->
<#macro dceDialog id dce>
  <div class="modal fade" id="modal-${id}">
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">${localize(dce.name)}</h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3 marked">
            ${localize(dce.description)}
          </div>
          <dl class="row">
            <#if dce.start??>
              <dt class="col-sm-4">
                <@message "start-date"/>
              </dt>
              <dd class="col-sm-8">
                <div>${dce.start.yearMonth!""}</div>
              </dd>
            </#if>
            <#if dce.end??>
              <dt class="col-sm-4">
                <@message "end-date"/>
              </dt>
              <dd class="col-sm-8">
                <div>${dce.end.yearMonth!""}</div>
              </dd>
            </#if>
          </dl>
            <@dceModel dce=dce/>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>

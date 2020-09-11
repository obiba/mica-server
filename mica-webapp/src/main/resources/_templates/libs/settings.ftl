<!-- Date and Datetime formats -->
<#assign datetimeFormat = "yyyy-MM-dd HH:mm"/>
<#assign dateFormat = "yyyy-MM-dd"/>

<!-- Favicon -->
<#assign faviconPath = "${contextPath}/favicon.ico"/>

<!-- Icons -->
<#assign networkIcon = "io io-network"/>
<#assign studyIcon = "io io-study"/>
<#assign datasetIcon = "io io-dataset"/>
<#assign harmoDatasetIcon = "io io-harmo-dataset"/>
<#assign variableIcon = "io io-variable"/>
<#assign projectIcon = "io io-project"/>
<#assign taxonomyIcon = "io io-taxonomy"/>

<!-- Assets location -->
<#assign assetsPath = "${contextPath}/assets"/>

<!-- Branding -->
<#assign brandImageSrc = "${assetsPath}/images/mica-logo.png"/>
<#assign brandImageClass = "img-circle elevation-3"/>
<#assign brandTextEnabled = true/>
<#assign brandTextClass = "font-weight-light"/>

<!-- Theme -->
<#assign adminLTEPath = "${assetsPath}/libs/node_modules/admin-lte"/>

<!-- Home page settings -->
<#assign networksLink = "${contextPath}/networks"/>
<!--#assign networksLink = "${contextPath}/search#lists?type=networks"/-->
<#assign studiesLink = "${contextPath}/studies"/>
<!--#assign studiesLink = "${contextPath}/search#lists?type=studies"/-->
<#assign datasetsLink = "${contextPath}/datasets"/>
<!--#assign datasetsLink = "${contextPath}/search#lists?type=datasets"/-->
<#assign portalLink = "${config.portalUrl!contextPath}" + "/"/>

<!-- List pages -->
<#assign listDefaultDisplay = "cards"/> <!-- cards or table -->
<#assign networkListDefaultDisplay = listDefaultDisplay/>
<#assign studyListDefaultDisplay = listDefaultDisplay/>
<#assign datasetListDefaultDisplay = listDefaultDisplay/>

<!-- Search page -->
<#if config?? && config.singleStudyEnabled>
  <#assign defaultSearchState = "#lists?type=variables"/>
<#else>
  <#assign defaultSearchState = "#lists?type=studies"/>
</#if>

<!-- Data Access pages -->
<#assign dataAccessInstructionsEnabled = true/>
<#assign dataAccessCalloutsEnabled = true/>

<!-- Charts -->
<#assign barChartBackgroundColor = "rgb(54, 162, 235)"/>
<#assign barChartBorderColor = "#3c8dbc"/>
<#assign colors = ["#f56954", "#00a65a", "#f39c12", "#00c0ef", "#3c8dbc", "#d2d6de", "#007bff", "#6610f2", "#20c997", "#6f42c1", "#e83e8c", "#dc3545", "#fd7e14", "#ffc107", "#28a745",  "#17a2b8"]>

<!-- Files -->
<#assign showFiles = true/>
<#assign showNetworkFiles = showFiles/>
<#assign showStudyFiles = showFiles/>
<#assign showStudyDCEFiles = showStudyFiles/>
<#assign showDatasetFiles = showFiles/>

<!-- Variables classifications charts -->
<!-- if taxonomies list is null or empty, no variables classifications is showned -->
<#assign variablesClassificationsTaxonomies = ["Mlstr_area"]/>
<#assign networkVariablesClassificationsTaxonomies = variablesClassificationsTaxonomies/>
<#assign studyVariablesClassificationsTaxonomies = variablesClassificationsTaxonomies/>
<#assign datasetVariablesClassificationsTaxonomies = variablesClassificationsTaxonomies/>

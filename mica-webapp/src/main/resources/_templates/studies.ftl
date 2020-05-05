<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "studies"/></title>
</head>
<body id="studies-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Template variables -->
  <#if !type??>
    <#assign title = "studies">
    <#assign callout = "studies-callout">
    <#assign showTypeColumn = true>
  <#elseif type == "Harmonization">
    <#assign title = "harmonization-studies">
    <#assign callout = "harmonization-studies-callout">
    <#assign showTypeColumn = false>
  <#else>
    <#assign title = "individual-studies">
    <#assign callout = "individual-studies-callout">
    <#assign showTypeColumn = false>
  </#if>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header title=title breadcrumb=[["..", "home"], [title]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p><@message callout/></p>
        </div>

        <div class="row">
          <div class="col-lg-12">
            <#if studies?? && studies?size != 0>
              <div class="card card-info card-outline">

                <div class="card-header d-flex p-0">
                  <h3 class="card-title p-3"><@message "studies"/></h3>
                  <ul class="nav nav-pills ml-auto p-2">
                    <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "table">active</#if>" href="#table" data-toggle="tab">
                        <i class="fas fa-grip-lines"></i></a>
                    </li>
                    <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "cards">active</#if>" href="#cards" data-toggle="tab">
                        <i class="fas fa-grip-horizontal"></i></a>
                    </li>
                  </ul>
                </div><!-- /.card-header -->


                <div class="card-body">
                  <div class="tab-content">
                    <div class="tab-pane <#if datasetListDefaultDisplay == "table">active</#if>" id="table">
                      <table id="studies" class="table table-bordered table-striped">
                        <thead>
                        <tr>
                          <th><@message "acronym"/></th>
                          <th><@message "name"/></th>
                          <th><@message "description"/></th>
                          <#if showTypeColumn>
                            <th><@message "type"/></th>
                          </#if>
                        </tr>
                        </thead>
                        <tbody>
                        <#list studies as std>
                          <tr>
                            <td><a href="../study/${std.id}">${localize(std.acronym)}</a></td>
                            <td><small>${localize(std.name)}</small></td>
                            <td><small>${localize(std.objectives)?trim?truncate_w(100, "...")}</small></td>
                            <#if showTypeColumn>
                              <td>
                                <#if std.class.simpleName == "HarmonizationStudy">
                                  <@message "harmonization"/>
                                <#else>
                                  <@message "individual"/>
                                </#if>
                              </td>
                            </#if>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>

                    <div class="tab-pane <#if datasetListDefaultDisplay == "cards">active</#if>" id="cards">
                      <div class="row d-flex align-items-stretch">
                        <#list studies as std>
                          <div class="col-12 col-sm-6 col-md-4 d-flex align-items-stretch">
                            <div class="card bg-light w-100">
                              <div class="card-header text-dark border-bottom-0">
                                <h2 class="lead"><b>${localize(std.acronym)}</b></h2>
                              </div>
                              <div class="card-body pt-0">
                                <div class="row">
                                  <div class="col-7">
                                    <p class="text-muted text-sm">${localize(std.name)}</p>
                                  </div>
                                  <div class="col-5 text-center">
                                      <#if std.logo??>
                                        <img class="img-fluid" style="max-height: 200px" alt="${localize(std.acronym)} logo" src="../ws/study/${std.id}/file/${std.logo.id}/_download"/>
                                      <#else >
                                        <p class="text-black-50 text-center mr-5 ml-5 pr-5">
                                          <i class="${studyIcon} fa-3x"></i>
                                        </p>
                                      </#if>
                                  </div>
                                </div>
                              </div>
                              <div class="card-footer">
                                <div class="text-right">
                                  <a href="../study/${std.id}" class="btn btn-sm btn-outline-info">
                                    <i class="fas fa-eye"></i> ${localize(std.acronym)}
                                  </a>
                                </div>
                              </div>
                            </div>
                          </div>
                        </#list>
                      </div>
                    </div>
                  </div>

                </div>
              </div>
            </#if>
          </div>
        </div>
        <!-- /.row -->

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

    <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<script>
    $(function () {
        $("#studies").DataTable(dataTablesDefaultOpts);
    });
</script>
</body>
</html>

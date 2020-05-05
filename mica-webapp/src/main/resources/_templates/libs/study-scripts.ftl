<!-- Timeline -->
<script src="../bower_components/d3/d3.js"></script>
<script src="../bower_components/mica-study-timeline/dist/mica-study-timeline.js"></script>
<script>
    $(function () {
        let options = dataTablesDefaultOpts;
        options.columnDefs = [
          { "type": "num", "targets": 0, "visible": false }
        ];
        <#list study.populations as pop>
        $("#population-${pop.id}-dces").DataTable(options);
        </#list>

        micajs.stats('studies', { query: "study(in(Mica_study.id,${study.id}))" }, function(stats) {
            $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
            $('#dataset-hits').text(new Intl.NumberFormat().format(stats.datasetResultDto.totalHits));
            $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
        });

        <#if timelineData??>
        let timelineData = ${timelineData};
        new $.MicaTimeline(new $.StudyDtoParser('${.lang}')).create('#timeline', timelineData).addLegend();
        </#if>
    });
</script>

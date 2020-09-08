<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-variable.js"></script>

<#if user?? || !config.variableSummaryRequiresAuthentication>
<script>
  $(function () {

    <#if type == "Dataschema">
      micajs.variable.harmonizations('${variable.id}', function(data) {
        console.dir(data);
        $('#loadingHarmonizedVariables').hide();
        const harmonizedVariablesTableBody = $('#harmonizedVariables > tbody');
        if (data.datasetVariableSummaries) {
          for (const harmonizedVariable of data.datasetVariableSummaries) {
            const status = micajs.harmo.status(harmonizedVariable);
            const statusDetail = micajs.harmo.statusDetail(harmonizedVariable);
            const comment = micajs.harmo.comment(harmonizedVariable);
            const baseStudyTable = harmonizedVariable.studyTable ? harmonizedVariable.studyTable : harmonizedVariable.harmonizationStudyTable;
            const population = micajs.study.population(baseStudyTable.studySummary, baseStudyTable.populationId);
            const dce = population ? micajs.study.populationDCE(population, baseStudyTable.dataCollectionEventId) : undefined;
            let dceName = population ? localizedString(population.name) : "";
            if (dce) {
              dceName = dceName + ' -- ' + localizedString(dce.name);
            }
            harmonizedVariablesTableBody.append('<tr>' +
                    '<td title=""><a href="../variable/' + harmonizedVariable.resolver.id + '">' + harmonizedVariable.resolver.name + '</a> ' + localizedString(baseStudyTable.name) + '' +
                    '<div class="text-muted">' + localizedString(baseStudyTable.description) + '</div>' +
                    '</td>' +
                    '<td><a href="../study/' + baseStudyTable.studyId + '">' + localizedString(baseStudyTable.studySummary.acronym) + '</a></td>' +
                    '<td>' + dceName + '</td>' +
                    '<td><i class=" ' + micajs.harmo.statusClass(localizedString(status)) + '"></i></td>' +
                    '<td>' + localizedString(statusDetail) + '</td>' +
                    '<td>' + localizedString(comment) + '</td>' +
                    '</tr>')
          }
          $('#harmonizedVariables').show();
        } else {
          $('#noHarmonizedVariables').show();
        }
      }, function (data) {
        $('#loadingHarmonizedVariables').hide();
        $('#noHarmonizedVariables').show();
      });
    </#if>

    micajs.variable.aggregation('${variable.id}', function(data) {
      $('#loadingSummary').hide();

      $('#n').html(data.total);
      $('#n-values').html(data.n);
      $('#n-missings').html(data.total - data.n);
      $('#counts').show();

      if (data.frequencies) {
        // frequencies chart
        const frequencyChartElem = $('#frequencyChart');
        const chartCanvas = frequencyChartElem.get(0).getContext('2d');
        const backgroundColors = ['${colors?join("', '")}'];
        new Chart(chartCanvas, makeVariableFrequenciesChartSettings(data.frequencies, backgroundColors));
        frequencyChartElem.show();

        // frequencies table
        let frequencyRows = '';
        data.frequencies.forEach(frequency => {
          const pct = data.n === 0 ? 0 : (frequency.count / data.n) * 100;
          frequencyRows = frequencyRows +
                  '<tr>' +
                  '<td>' + frequency.value + '</td>' +
                  '<td>' + frequency.count + '</td>' +
                  '<td>' + pct.toFixed(2) + '</td>' +
                  '<td>' + (frequency.missing ? '<i class="fas fa-check"></i>' : '') + '</td>' +
                  '</tr>';
        });
        $('#frequencyValues').html(frequencyRows);

        const dataTableOpts = {
          "paging": false,
          "lengthChange": false,
          "searching": false,
          "ordering": true,
          "order": [[1, "desc"]],
          "info": false,
          "autoWidth": true,
          "language": {
            "url": "${assetsPath}/i18n/datatables.${.lang}.json"
          }
        };
        $("#frequencyTable").DataTable(dataTableOpts);

        $('#categoricalSummary').show();
      }

      if (data.statistics) {
        const summary = data.statistics;

        $('#mean').html(summary.n === 0 ? '-' : summary.mean.toFixed(2));
        $('#stdDev').html(summary.n === 0 ? '-' : summary.stdDeviation.toFixed(2));
        $('#variance').html(summary.n === 0 ? '-' : summary.variance.toFixed(2));
        $('#sum').html(summary.n === 0 ? '-' : summary.sum.toFixed(2));
        $('#sum-of-squares').html(summary.n === 0 ? '-' : summary.sumOfSquares.toFixed(2));
        $('#min').html(summary.n === 0 ? '-' : summary.min.toFixed(2));
        $('#max').html(summary.n === 0 ? '-' : summary.max.toFixed(2));

        if (data.intervalFrequencies) {
          // histogram chart
          const histogramChartElem = $('#histogramChart');
          const chartCanvas = histogramChartElem.get(0).getContext('2d');
          new Chart(chartCanvas, makeVariableHistogramChartSettings(data.intervalFrequencies, '${barChartBorderColor}', '${barChartBackgroundColor}'));
          histogramChartElem.show();
        }

        $('#continuousSummary').show();
      }
      if (!data.frequencies && !data.statistics) {
        $('#noSummary').show();
      }
    }, function (data) {
      $('#loadingSummary').hide();
      $('#noSummary').show();
    });
  });
</script>
</#if>

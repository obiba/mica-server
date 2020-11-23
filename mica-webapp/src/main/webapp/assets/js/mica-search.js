'use strict';

// an EventBus is a Vue app without element
// its data are callback functions, registered by event name
const EventBus = new Vue({
  data: {
    callbacks: {}
  },
  methods: {
    register: function (eventName, callback) {
      if (!this.callbacks[eventName]) {
        this.callbacks[eventName] = [];
        this.$on(eventName, function (payload) {
          for (let callback of this.callbacks[eventName]) {
            callback(payload);
          }
        });
      }
      this.callbacks[eventName].push(callback);
      //console.dir(this.callbacks)
    },
    unregister: function (eventName) {
      this.callbacks[eventName] = undefined;
    }
  }
});

// global translate filter for use in imported components
Vue.filter("translate", (key) => {
  let value = Mica.tr[key];
  return typeof value === "string" ? value : key;
});

Vue.filter("localize-string", (input) => {
  if (typeof input === "string") return input;
  return StringLocalizer.localize(input);
});

//
// Search criteria Vue
// sidebar menus for taxonomy selection. Terms selection is delegated to the main app (query builder)
//

/**
 * Taxonomy sidebar menu component
 */
Vue.component('taxonomy-menu', {
  props: ['taxonomy'],
  template: `
  <li class="nav-item">
      <a href="#"
        class="nav-link"
        data-toggle="modal"
        data-target="#taxonomy-modal"
        :title="taxonomy.description | localize-string"
        @click.prevent="$emit('taxonomy-selection', taxonomy.name)"><i class="far fa-circle nav-icon"></i><p>{{ taxonomy.title | localize-string }}</p>
      </a>
  </li>
`
});

new Vue({
  el: '#search-criteria',
  data() {
    return {
      criteriaMenu: {
        items: {
          variable: {
            icon: Mica.icons.variable,
            title: Mica.tr.variables,
            menus: []
          },
          dataset: {
            icon: Mica.icons.dataset,
            title: Mica.tr.datasets,
            menus: []
          },
          study: {
            icon: Mica.icons.study,
            title: Mica.tr.studies,
            menus: []
          },
          network: {
            icon: Mica.icons.network,
            title: Mica.tr.networks,
            menus: []
          },
        },
        order: []
      }
    };
  },
  methods: {
    // make the menu
    onMicaTaxonomies: function (payload) {
      // sort and include configured targets
      const visibleMenus = Mica.display.searchCriteriaMenus || [];
      const filteredTargets = payload.filter(p => visibleMenus.indexOf(p.name) > -1);
      filteredTargets.sort((a, b) => {
        const ai = visibleMenus.indexOf(a.name);
        const bi = visibleMenus.indexOf(b.name);
        return ai - bi;
      });

      for (let target of filteredTargets) {
        this.criteriaMenu.items[target.name].title = StringLocalizer.localize(target.title);
        switch (target.name) {
          case 'variable':
            let level = target.terms[0].terms;
            const theRest = target.terms.slice(1);

            if (theRest.length > 0) {
              this.criteriaMenu.items.variable.menus = level.concat(theRest);
            } else {
              this.criteriaMenu.items.variable.menus = level;
            }
            break;
          case 'dataset':
          case 'study':
          case 'network':
            this.criteriaMenu.items[target.name].menus = target.terms;
            break;
        }
        if (this.criteriaMenu.items[target.name].menus && this.criteriaMenu.items[target.name].menus.length > 0) {
          this.criteriaMenu.order.push(target.name);
        }
      }
    },
    // forward taxonomy selection
    onTaxonomySelection: function (payload, target) {
      EventBus.$emit('taxonomy-selection', {target, taxonomyName: payload});
    }
  },
  mounted() {
    EventBus.register('mica-taxonomy', this.onMicaTaxonomies);
  }
});

//
// Results Vue
// results display app, with some filtering criteria selection, and requests for query execution
//

/**
 * Component used to filter results by Study className vocabulary
 */
const StudyFilterShortcutComponent = Vue.component('study-filter-shortcut', {
  name: 'StudyFilterShortcut',
  template: `
  <div v-if="visible && showFilter">
    <div class="btn-group" role="group" aria-label="Basic example">
      <button type="button" v-bind:class="{active: selection.all}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('all')">{{tr('all')}}</button>
      <button type="button" v-bind:class="{active: selection.study}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('study')">{{tr('individual')}}</button>
      <button type="button" v-bind:class="{active: selection.harmonization}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('harmonization')">{{tr('harmonization')}}</button>
    </div>
  </div>
  `,
  data() {
    return {
      selection: {all: true, study: false, harmonization: false},
      visible: true
    }
  },
  computed: {
    showFilter: () => Mica.config.isCollectedDatasetEnabled
      && Mica.config.isHarmonizedDatasetEnabled
      && !Mica.config.isSingleStudyEnabled
  },
  methods: {
    tr(key) {
      return Mica.tr[key]
    },
    buildClassNameArgs(key) {
      switch (key) {
        case 'study':
          return 'Study';

        case 'harmonization':
          return 'HarmonizationStudy';
      }

      return ['Study', 'HarmonizationStudy'];
    },
    onLocationChanged(payload) {
      this.selection = MicaTreeQueryUrl.getStudyTypeSelection(payload.tree);
      this.visible = DISPLAYS.GRAPHICS !== payload.display;
    },
    onSelectionClicked(selectionKey) {
      const classNameQuery = new RQL.Query('in', ['Mica_study.className', this.buildClassNameArgs(selectionKey)]);
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATE, {target: 'study', query: classNameQuery});
      EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
    }
  },
  mounted() {
    EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));
  },
  beforeDestory() {
    EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
  }
});

const DataTableDefaults = {
  searching: false,
  ordering: false,
  lengthMenu: [10, 20, 50, 100],
  pageLength: 20,
  dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>"
};

class StringLocalizer {
  static __localizeInternal(entries, locale) {
    const result = (Array.isArray(entries) ? entries : [entries]).filter((entry) => entry && (locale === entry.lang || locale === entry.locale)).pop();

    if (result) {
      let value = result.value ? result.value : result.text;
      return value ? value : null;
    }
    return null;
  }

  static localize(entries) {
    if (entries) {
      const result = StringLocalizer.__localizeInternal(entries, Mica.locale)
        || StringLocalizer.__localizeInternal(entries, Mica.defaultLocale)
        || StringLocalizer.__localizeInternal(entries, 'und');

      return result ? result : '';
    } else {
      return '';
    }
  }
}

class TaxonomyTitleFinder {
  initialize(taxonomies) {
    this.taxonomies = taxonomies;
  }

  title(taxonomyName, vocabularyName, termName) {
    if (taxonomyName) {
      const taxonomy = this.taxonomies[taxonomyName];
      if (taxonomy) {
        if (!vocabularyName && !termName) return StringLocalizer.localize(taxonomy.title);
        else if (vocabularyName) {
          let foundVocabulary = (taxonomy.vocabularies || []).filter(vocabulary => vocabulary.name === vocabularyName)[0];

          if (foundVocabulary) {
            if (!termName) return StringLocalizer.localize(foundVocabulary.title);
            else {
              let foundTerm = (foundVocabulary.terms || []).filter(term => term.name === termName)[0];

              if (foundTerm) return StringLocalizer.localize(foundTerm.title);
            }
          }
        }
      }
    }

    return null;
  }
}
const taxonomyTitleFinder  = new TaxonomyTitleFinder();

class MicaQueryAlertListener {
  constructor() {
    EventBus.register(EVENTS.QUERY_ALERT, this.__onQueryAlery.bind(this));
  }

  __getTaxonomyVocabularyNames(query) {
    const parts = (query.name === 'match' ? query.args[1] : query.args[0]).split(/\./);
    return parts.length === 2 ? {taxonomy: parts[0], vocabulary: parts[1]} : {};
  }

  __onQueryAlery(payload) {
    const target = Mica.tr[payload.target];
    const query = payload.query || {};
    const taxonomyInfo = ["and", "or"].indexOf(query.name) === -1 ? this.__getTaxonomyVocabularyNames(query) : undefined;
    const message = Mica.trArgs(
      `criterion.${payload.action}`,
      [taxonomyInfo ? taxonomyTitleFinder.title(taxonomyInfo.taxonomy, taxonomyInfo.vocabulary) : "", target]
    );

    if (message) {
      MicaService.toastSuccess(message);
    }
  }
}
const queryAlertListener  = new MicaQueryAlertListener();

/**
 * Component for all results related functionality
 */
const ResultsTabContent = {
  el: '#results-tab-content',
  components: {
    'study-filter-shortcut': StudyFilterShortcutComponent
  },
  data() {
    const subAgg = {
      agg: 'model-numberOfParticipants-participant-number',
      dataKey: 'obiba.mica.StatsAggregationResultDto.stats',
      title: Mica.tr['participants']
    };

    const chartOptions = {
      'geographical-distribution-chart': {

        id: 'geographical-distribution-chart',
        title: Mica.tr['geographical-distribution-chart-title'],
        text: Mica.tr['geographical-distribution-chart-text'],
        type: 'choropleth',
        borderColor: Mica.charts.borderColor,
        agg: 'populations-model-selectionCriteria-countriesIso',
        vocabulary: 'populations-selectionCriteria-countriesIso',
        dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
        parseForChart: function(chartData) {
          let labels = [];
          let data = [];

          let states;
          let featureFinder = function(key) {
            return states.filter(state => state.id === key).pop();
          };
          if (['world'].includes(Mica.map.name)) {
            states = ChartGeo.topojson.feature(Mica.map.topo, Mica.map.topo.objects.countries1).features;
          } else {
            states = ChartGeo.topojson.feature(Mica.map.topo, Mica.map.topo.objects.collection).features;
          }
          chartData.filter(term => term.count>0).forEach(term => {
            labels.push(term.title);
            data.push({
              feature: featureFinder(term.key),
              value: term.count
            });
          });

          return [labels, {
            outline: states,
            data: data
          }];
        },
        options: {
          showOutline: true,
            showGraticule: false,
            legend: {
            display: false
          },
          scale: {
            projection: 'mercator'//'equalEarth'//'naturalEarth1'
          },
          geo: {
            colorScale: {
              display: true,
            },
          }
        }
      },
      'study-design-chart': {
        id: 'study-design-chart',
        title: Mica.tr['study-design-chart-title'],
        text: Mica.tr['study-design-chart-text'],
        type: 'horizontalBar',
        backgroundColor: Mica.charts.backgroundColor,
        borderColor: Mica.charts.borderColor,
        agg: 'model-methods-design',
        vocabulary: 'methods-design',
        dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
        subAgg
      },
      'number-participants-chart': {
        id: 'number-participants-chart',
        title: Mica.tr['number-participants-chart-title'],
        text: Mica.tr['number-participants-chart-text'],
        type: 'doughnut',
        backgroundColor: Mica.charts.backgroundColors,
        borderColor: Mica.charts.borderColor,
        agg: 'model-numberOfParticipants-participant-number-range',
        vocabulary: 'numberOfParticipants-participant-range',
        dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
        subAgg,
        legend: {
        display: true,
          position: 'right',
          align: 'start',
        }
      },
      'bio-samples-chart': {
        id: 'bio-samples-chart',
        title: Mica.tr['bio-samples-chart-title'],
        text: Mica.tr['bio-samples-chart-text'],
        type: 'horizontalBar',
        backgroundColor: Mica.charts.backgroundColor,
        borderColor: Mica.charts.borderColor,
        agg: 'populations-dataCollectionEvents-model-bioSamples',
        vocabulary: 'populations-dataCollectionEvents-bioSamples',
        dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
      },
      'study-start-year-chart': {
        id: 'study-start-year-chart',
        title: Mica.tr['study-start-year-chart-title'],
        text: Mica.tr['study-start-year-chart-text'],
        type: 'horizontalBar',
        backgroundColor: Mica.charts.backgroundColor,
        borderColor: Mica.charts.borderColor,
        agg: 'model-startYear-range',
        vocabulary: 'start-range',
        dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
        subAgg
      }
    }

    return {
      counts: {},
      hasVariableQuery: false,
      hasGraphicsResult: false,
      selectedBucket: BUCKETS.study,
      dceChecked: false,
      bucketTitles: {
        study: Mica.tr.study,
        dataset: Mica.tr.dataset,
        dce: Mica.tr['data-collection-event'],
      },
      chartOptions: Mica.charts.chartIds.map(id => chartOptions[id]),
      canDoFullCoverage: false,
      hasCoverageTermsWithZeroHits: false,
      queryForFullCoverage: null,
      queriesWithZeroHitsToRemove: []
    }
  },
  methods: {
    onSelectResult(type, target) {
      EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {display: DISPLAYS.LISTS, type, target});
    },
    onSelectSearch() {
      EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {display: DISPLAYS.LISTS});
    },
    onSelectCoverage() {
      EventBus.$emit(EVENTS.QUERY_TYPE_COVERAGE, {display: DISPLAYS.COVERAGE});
    },
    onSelectGraphics() {
      EventBus.$emit(EVENTS.QUERY_TYPE_GRAPHICS, {type: TYPES.STUDIES, display: DISPLAYS.GRAPHICS});
    },
    onSelectBucket(bucket) {
      console.debug(`onSelectBucket : ${bucket} - ${this.dceChecked}`);
      this.selectedBucket = bucket;
      EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {bucket});
    },
    onGraphicsResult(payload) {
      this.hasGraphicsResult = payload.response.studyResultDto.totalHits > 0;
    },
    onResult(payload) {
      const data = payload.response;
      this.counts = {
        variables: "0",
        datasets: "0",
        studies: "0",
        networks: "0",
      };

      if (data && data.variableResultDto && data.variableResultDto.totalHits) {
        this.counts.variables = data.variableResultDto.totalHits.toLocaleString();
      }

      if (data && data.datasetResultDto && data.datasetResultDto.totalHits) {
        this.counts.datasets = data.datasetResultDto.totalHits.toLocaleString();
      }

      if (data && data.studyResultDto && data.studyResultDto.totalHits) {
        this.counts.studies = data.studyResultDto.totalHits.toLocaleString();
      }

      if (data && data.networkResultDto && data.networkResultDto.totalHits) {
        this.counts.networks = data.networkResultDto.totalHits.toLocaleString();
      }
    },
    onCoverageResult(payload) {
      if (payload.response.rows !== undefined) { // for filters
        let rowsEligibleForFullCoverage = [];
        payload.response.rows.forEach(row => {
          if (Array.isArray(row.hits)) {
            if (row.hits.filter(hit => hit === 0).length === 0) {
              rowsEligibleForFullCoverage.push(row);
            }
          }
        });

        // filter for full coverage
        let coverageVocabulary = this.selectedBucket.startsWith('dce') ? 'dceId' : 'id';

        let coverageArgs = ['Mica_' + fromBucketToTarget(this.selectedBucket) + '.' + coverageVocabulary];
        coverageArgs.push(rowsEligibleForFullCoverage.map(selection => selection.value));

        const numberOfTerms = payload.response.termHeaders.length;

        this.canDoFullCoverage = rowsEligibleForFullCoverage.length > 0 && rowsEligibleForFullCoverage < numberOfTerms; // active?

        if (this.canDoFullCoverage) {
          this.queryForFullCoverage = new RQL.Query('in', coverageArgs);
        }

        // filter for subdomains with variables
        const taxonomyNames = Array(numberOfTerms), vocabularyNames = Array(numberOfTerms);
        let lastTaxonomyHeaderIndex = 0, lastVocabularyHeaderIndex = 0;
        payload.response.taxonomyHeaders.forEach(taxonomyHeader => {
          const name = taxonomyHeader.entity.name, termsCount = taxonomyHeader.termsCount;

          taxonomyNames.fill(name, lastTaxonomyHeaderIndex, lastTaxonomyHeaderIndex + termsCount);
          lastTaxonomyHeaderIndex += termsCount;
        });

        payload.response.vocabularyHeaders.forEach(vocabularyHeader => {
          const name = vocabularyHeader.entity.name, termsCount = vocabularyHeader.termsCount;

          vocabularyNames.fill(name, lastVocabularyHeaderIndex, lastVocabularyHeaderIndex + termsCount);
          lastVocabularyHeaderIndex += termsCount;
        });

        this.queriesWithZeroHitsToRemove = [];
        payload.response.termHeaders.forEach((termHeader, index) => {
          const key = taxonomyNames[index] + '.' + vocabularyNames[index], name = termHeader.entity.name;

          if (termHeader.hits === 0) {
            this.queriesWithZeroHitsToRemove.push(new RQL.Query('in', [key, [name]]));
          }
        });

        this.hasCoverageTermsWithZeroHits = this.queriesWithZeroHitsToRemove.length > 0; // active?
      }
    },
    onZeroColumnsToggle() {
      this.queriesWithZeroHitsToRemove.forEach(query => {
        EventBus.$emit(EVENTS.QUERY_TYPE_DELETE, {target: TARGETS.VARIABLE, query, display: DISPLAYS.COVERAGE});
      });
    },
    onFullCoverage() {
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATE, {target: fromBucketToTarget(this.selectedBucket), query: this.queryForFullCoverage, display: DISPLAYS.COVERAGE});
    },
    onLocationChanged: function (payload) {
      $(`.nav-pills #${payload.display}-tab`).tab('show');
      $(`.nav-pills #${payload.type}-tab`).tab('show');

      if (payload.bucket) {
        this.selectedBucket = TARGET_ID_BUCKET_MAP[payload.bucket];
        const tabPill = [TARGET_ID_BUCKET_MAP.studyId, TARGET_ID_BUCKET_MAP.dceId].indexOf(this.selectedBucket) > -1
          ? TARGET_ID_BUCKET_MAP.studyId
          : TARGET_ID_BUCKET_MAP.datasetId;
        this.dceChecked = TARGET_ID_BUCKET_MAP.dceId === this.selectedBucket;
        $(`.nav-pills #bucket-${tabPill}-tab`).tab('show');
      }

      const targetQueries = MicaTreeQueryUrl.getTreeQueries();
      this.hasVariableQuery = TARGETS.VARIABLE in targetQueries && targetQueries[TARGETS.VARIABLE].args.length > 0;
    }
  },
  beforeMount() {
    EventBus.register('variables-results', this.onResult.bind(this));
    EventBus.register('datasets-results', this.onResult.bind(this));
    EventBus.register('studies-results', this.onResult.bind(this));
    EventBus.register('networks-results', this.onResult.bind(this));
    EventBus.register('coverage-results', this.onCoverageResult.bind(this));
    EventBus.register(EVENTS.QUERY_TYPE_GRAPHICS_RESULTS, this.onGraphicsResult.bind(this));
    EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));
  },
  beforeDestory() {
    EventBus.unregister('variables-results', this.onResult);
    EventBus.unregister('datasets-results', this.onResult);
    EventBus.unregister('studies-results', this.onResult);
    EventBus.unregister('networks-results', this.onResult);
    EventBus.unregister('coverage-results', this.onResult);
    EventBus.unregister(EVENTS.QUERY_TYPE_GRAPHICS_RESULTS, this.onGraphicsResult);
    EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
  }
};

/**
 * Registering plugins defined in VueMicaSearch
 */
Vue.use(VueMicaSearch, {
  mixin: {
    methods: {
      getEventBus: () => EventBus,
      getMicaConfig: () => Mica.config,
      getLocale: () => Mica.locale,
      getDisplayOptions: () => Mica.display,
      normalizePath: (path) => {
        return contextPath + path;
      },
      localize: (entries) => StringLocalizer.localize(entries),
      registerDataTable: (tableId, options) => {
        const mergedOptions = Object.assign(options, DataTableDefaults);
        mergedOptions.language = {
          url: contextPath + '/assets/i18n/datatables.' + Mica.locale + '.json'
        };
        const dTable = $('#' + tableId).DataTable(mergedOptions);
        dTable.on('draw', function() {
          // bs tooltip
          $('[data-toggle="tooltip"]').tooltip();
        });

        // checkboxes only for variables
        if ('vosr-variables-result' === tableId) {
          initSelectDataTable(dTable, options);
        }

        return dTable;
      }
    }
  }
});

/**
 * Querybuilder Vue
 *
 * Main app that orchestrates the query display, criteria selection, query execution and dispatch of the results
 *
 */
new Vue({
  el: '#query-builder',
  data() {
    return {
      taxonomies: {},
      targets: [],
      message: '',
      selectedTaxonomy: null,
      selectedTaxonomyTitle: null,
      selectedTarget: null,
      queryType: 'variables-list',
      lastList: '',
      queryExecutor: new MicaQueryExecutor(EventBus, DataTableDefaults.pageLength),
      queries: null,
      noQueries: true,
      queryToCopy: null,
      queryToCart: null,
      newVariableSetName: '',
      variableSets: [],
      advanceQueryMode: false,
      downloadUrlObject: '',
      variableSelections: []
    };
  },
  methods: {
    refreshQueries() {
      this.queries = MicaTreeQueryUrl.getTreeQueries();
      this.noQueries = true;
      if (this.queries) {
        for (let key of [TARGETS.VARIABLE, TARGETS.DATASET, TARGETS.STUDY, TARGETS.NETWORK]) {
          let target = this.queries[key];
          if (target && target.args && target.args.length > 0) {
            this.noQueries = false;
            break;
          }
        }
      }
    },
    getTaxonomyForTarget(target) {
      let result = [];

      if (TARGETS.VARIABLE === target) {
        let taxonomies = [];
        for (let taxonomy in this.taxonomies) {
          if (taxonomy === `Mica_${target}` || !taxonomy.startsWith('Mica_')) {
            const found = this.taxonomies[taxonomy];
            if (found) taxonomies.push(found);
          }
        }

        result.push(taxonomies);
      } else {
        let taxonomy = this.taxonomies[`Mica_${target}`];
        result.push(taxonomy);
      }

      return result[0];
    },
    // show a modal with all the vocabularies/terms of the selected taxonomy
    // initialized by the query terms and update/trigger the query on close
    onTaxonomySelection: function (payload) {
      this.selectedTaxonomy = this.taxonomies[payload.taxonomyName];
      this.selectedTarget = payload.target;

      let selectedTaxonomyVocabulariesTitle = '';
      if (this.selectedTaxonomy) {
        this.selectedTaxonomyTitle = this.selectedTaxonomy.title;
        selectedTaxonomyVocabulariesTitle = this.selectedTaxonomy.vocabularies.map(voc => voc.title[0].text).join(', ');
      } else {
        const foundTaxonomyGroup = this.findTaxonomyGroup(payload.taxonomyName, payload.target);
        this.selectedTaxonomy = foundTaxonomyGroup.taxonomies;
        this.selectedTaxonomyTitle = foundTaxonomyGroup.title;
      }

      this.message = '[' + payload.taxonomyName + '] ' + this.selectedTaxonomyTitle[0].text + ': ';
      this.message = this.message + selectedTaxonomyVocabulariesTitle;
    },
    findTaxonomyGroup: function (taxonomyName, target) {
      let found = {};

      const foundTarget = this.targets.filter(it => it.name === target)[0];
      let foundTaxonomyGroup = foundTarget.terms.filter(it => it.name === taxonomyName)[0];

      if (foundTaxonomyGroup) {
        found.title = foundTaxonomyGroup.title;
        let taxonomies = [];
        foundTaxonomyGroup.terms.forEach(term => {
          const taxonomy = this.taxonomies[term.name];
          if (taxonomy) {
            taxonomies.push(taxonomy);
          }
        });

        found.taxonomies = taxonomies;
      }

      return found;
    },
    onExecuteQuery: function () {
      console.debug('Executing ' + this.queryType + ' query ...');
      EventBus.$emit(this.queryType, 'I am the result of a ' + this.queryType + ' query');
    },
    onLocationChanged: function (payload) {
      this.downloadUrlObject = MicaTreeQueryUrl.getDownloadUrl(payload);

      let tree = MicaTreeQueryUrl.getTree();

      // query string to copy
      tree.findAndDeleteQuery((name) => 'limit' === name);
      this.queryToCopy = tree.serialize();

      // query string for adding variables to cart
      let vQuery = tree.search((name) => name === 'variable');
      if (!vQuery) {
        vQuery = new RQL.Query('variable',[]);
        tree.addQuery(null, vQuery);
      }
      tree.addQuery(vQuery, new RQL.Query('limit', [0, 100000]));
      tree.addQuery(vQuery, new RQL.Query('fields', ['variableType']));
      this.queryToCart = tree.serialize();

      this.refreshQueries();
    },
    onQueryUpdate(payload) {
      console.debug('query-builder update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [payload]});

      EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
    },
    onQueryRemove(payload) {
      console.debug('query-builder update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_DELETE, payload);

      EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
    },
    onNodeUpdate(payload) {
      console.debug('query-builder node update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [payload]});

      EventBus.$emit(EVENTS.CLEAR_RESULTS_SELECTIONS);
    },
    onCopyQuery() {
      navigator.clipboard.writeText(this.queryToCopy);
    },
    onAddToCart() {
      const onsuccess = function(cart, oldCart) {
        VariablesSetService.showCount('#cart-count', cart, Mica.locale);
        if (cart.count === oldCart.count) {
          MicaService.toastInfo(Mica.tr['no-variable-added']);
        } else {
          MicaService.toastSuccess(Mica.tr['variables-added-to-cart'].replace('{0}', (cart.count - oldCart.count).toLocaleString(Mica.locale)));
        }
      };

      if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
        VariablesSetService.addToCart(this.variableSelections, onsuccess);
      } else {
        VariablesSetService.addQueryToCart(this.queryToCart, onsuccess);
      }
    },
    onAddToSet(setId) {
      const onsuccess = (set, oldSet) => {
        if (set.count === oldSet.count) {
          MicaService.toastInfo(Mica.tr['no-variable-added-set'].replace('{{arg0}}', '"' + set.name + '"'));
        } else {
          MicaService.toastSuccess(Mica.tr['variables-added-to-set'].replace('{0}', (set.count - oldSet.count).toLocaleString(Mica.locale)).replace('{1}', '"' + set.name + '"'));
        }

        this.newVariableSetName = '';
        this.reloadSetsList();
      }

      if (setId || (this.newVariableSetName && this.newVariableSetName.length > 0)) {
        if (Array.isArray(this.variableSelections) && this.variableSelections.length > 0) {
          VariablesSetService.addToSet(setId, this.newVariableSetName, this.variableSelections, onsuccess);
        } else {
          VariablesSetService.addQueryToSet(setId, this.newVariableSetName, this.queryToCart, onsuccess);
        }
      }
    },
    onDownloadQueryResult() {
      if (this.downloadUrlObject) {
        const form = document.createElement('form');
        form.setAttribute('class', 'hidden');
        form.setAttribute('method', 'post');

        form.action = this.downloadUrlObject.url;
        form.accept = 'text/csv';

        const input = document.createElement('input');
        input.name = 'query';
        input.value = this.downloadUrlObject.query;
        form.appendChild(input);

        document.body.appendChild(form);
        form.submit();
        form.remove();
      } else {
        MicaService.toastError(Mica.tr['no-coverage-available']);
      }
    },
    onSearchModeToggle() {
      this.advanceQueryMode = !this.advanceQueryMode;
    },
    reloadSetsList() {
      VariablesSetService.getSets(data => {
        if (Array.isArray(data)) {
          this.variableSets = data.filter(set => set.name);
          document.getElementById('list-count').innerHTML = this.variableSets.length;
        }
      }, response => {

      });
    }
  },
  computed: {
    selectedQuery() {
      if (this.selectedTarget) {
        return this.queries[this.selectedTarget];
      }

      return undefined;
    },
    numberOfSetsRemaining() {
      return Mica.maxNumberOfSets - (this.variableSets || []).length;
    }
  },
  beforeMount() {
    console.debug('Before mounted QueryBuilder');
    this.queryExecutor.init();
  },
  mounted() {
    console.debug('Mounted QueryBuilder');
    EventBus.register('taxonomy-selection', this.onTaxonomySelection);
    EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));

    EventBus.register(EVENTS.CLEAR_RESULTS_SELECTIONS, () => this.variableSelections = []);

    for (const typeKey in TYPES) {
      EventBus.register(`${TYPES[typeKey]}-selections-updated`, payload => this.variableSelections = payload.selections || []);
    }

    // fetch the configured search criteria, in the form of a taxonomy of taxonomies
    axios
      .get(contextPath + '/ws/taxonomy/Mica_taxonomy/_filter?target=taxonomy')
      .then(response => {
        this.targets = response.data.vocabularies;
        EventBus.$emit('mica-taxonomy', this.targets);

        const targetQueries = [];

        for (let target of this.targets) {
          // then load the taxonomies
          targetQueries.push(`${contextPath}/ws/taxonomies/_filter?target=${target.name}`);
        }

        return axios.all(targetQueries.map(query => axios.get(query))).then(axios.spread((...responses) => {
          responses.forEach((response) => {
            for (let taxo of response.data) {
              this.taxonomies[taxo.name] = taxo;
            }
          });

          this.refreshQueries();

          taxonomyTitleFinder.initialize(this.taxonomies);

          Vue.filter("taxonomy-title", (input) => {
            const [taxonomy, vocabulary, term] = input.split(/\./);
            return  taxonomyTitleFinder.title(taxonomy, vocabulary, term) || input;
          });

          // Delay ResultsTabContent to ensure taxonomy translations
          new Vue(ResultsTabContent);

          // Emit 'query-type-selection' to pickup a URL query to be executed; if nothing found a Variable query is executed
          EventBus.$emit(EVENTS.QUERY_TYPE_SELECTION, {});

          return this.taxonomies;
        }));
      });

    const targetQueries = MicaTreeQueryUrl.getTreeQueries();

    let advancedNodeCount = 0;
    for (const target in targetQueries) {
      let advancedOperator = target === TARGETS.VARIABLE ? 'and' : 'or';
      const targetQuery = targetQueries[target];
      if (targetQuery) {
        new RQL.QueryTree(targetQuery).visit(query => {
          if (query.name === advancedOperator) {
            advancedNodeCount++;
          }
        });
      }
    }
    this.advanceQueryMode = advancedNodeCount > 0;

    // don't close sets' dropdown when clicking inside of it
    if (this.$refs.listsDropdownMenu) {
      this.$refs.listsDropdownMenu.addEventListener("click", event => event.stopPropagation());
    }

    this.reloadSetsList();
    this.onExecuteQuery();
  },
  beforeDestory() {
    console.debug('Before destroy query builder');
    EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
    EventBus.unregister('taxonomy-selection', this.onTaxonomySelection);
    EventBus.unregister(EVENTS.QUERY_TYPE_SELECTION, this.onQueryTypeSelection);
    this.queryExecutor.destroy();
  }
});

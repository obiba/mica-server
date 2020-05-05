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
      for (let target of payload) {
        this.criteriaMenu.items[target.name].title = StringLocalizer.localize(target.title);
        switch (target.name) {
          case 'variable':
            // TODO handle multi level
            this.criteriaMenu.items.variable.menus = target.terms[0].terms;
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
      console.dir(payload);
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
  <div v-if="showFilter">
    <div class="btn-group" role="group" aria-label="Basic example">
      <button type="button" v-bind:class="{active: selection.all}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('all')">{{tr('all')}}</button>
      <button type="button" v-bind:class="{active: selection.study}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('study')">{{tr('individual')}}</button>
      <button type="button" v-bind:class="{active: selection.harmonization}" class="btn btn-sm btn-info" v-on:click="onSelectionClicked('harmonization')">{{tr('harmonization')}}</button>
    </div>
  </div>
  `,
  data() {
    return {
      selection: {all: true, study: false, harmonization: false}
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
    isAll(values) {
      return !values || Array.isArray(values) && values.length > 1;
    },
    isClassName(name, values) {
      return Array.isArray(values) ? values.length === 1 && values.indexOf(name) > -1 : values === name;
    },
    onLocationChanged(payload) {
      const tree = payload.tree;
      const classNameQuery = tree.search((name, args) => args.indexOf('Mica_study.className') > -1);
      if (classNameQuery) {
        const values = classNameQuery.args[1];
        this.selection.all = this.isAll(values);
        this.selection.study = this.isClassName('Study', values);
        this.selection.harmonization = this.isClassName('HarmonizationStudy', values);
      } else {
        this.selection = {all: true, study: false, harmonization: false};
      }
    },
    onSelectionClicked(selectionKey) {
      const classNameQuery = new RQL.Query('in', ['Mica_study.className', this.buildClassNameArgs(selectionKey)]);
      EventBus.$emit('query-type-update', {target: 'study', query: classNameQuery});
    }
  },
  mounted() {
    EventBus.register('location-changed', this.onLocationChanged.bind(this));
  },
  beforeDestory() {
    EventBus.unregister('location-changed', this.onLocationChanged);
  }
});

const DataTableDefaults = {
  searching: false,
  ordering: false,
  lengthMenu: [10, 20, 50, 100],
  pageLength: 20,
  dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>"
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
    const result = StringLocalizer.__localizeInternal(entries, Mica.locale)
      || StringLocalizer.__localizeInternal(entries, 'en')
      || StringLocalizer.__localizeInternal(entries, 'und');

    return result ? result : "";
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


  /**
 * Registering plugins defined in VueObibaSearchResult
 */
Vue.use(VueObibaSearchResult, {
  mixin: {
    methods: {
      getEventBus: () => EventBus,
      getMicaConfig: () => Mica.config,
      getLocale: () => Mica.locale,
      localize: (entries) => StringLocalizer.localize(entries),
      registerDataTable: (tableId, options) => {
        const mergedOptions = Object.assign(options, DataTableDefaults);
        mergedOptions.language = {
          url: '/assets/i18n/datatables.' + Mica.locale + '.json'
        };
        return $('#' + tableId).DataTable(mergedOptions);
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
      message: '',
      selectedTaxonomy: null,
      selectedTarget: null,
      queryType: 'variables-list',
      lastList: '',
      queryExecutor: new MicaQueryExecutor(EventBus, DataTableDefaults.pageLength),
      queries: null,
      noQueries: true
    };
  },
  methods: {
    refreshQueries() {
      this.queries = MicaTreeQueryUrl.getTreeQueries();
      this.noQueries = true;
      if (this.queries) {
        for (let key of ['variable', 'dataset', 'study', 'network']) {
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

      this.message = '[' + payload.taxonomyName + '] ' + this.selectedTaxonomy.title[0].text + ': ';
      this.message = this.message + this.selectedTaxonomy.vocabularies.map(voc => voc.title[0].text).join(', ');
    },
    onExecuteQuery: function () {
      console.debug('Executing ' + this.queryType + ' query ...');
      EventBus.$emit(this.queryType, 'I am the result of a ' + this.queryType + ' query');
    },
    onLocationChanged: function () {
      this.refreshQueries();
    },
    onQueryUpdate(payload) {
      console.debug('query-builder update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_UPDATES_SELECTION, {updates: [payload]});
    },
    onQueryRemove(payload) {
      console.debug('query-builder update', payload);
      EventBus.$emit(EVENTS.QUERY_TYPE_DELETE, payload);
    }
  },
  computed: {
    selectedQuery() {
      if (this.selectedTarget) {
        return this.queries[this.selectedTarget];
      }

      return undefined;
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

    // fetch the configured search criteria, in the form of a taxonomy of taxonomies
    axios
      .get('/ws/taxonomy/Mica_taxonomy/_filter?target=taxonomy')
      .then(response => {
        let targets = response.data.vocabularies;
        EventBus.$emit('mica-taxonomy', targets);

        const targetQueries = [];

        for (let target of targets) {
          // then load the taxonomies

          targetQueries.push(`/ws/taxonomies/_filter?target=${target.name}`);
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


          // Emit 'query-type-selection' to pickup a URL query to be executed; if nothing found a Variable query is executed
          EventBus.$emit('query-type-selection', {});

          return this.taxonomies;
        }));
      });
    this.onExecuteQuery();
  },
  beforeDestory() {
    console.debug('Before destroy query builder');
    EventBus.unregister(EVENTS.LOCATION_CHANGED, this.onLocationChanged);
    EventBus.unregister('taxonomy-selection', this.onTaxonomySelection);
    EventBus.unregister('query-type-selection', this.onQueryTypeSelection);
    this.queryExecutor.destroy();
  }
});


/**
 * Component for all results related functionality
 */
new Vue({
  el: '#results-tab-content',
  components: {
    'study-filter-shortcut': StudyFilterShortcutComponent
  },
  data() {
    return {
      counts: {},
      hasVariableQuery: false,
      selectedBucket: BUCKETS.study,
      dceChecked: false,
      bucketTitles: {
        study: Mica.tr.study,
        dataset: Mica.tr.dataset,
        dce: Mica.tr['data-collection-event'],
      }
    }
  },
  methods: {
    onSelectResult(type, target) {
      EventBus.$emit('query-type-selection', {display: 'list', type, target});
    },
    onSelectSearch() {
      EventBus.$emit('query-type-selection', {display: DISPLAYS.LISTS});
    },
    onSelectCoverage() {
      EventBus.$emit('query-type-coverage', {display: DISPLAYS.COVERAGE});
    },
    onSelectGraphics() {
      EventBus.$emit('query-type-selection', {display: DISPLAYS.GRAPHICS});
    },
    onSelectBucket(bucket) {
      console.debug(`onSelectBucket : ${bucket} - ${this.dceChecked}`);
      this.selectedBucket = bucket;
      EventBus.$emit('query-type-selection', {bucket});
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
    onLocationChanged: function (payload) {
      $(`.nav-pills #${payload.display}-tab`).tab('show');
      $(`.nav-pills #${payload.type}-tab`).tab('show');
      if (payload.bucket) {
        this.selectedBucket = TAREGT_ID_BUCKET_MAP[payload.bucket];
        const tabPill = [TAREGT_ID_BUCKET_MAP.studyId, TAREGT_ID_BUCKET_MAP.dceId].indexOf(this.selectedBucket) > -1
          ? TAREGT_ID_BUCKET_MAP.studyId
          : TAREGT_ID_BUCKET_MAP.datasetId;
        this.dceChecked = TAREGT_ID_BUCKET_MAP.dceId === this.selectedBucket;
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
    EventBus.register(EVENTS.LOCATION_CHANGED, this.onLocationChanged.bind(this));
  },
  beforeDestory() {
    EventBus.unregister('variables-results', this.onResult);
    EventBus.unregister('datasets-results', this.onResult);
    EventBus.unregister('studies-results', this.onResult);
    EventBus.unregister('networks-results', this.onResult);
  }
});

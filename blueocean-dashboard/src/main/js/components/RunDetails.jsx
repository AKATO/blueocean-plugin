import React, { Component, PropTypes } from 'react';
import {
    ModalView,
    ModalBody,
    ModalHeader,
    PipelineResult,
    PageTabs,
    TabLink,
} from '@jenkins-cd/design-language';

import {
    actions,
    currentRuns as runsSelector,
    isMultiBranch as isMultiBranchSelector,
    previous as previousSelector,
    createSelector,
    connect,
} from '../redux';

import { removeLastUrlSegment } from '../util/UrlUtils';

const { func, object, array, any, string } = PropTypes;

class RunDetails extends Component {
    constructor(props) {
        super(props);

        this.navigateToChanges = this.navigateToChanges.bind(this);
    }
    componentWillMount() {
        if (this.context.config && this.context.params) {
            const {
                params: {
                    pipeline,
                },
                config = {},
            } = this.context;

            config.pipeline = pipeline;

            this.props.fetchRunsIfNeeded(config);
            this.props.setPipeline(config);
            this.opener = this.props.previous;
        }
    }
    navigateToChanges() {
        const changesUrl = `${removeLastUrlSegment(this.context.location.pathname)}/changes`;
        this.context.router.push(changesUrl);
    }
    render() {
        // early out
        if (!this.context.params
            || !this.props.runs
            || this.props.isMultiBranch === null) {
            return null;
        }

        const {
            router,
            location,
            params: {
                branch,
                runId,
                pipeline: name,
            },
        } = this.context;

        const baseUrl = removeLastUrlSegment(this.context.location.pathname);

        const result = this.props.runs.filter(
            (run) => run.id === runId && decodeURIComponent(run.pipeline) === branch)[0];

        result.name = name;

        const afterClose = () => {
            const fallback = `/organizations/${organization}/${name}/`;

            location.pathname = this.opener || fallback;
            location.hash = `#${branch}-${runId}`;

            router.push(location);
        };

        return (
            <ModalView
              isVisible
              transitionClass="slideup"
              transitionDuration={300}
              result={result.result}
              {...{ afterClose }}
            >
                <ModalHeader>
                    <div>
                        <PipelineResult data={result}
                          onAuthorsClick={this.navigateToChanges}
                        />
                        <PageTabs base={baseUrl}>
                            <TabLink to="/pipeline">Pipeline</TabLink>
                            <TabLink to="/changes">Changes</TabLink>
                            <TabLink to="/tests">Tests</TabLink>
                            <TabLink to="/artifacts">Artifacts</TabLink>
                        </PageTabs>
                        </div>
                </ModalHeader>
                <ModalBody>
                    <div>
                        {React.cloneElement(
                            this.props.children,
                            { baseUrl, result, ...this.props }
                        )}
                    </div>
                </ModalBody>
            </ModalView>
        );
    }
}

RunDetails.contextTypes = {
    config: object.isRequired,
    params: object,
    router: object.isRequired, // From react-router
    location: object.isRequired, // From react-router
};

RunDetails.propTypes = {
    children: PropTypes.node,
    runs: array,
    isMultiBranch: any,
    fetchIfNeeded: func,
    fetchRunsIfNeeded: func,
    setPipeline: func,
    getPipeline: func,
    previous: string,
};

const selectors = createSelector(
    [runsSelector, isMultiBranchSelector, previousSelector],
    (runs, isMultiBranch, previous) => ({ runs, isMultiBranch, previous }));

export default connect(selectors, actions)(RunDetails);

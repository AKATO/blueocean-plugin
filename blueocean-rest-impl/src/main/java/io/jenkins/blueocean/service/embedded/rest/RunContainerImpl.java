package io.jenkins.blueocean.service.embedded.rest;

import com.google.common.base.Predicate;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.ScheduleResult;
import hudson.util.RunList;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.BlueQueueItem;
import io.jenkins.blueocean.rest.model.BlueRun;
import io.jenkins.blueocean.rest.model.BlueRunContainer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * @author Vivek Pandey
 */
public class RunContainerImpl extends BlueRunContainer {

    private final Job job;
    private final BluePipeline pipeline;

    public RunContainerImpl(@Nonnull BluePipeline pipeline, @Nonnull Job job) {
        this.job = job;
        this.pipeline = pipeline;
    }

    @Override
    public Link getLink() {
        return pipeline.getLink().rel("runs");
    }

    @Override
    public BlueRun get(String name) {
        RunList<? extends hudson.model.Run> runList = job.getBuilds();

        hudson.model.Run run = null;
        if (name != null) {
            for (hudson.model.Run r : runList) {
                if (r.getId().equals(name)) {
                    run = r;
                    break;
                }
            }
            if (run == null) {
                throw new ServiceException.NotFoundException(
                    String.format("Run %s not found in organization %s and pipeline %s",
                        name, pipeline.getOrganization(), job.getName()));
            }
        } else {
            run = runList.getLastBuild();
        }
        return  AbstractRunImpl.getBlueRun(run, pipeline.getLink());
    }

    @Override
    public Iterator<BlueRun> iterator() {
        return RunSearch.findRuns(job, pipeline.getLink()).iterator();
    }

    @Override
    public BluePipeline getPipeline(String name) {
        return pipeline;
    }

    /**
     * Schedules a build. If build already exists in the queue and the pipeline does not
     * support running multiple builds at the same time, return a reference to the existing
     * build.
     *
     * @return Qeueu item.
     */
    @Override
    public BlueQueueItem create() {
        if (job instanceof Queue.Task) {
            ScheduleResult scheduleResult = Jenkins.getInstance()
                .getQueue()
                .schedule2((Queue.Task)job, 0, new CauseAction(new Cause.UserIdCause()));

            if(scheduleResult.isAccepted()) {
                final Queue.Item item = scheduleResult.getItem();

                BlueQueueItem queueItem = FluentIterableWrapper.from(QueueContainerImpl.getQueuedItems(job, pipeline))
                    .firstMatch(new Predicate<BlueQueueItem>() {
                        @Override
                        public boolean apply(@Nullable BlueQueueItem input) {
                            return input.getId().equalsIgnoreCase(Long.toString(item.getId()));
                        }
                    }).orNull();

                if (queueItem == null) {
                    throw new ServiceException.UnexpectedErrorException("The queue item does not exist in the queue");
                } else {
                    return queueItem;
                }
            } else {
                throw new ServiceException.UnexpectedErrorException("Queue item request was not accepted");
            }
        } else {
            throw new ServiceException.NotImplementedException("This pipeline type does not support being queued.");
        }
    }
}

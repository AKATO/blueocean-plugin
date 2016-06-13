package io.jenkins.blueocean.service.embedded.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.model.BlueQueueContainer;
import io.jenkins.blueocean.rest.model.BlueQueueItem;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Ivan Meredith
 */
public class QueueContainerImpl extends BlueQueueContainer {
    private PipelineImpl pipeline;
    private Job job;

    public QueueContainerImpl(PipelineImpl pipeline, Job job) {
        this.pipeline = pipeline;
        this.job = job;
    }

    @Override
    public BlueQueueItem get(String name) {
        for (BlueQueueItem blueQueueItem : getQueuedItems()) {
            if(name.equals(blueQueueItem.getId())){
                return blueQueueItem;
            }
        }
        return null;
    }

    @Override
    public BlueQueueItem newItem() {
        if (job instanceof Queue.Task) {
            final Queue.Item item = Jenkins.getInstance()
                .getQueue()
                .schedule((Queue.Task)job, 0, new CauseAction(new Cause.UserIdCause()));

            BlueQueueItem queueItem = FluentIterableWrapper.from(getQueuedItems())
                .firstMatch(new Predicate<BlueQueueItem>() {
                    @Override
                    public boolean apply(@Nullable BlueQueueItem input) {
                        return input.getId().equalsIgnoreCase(Long.toString(item.getId()));
                    }
                }).orNull();

            if(queueItem == null) {
                throw new ServiceException.UnexpectedErrorException("The queue item does not exist in the queue");
            } else {
                return queueItem;
            }
        } else {
            throw new ServiceException.NotImplementedException("This pipeline type does not support being queued.");
        }
    }

    @Override
    public Iterator<BlueQueueItem> iterator() {
        return getQueuedItems().iterator();
    }

    /**
     * This function gets gets a list of all queued items if the job is a buildable item.
     *
     * Note the estimated build number calculation is a guess - job types need not return
     * sequential build numbers.
     *
     * @return List of items newest first
     */
    private List<BlueQueueItem> getQueuedItems() {
        if(job instanceof BuildableItem) {
            BuildableItem task = (BuildableItem)job;
            List<Queue.Item> items = Jenkins.getInstance().getQueue().getItems(task);
            List<BlueQueueItem> items2 = Lists.newArrayList();
            for (int i = 0; i < items.size(); i++) {
                items2.add(0, new QueueItemImpl(
                    items.get(i),
                    pipeline,
                    (items.size() == 1 ? job.getNextBuildNumber() : job.getNextBuildNumber() + i)));
            }

            return items2;
        } else {
            throw new ServiceException.UnexpectedErrorException("This pipeline is not buildable and therefore does not have a queue.");
        }
    }
}

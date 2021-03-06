package net.edgefox.googledrive.filesystem.change;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * User: Ivan Lyutov
 * Date: 11/20/13
 * Time: 2:21 PM
 */
public abstract class ChangesWatcher<T> {
    protected final Set<FileSystemChange<T>> changes;
    protected final Set<T> handledEntries;
    @Inject
    protected ScheduledExecutorService executorService;

    public ChangesWatcher() {
        changes = Collections.synchronizedSet(new LinkedHashSet<FileSystemChange<T>>());
        handledEntries = new HashSet<>();
    }

    public abstract void start() throws Exception;

    public Set<FileSystemChange<T>> getChangesCopy() {
        filterChanges();        
        return new LinkedHashSet<>(changes);
    }

    public void changeHandled(FileSystemChange<T> changeToHandle) {
        changes.remove(changeToHandle);
    }

    public void ignoreChanges(Set<T> handledEntries) {
        this.handledEntries.addAll(handledEntries);
    }

    private void filterChanges() {
        Set<FileSystemChange<T>> changesToIgnore = new HashSet<>();
        for (FileSystemChange<T> change : changes) {
            if (handledEntries.remove(change.getId())) {
                changesToIgnore.add(change);
            }
        }
        changes.removeAll(changesToIgnore);
    }
}

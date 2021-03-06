package net.edgefox.googledrive.filesystem.change.remote;

import com.google.api.services.drive.model.About;
import net.edgefox.googledrive.filesystem.FileSystem;
import net.edgefox.googledrive.filesystem.change.ChangesWatcher;
import net.edgefox.googledrive.filesystem.change.RemoteChangePackage;
import net.edgefox.googledrive.service.GoogleDriveService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * User: Ivan Lyutov
 * Date: 11/20/13
 * Time: 1:07 PM
 */
@Singleton
public class RemoteChangesWatcher extends ChangesWatcher<String> {
    private static Logger logger = Logger.getLogger(RemoteChangesWatcher.class);
    @Inject
    private GoogleDriveService googleDriveService;
    @Inject
    private volatile FileSystem fileSystem;
    @Inject
    private Semaphore applicationSemaphore;

    public void start() throws IOException {
        logger.info("Trying to start RemoteChangesWatcher");
        logger.info("Setting initial revision number");
        About about = googleDriveService.about();
        Long revisionNumber = about.getLargestChangeId();
        fileSystem.updateFileSystemRevision(revisionNumber);
        logger.info(String.format("Initial revision number has been set to %d", revisionNumber));
        executorService.scheduleWithFixedDelay(new PollTask(), 0, 10, TimeUnit.SECONDS);
        logger.info("RemoteChangesWatcher has been successfully started");
    }

    private class PollTask implements Runnable {

        @Override
        public void run() {
            try {
                applicationSemaphore.acquire();
                RemoteChangePackage remoteChangePackage = googleDriveService.getChanges(fileSystem.getFileSystemRevision());
                changes.addAll(remoteChangePackage.getChanges());
                fileSystem.updateFileSystemRevision(remoteChangePackage.getRevisionNumber());
            } catch (IOException e) {
                logger.error("Error occurred while fetching new changes from GoogleDrive service", e);
            } catch (InterruptedException e) {
                logger.warn(e);
            } finally {
                applicationSemaphore.release();
            }
        }
    }
}

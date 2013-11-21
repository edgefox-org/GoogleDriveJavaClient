package filesystem.change.local;

import filesystem.FileMetadata;
import filesystem.FileSystem;
import filesystem.Trie;
import filesystem.change.FileSystemChange;
import org.apache.log4j.Logger;
import service.GoogleDriveService;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * User: Ivan Lyutov
 * Date: 11/21/13
 * Time: 2:28 PM
 */
public class LocalChangesHandler {
    private static final Logger logger = Logger.getLogger(LocalChangesHandler.class);
    @Inject
    private Path trackedPath;
    @Inject
    private volatile FileSystem fileSystem;
    @Inject
    private LocalChangesWatcher localChangesWatcher;
    @Inject
    private GoogleDriveService googleDriveService;
    
    public void handle() {
        Set<FileSystemChange<Path>> localChanges = localChangesWatcher.getChangesCopy();
        logger.info(String.format("Trying to apply local changes: %s", localChanges));
        for (FileSystemChange<Path> change : localChanges) {
            tryHandleLocalChange(change, 3);
        }
        logger.info(String.format("Local changes have been handled: %s", localChanges));
    }

    private void tryHandleLocalChange(FileSystemChange<Path> change, int triesLeft) {
        boolean success = true;
        try {
            logger.info(String.format("Trying to apply change: %s", change));
            Trie<String, FileMetadata> imageFile = fileSystem.get(trackedPath.relativize(change.getId()));
            if (imageFile == null && !change.isRemoved()) {
                if (change.isDir()) {
                    createDirectory(change);
                } else {
                    uploadLocalFile(change);
                }
            } else if (imageFile != null) {
                if (change.isRemoved()) {
                    deleteRemoteFile(imageFile);
                } else {
                    uploadLocalFile(change);
                }
                //TODO: implement move event handling for local changes
            }
        } catch (Exception e) {
            logger.warn(String.format("Failed to apply change: %s", change), e);
            success = false;
            if (triesLeft == 0) {
                localChangesWatcher.changeHandled(change);
            } else {
                tryHandleLocalChange(change, --triesLeft);
            }
        } finally {
            if (success) {
                logger.info(String.format("Сhange has been successfully applied: %s", change));
                localChangesWatcher.changeHandled(change);
            }
        }
    }

    private void createDirectory(FileSystemChange<Path> change) throws IOException {
        Trie<String, FileMetadata> parentImageFile = fileSystem.get(trackedPath.relativize(change.getParentId()));
        String parentId = parentImageFile.getModel().getId();
        FileMetadata fileMetadata = googleDriveService.createDirectory(parentId, change.getTitle());
        fileSystem.update(change.getId(), fileMetadata);
    }

    private void deleteRemoteFile(Trie<String, FileMetadata> imageFile) throws IOException {
        googleDriveService.delete(imageFile.getModel().getId());
        fileSystem.delete(imageFile);
    }

    private void uploadLocalFile(FileSystemChange<Path> change) throws IOException {
        Trie<String, FileMetadata> parent = fileSystem.get(trackedPath.relativize(change.getParentId()));

        if (parent == null) {
            throw new IllegalStateException(String.format("Unable to handle change: %s", change));
        }

        FileMetadata fileMetadata = googleDriveService.upload(parent.getModel().getId(),
                                                              change.getId().toFile());
        fileSystem.update(trackedPath.relativize(change.getId()), fileMetadata);
    }
}

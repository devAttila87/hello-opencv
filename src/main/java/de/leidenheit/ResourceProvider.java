package de.leidenheit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourceProvider {
    
    private static final Logger LOGGER = Logger.getLogger(ResourceProvider.class.getSimpleName());


    public ResourceProvider() {
        // TODO
    }

    /**
     * Find absolute paths of content in a given resource path.
     * @param resourcePath
     * @return A list of String containing found file paths. 
     */
    public List<String> findFilePathsFromResourcePath(final String resourcePath) {
        final var resourceContentFilePaths = new ArrayList<String>(); 
        try {
            final var classLoader = this.getClass().getClassLoader();
            LOGGER.info("classLoader: " + classLoader.getName());
            var count = 0;

            /* TODO fixme final var urls = classLoader.getResources(resourcePath);
            while (urls.hasMoreElements()) {
                final var url = urls.nextElement();
                resourceContentFilePaths.add(url.toURI().toString());
                count++;
            }
            */
            final var files = new File("src/resources/" + resourcePath);
            resourceContentFilePaths.addAll(List.of(files.list())
                .stream()
                .map(fileName -> files.getAbsolutePath() + "/" + fileName)
                .toList()    
            );
            count = resourceContentFilePaths.size();
         } catch (Exception exception) {
            LOGGER.warning(resourcePath + " throws " + exception.getMessage());
            resourceContentFilePaths.clear();
        };
        return resourceContentFilePaths;

        /* TODO refactoring of this code:
        // read all files from /resource/chessboard into an array
        final var images = new ArrayList<Mat>();
        try {
            for (int i=1; i<11; i++) {
                final var img = new File("src/resources/chessboard/" + i +  ".jpg");
                LOGGER.info("loading: " + img);
                images.add(Imgcodecs.imread(
                    img.getAbsolutePath()
                ));
                
                final Size dSize = new Size(images.get(i-1).width() * 0.25, images.get(i-1).height() * 0.25);
                final Mat matResized = new Mat();
                Imgproc.resize( images.get(i-1), matResized, dSize);
                HighGui.imshow(img.getName(), matResized);
                HighGui.waitKey(20);
                HighGui.destroyWindow(img.getName());
            }
            LOGGER.info("loaded files: " + images);
        } catch (Exception ex) {
            LOGGER.warning("error: " + ex.getMessage());
        }
        */
    }

    public void writeMatResource(Mat matrix, String resourceDirectory, String resourceFileName) {
        try {
            final var targetFile = prepareFile(resourceDirectory, resourceFileName);
            new ObjectMapper().writeValue(targetFile, matrix);
            LOGGER.info("Successfully serialized into " + targetFile.getAbsolutePath());
        } catch (Exception ex) {
            // todo
        }
    }

    public void writeMatMultiResource(List<Mat> matrixList, String resourceDirectory, String resourceFileName) {
        final var targetFile = prepareFile(resourceDirectory, resourceFileName);
        try {
            new ObjectMapper().writeValue(targetFile, matrixList);
            LOGGER.info("Successfully serialized into " + targetFile.getAbsolutePath());
        } catch (Exception ex) {
            // todo
        }
    }

    private File prepareFile(String resourceDirectory, String resourceFileName) {
        var targetFile = new File("src/resources/" + resourceDirectory);
        targetFile.mkdirs();
        return new File(targetFile.getPath() + "/" + resourceFileName);
    }
}
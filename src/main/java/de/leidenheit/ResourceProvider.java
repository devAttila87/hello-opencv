package de.leidenheit;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

            LOGGER.info(count + " files found in " 
                + resourcePath + ": \n" 
                + resourceContentFilePaths);        
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

    public void writeResource(Object object, String resourceDirectory, String resourceFileName) {
        final var objMapper = new ObjectMapper();
        try {
            var targetFile = new File("src/resources/" + resourceDirectory);
            targetFile.mkdirs();
            targetFile = new File(targetFile.getPath() + "/" + resourceFileName);
            objMapper.writeValue(targetFile, object);
            LOGGER.info("Successfully serialized into " + targetFile.getAbsolutePath());   
        } catch (Exception e) {
            LOGGER.warning("throws " + e.getMessage());
        }
    }

}

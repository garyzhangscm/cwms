package com.garyzhangscm.cwms.inventory.service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    /**
     * Generate thumbnail from an image
     * @return url of the thumbnail
     */
    public void generateThumbnail(String imageUrl, String thumbnailFileName) throws IOException {
        generateThumbnail(imageUrl, new File(thumbnailFileName));


    }/**
     * Generate thumbnail from an image
     * @return url of the thumbnail
     */
    public void generateThumbnail(String imageUrl, File thumbnailFile) throws IOException {


        logger.debug("generate thumbnail: created thumbnail file with destination {}",
                thumbnailFile.getAbsolutePath());

        if (!thumbnailFile.getParentFile().exists()) {
            logger.debug("generate thumbnail: parent file {} doesn't exists",
                    thumbnailFile.getParentFile().getAbsolutePath());
            thumbnailFile.getParentFile().mkdirs();
        }
        if (!thumbnailFile.exists()) {
            logger.debug("generate thumbnail: local file {} doesn't exists",
                    thumbnailFile.getAbsolutePath());
            thumbnailFile.createNewFile();
        }
        Thumbnails.of(imageUrl).size(80, 80)
                .toFile(thumbnailFile);
    }
}

package models;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import play.Logger;
import play.db.ebean.Model;
import play.libs.Json;
import plugins.S3Plugin;
import scala.Array;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class Picture extends Model implements JsonMappable {

	private static final long serialVersionUID = 1L;
	
	public static Finder<UUID, Picture> find = new Finder<UUID, Picture>(UUID.class, Picture.class);
	
	@Id
	public UUID id;
	
	@Column(length = 4, nullable = false)
	public ImageType type;
	@Column(nullable = false)
	public int width;
	@Column(nullable = false)
	public int height;
	
	@OneToOne
	public User user;
	
	@Transient
	public File file;
	
	public String getUrl() {
		return getUrl(ThumbnailSize.ORIGINAL);
	}
	
	private String getUrlPrefix() {
		return "https://" + S3Plugin.s3Bucket + ".s3.amazonaws.com/";
	}
	
	private String getS3Key(ThumbnailSize size) {
		//TODO?: support non-jpeg thumbnails?
		String extension = (size == ThumbnailSize.ORIGINAL)? type.getExtension() : ImageType.JPEG.getExtension();
		return user.id + "/" + id + "." + size.getUrlSuffix(extension);
	}
	
	public String getUrl(ThumbnailSize size) {
		return getUrlPrefix() + getS3Key(size);
	}
		
	public int getWidth(ThumbnailSize size) {
		return size.getWidth(width, height);
	}
	
	public int getHeight(ThumbnailSize size) {
		return size.getHeight(width, height);
	}
	
	@Override
	public void save() {
		if (S3Plugin.amazonS3 == null) {
			Logger.error("Could not save because S3 plugin was not intialized");
			throw new RuntimeException("Could not save");
		} else if (file == null) {
			throw new RuntimeException("Could not save - no file specified");
		} else {
			try {
				// read width/height/type
				boolean found = false;
				ImageInputStream in = ImageIO.createImageInputStream(file);
				try {
					final Iterator<ImageReader> readers = ImageIO
							.getImageReaders(in);
					if (readers.hasNext()) {
						ImageReader reader = readers.next();
						try {
							reader.setInput(in);
							width = reader.getWidth(0);
							height = reader.getHeight(0);
							type = ImageType.getFromString(reader.getFormatName());
							found = true;
							if (type == null)
								throw new RuntimeException(
										"File has invalid format");
						} finally {
							reader.dispose();
						}
					}
				} finally {
					if (in != null)
						in.close();
				}
				if (!found)
					throw new RuntimeException("File has unknown format");

				super.save(); // assigns a uuid

				// generate and save thumbnails
				//TODO?: do we want a more efficient method that saves and returns flow thumbnail first?
				//       (depends on whether we can use local file display first)
				BufferedImage sourceImage = ImageIO.read(file);
				for (ThumbnailSize size : ThumbnailSize.values()) {
					File thumbFile = generateThumbnail(size, sourceImage);

					PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, getS3Key(size), thumbFile);
					ObjectMetadata metadata = new ObjectMetadata();
					metadata.setContentType(size.getContentType(type));
					metadata.setContentLength(file.length());
					putObjectRequest.setMetadata(metadata);
					putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
					S3Plugin.amazonS3.putObject(putObjectRequest);
					
					if (size != ThumbnailSize.ORIGINAL)
						thumbFile.delete();
				}
			} catch (IOException e) {
				Logger.error("Could not save due to exception:", e);
				throw new RuntimeException("Could not save");
			}
		}
	}
	
	private File generateThumbnail(ThumbnailSize size, BufferedImage sourceImage) throws IOException {
		if (size == ThumbnailSize.ORIGINAL)
			return file;
		int thumbWidth = size.getWidth(width, height);
		int thumbHeight = size.getHeight(width, height);
		int ratioWidth = size.getRatioWidth(width, height);
		int ratioHeight = size.getRatioHeight(width, height);
		
		BufferedImage image = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		//int drawX = (thumbWidth - ratioWidth) / 2;
		//int drawY = (thumbHeight - ratioHeight) / 2;
		int sourceWidth = (int)(Math.min(ratioWidth / (double)thumbWidth, thumbWidth / (double)ratioWidth)  * width);
		int sourceHeight = (int)(Math.min(ratioHeight / (double)thumbHeight, thumbHeight / (double)ratioHeight) * height);
		int sourceX = (width - sourceWidth) / 2;
		int sourceY = (height - sourceHeight) / 2;
		
		
		g.drawImage(sourceImage, 0, 0, thumbWidth - 1, thumbHeight - 1, sourceX, sourceY, sourceX + sourceWidth - 1, sourceY + sourceHeight - 1, Color.WHITE, null);
		
		//	g.drawImage(sourceImage, 0, 0, thumbWidth - 1, thumbHeight - 1, 0, 0, width - 1, height - 1, Color.WHITE, null);
						
		//TODO?: support non-jpeg thumbnails?
		File result = File.createTempFile("snip", null);
		
		//up image quality from default - use 85%
		writeJpegWithCompression(image, result, 0.85f);
		
		//ImageIO.write(image, "jpeg", out);
		
		g.dispose();
		
		return result;
	}

	private void writeJpegWithCompression(BufferedImage image,
			File file, float compression) throws IOException {
		JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(compression);
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		FileImageOutputStream out = new FileImageOutputStream(file);
		writer.setOutput(out);
		writer.write(null, new IIOImage(image, null, null), jpegParams);
		out.close();
	}

	@Override
	public void delete() {
		if (S3Plugin.amazonS3 == null) {
            Logger.error("Could not delete because S3 plugin was not intialized");
            throw new RuntimeException("Could not delete");
        }
        else {
        	List<String> keys = new ArrayList<String>(ThumbnailSize.values().length); 
        	for (ThumbnailSize size: ThumbnailSize.values()) {
        		keys.add(getS3Key(size));
        	}
        	DeleteObjectsRequest request = new DeleteObjectsRequest(S3Plugin.s3Bucket);
    		request.withKeys(keys.toArray(new String[0]));
    		S3Plugin.amazonS3.deleteObjects(request);
            super.delete();
        }
	}
	
	@Override
	public JsonNode toJson() {
		return toJson(false);
	}

	@Override
	public JsonNode toJson(boolean showChildren) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("width", width);
		map.put("height", height);
		map.put("id", id);
		map.put("url", getUrl());
		HashMap<String,Object> thumbsMap = new HashMap<String, Object>();
		for (ThumbnailSize size : ThumbnailSize.values()) {
			if (size == ThumbnailSize.ORIGINAL)
				continue;
			HashMap<String,Object> thumbMap = new HashMap<String, Object>();
			thumbMap.put("width", size.getWidth(width, height));
			thumbMap.put("height", size.getHeight(width, height));
			//thumbMap.put("url", getUrl(size));
			thumbsMap.put(size.toString().toLowerCase(), thumbMap);
		}
		map.put("thumbnails", thumbsMap);
		return Json.toJson(map);
	}

	@Override
	public boolean applyJson(JsonNode node) {
		// not writable
		return false;
	}

	public static boolean owns(Long userId, Picture pic) {
		if (pic == null)
			return false;
		if (userId != pic.user.id)
			return false;
		return true;
	}
	
}

package controllers;

import java.io.File;
import java.util.UUID;

import models.ImageType;
import models.Picture;
import play.mvc.Controller;
import play.mvc.Security;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

@Security.Authenticated(UserSignedIn.class)
public class Pictures extends Controller {

	public static Result uploadPicture() {
		//TODO: should we just send the picture directly (not in multipart form data)
		//      since we don't care about the filename?
		//File file = request().body().asRaw().asFile();
		//String contentType = request().getHeader(CONTENT_TYPE);
		
		MultipartFormData body = request().body().asMultipartFormData();
		if (body == null) {
			return badRequest();
		}
		FilePart picture = body.getFile("picture");
		if (picture == null) {
			return badRequest();
		}
		if (ImageType.getFromContentType(picture.getContentType()) == null) {
			//TODO: signal that the image type was bad
			badRequest();
		}
		
		File file = picture.getFile();
		Picture pic = new Picture();
		pic.file = file;
		pic.user = Users.getSessionUserRef();
		pic.save();
		
		//return json array representing picture locations
		return ok(pic.toJson());
	}
	
	public static Result getPictureInfo(String uuid) {
		Picture pic = Picture.find.byId(UUID.fromString(uuid));
		if (pic.user != Users.getSessionUserRef()) {
			return notFound();
		}
		return ok(pic.toJson());
	}
	
	//TODO?: how/when do we delete pictures?
}
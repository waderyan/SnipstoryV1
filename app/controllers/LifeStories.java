package controllers;

import com.fasterxml.jackson.databind.JsonNode;

import models.JsonMappable;
import models.LifeStory;
import models.StoryChapter;
import models.StoryItem;
import models.StoryPage;
import models.User;
import play.db.ebean.Model;
import play.mvc.*;

@Security.Authenticated(UserSignedIn.class)
public class LifeStories extends Controller {

	public static Result getMyStory() {
		LifeStory story = getUserStory();
		return ok(story.toJson());
	}
	
	public static Result getAllMyStory() {
		LifeStory story = getUserStory();
		return ok(story.toJson(true));
	}
	
	private static LifeStory getUserStory() {
		LifeStory story = LifeStory.find.where().eq("user_id", request().username()).findUnique();
		if (story == null) {
			//create initial story for the user
			User user = Users.getSessionUserRef();
			story = new LifeStory();
			story.user = user;
			
			StoryChapter chapter = new StoryChapter();
			chapter.lifeStory = story;
			chapter.name = "Childhood";
			chapter.ordering = LifeStory.ORDERING_INTERVAL;
			StoryPage page = new StoryPage();
			page.storyChapter = chapter;
			page.description = "Add a description for this page.";
			page.name = "Page 1";
			page.ordering = LifeStory.ORDERING_INTERVAL;
			chapter.pages.add(page);
			story.chapters.add(chapter);
			
			chapter = new StoryChapter();
			chapter.lifeStory = story;
			chapter.name = "School";
			chapter.ordering = LifeStory.ORDERING_INTERVAL * 2;
			page = new StoryPage();
			page.storyChapter = chapter;
			page.description = "Add a description for this page.";
			page.name = "Page 1";
			page.ordering = LifeStory.ORDERING_INTERVAL;
			chapter.pages.add(page);
			story.chapters.add(chapter);
			
			chapter = new StoryChapter();
			chapter.lifeStory = story;
			chapter.name = "College";
			chapter.ordering = LifeStory.ORDERING_INTERVAL * 3;
			page = new StoryPage();
			page.storyChapter = chapter;
			page.description = "Add a description for this page.";
			page.name = "Page 1";
			page.ordering = LifeStory.ORDERING_INTERVAL;
			chapter.pages.add(page);
			story.chapters.add(chapter);
			
			chapter = new StoryChapter();
			chapter.lifeStory = story;
			chapter.name = "Wedding";
			chapter.ordering = LifeStory.ORDERING_INTERVAL * 4;
			page = new StoryPage();
			page.storyChapter = chapter;
			page.description = "Add a description for this page.";
			page.name = "Page 1";
			page.ordering = LifeStory.ORDERING_INTERVAL;
			chapter.pages.add(page);
			story.chapters.add(chapter);
			
			chapter = new StoryChapter();
			chapter.lifeStory = story;
			chapter.name = "Employment";
			chapter.ordering = LifeStory.ORDERING_INTERVAL * 5;
			page = new StoryPage();
			page.storyChapter = chapter;
			page.description = "Add a description for this page.";
			page.name = "Page 1";
			page.ordering = LifeStory.ORDERING_INTERVAL;
			chapter.pages.add(page);
			story.chapters.add(chapter);
			
			story.save();
		}
		return story;
	}
	
	public static Result getChapter(Long id) {
		StoryChapter chapter = StoryChapter.find.byId(id);
		if (!UserSignedIn.hasChapter(chapter))
			return notFound();
		
		return ok(chapter.toJson());
	}
	
	public static Result getPage(Long id) {
		StoryPage page = StoryPage.find.byId(id);
		if (!UserSignedIn.hasPage(page))
			return notFound();
		
		return ok(page.toJson());
	}
	
	public static Result getItem(Long id) {
		StoryItem item = StoryItem.find.byId(id);
		if (!UserSignedIn.hasItem(item))
			return notFound();
		
		return ok(item.toJson());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result createChapter() {
		LifeStory story = getUserStory();
		StoryChapter chapter = new StoryChapter();
		chapter.lifeStory = story;
		
		JsonNode node = request().body().asJson();
		if (!chapter.applyJson(node))
			return badRequest();
		chapter.save();
		return ok(chapter.toJson());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result createPage(Long chapterId) {
		StoryChapter chapter = StoryChapter.find.byId(chapterId);
		if (!UserSignedIn.hasChapter(chapter))
			return notFound();
		
		StoryPage page = new StoryPage();
		page.storyChapter = chapter;
		
		JsonNode node = request().body().asJson();
		if (!page.applyJson(node))
			return badRequest();
		page.save();
		return ok(page.toJson());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result createItem(Long pageId) {
		StoryPage page = StoryPage.find.byId(pageId);
		if (!UserSignedIn.hasPage(page))
			return notFound();
		
		StoryItem item = new StoryItem();
		item.storyPage = page;
		
		JsonNode node = request().body().asJson();
		if (!item.applyJson(node))
			return badRequest();
		item.save();
		return ok(item.toJson());
	}
	
	
	public static Result editChapter(Long chapterId) {
		StoryChapter chapter = StoryChapter.find.byId(chapterId);
		if (!UserSignedIn.hasChapter(chapter))
			return notFound();
		if (!editModel(chapter))
			return badRequest();
		return ok(chapter.toJson());
	}
	
	public static Result editPage(Long pageId) {
		StoryPage page = StoryPage.find.byId(pageId);
		if (!UserSignedIn.hasPage(page))
			return notFound();
		if (!editModel(page))
			return badRequest();
		return ok(page.toJson());
	}
	
	public static Result editItem(Long itemId) {
		StoryItem item = StoryItem.find.byId(itemId);
		if (!UserSignedIn.hasItem(item))
			return notFound();
		if (!editModel(item))
			return badRequest();
		return ok(item.toJson());
	}
	
	private static boolean editModel(JsonMappable model) {
		JsonNode node = request().body().asJson();
		if (!model.applyJson(node))
			return false;
		((Model)model).save();
		return true;
	}
	
	
	public static Result deleteChapter(Long chapterId) {
		StoryChapter chapter = StoryChapter.find.byId(chapterId);
		if (!UserSignedIn.hasChapter(chapter))
			return notFound();
		chapter.delete();
		return ok();
	}
	
	public static Result deletePage(Long pageId) {
		StoryPage page = StoryPage.find.byId(pageId);
		if (!UserSignedIn.hasPage(page))
			return notFound();
		page.delete();
		return ok(page.toJson());
	}
	
	public static Result deleteItem(Long itemId) {
		StoryItem item = StoryItem.find.byId(itemId);
		if (!UserSignedIn.hasItem(item))
			return notFound();
		item.delete();
		return ok();
	}
}

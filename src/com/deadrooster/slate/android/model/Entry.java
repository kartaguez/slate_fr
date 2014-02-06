package com.deadrooster.slate.android.model;

public class Entry {

	private String title;
	private int category;
	private String content;
	private String preview;
	private String thumbnailUrl;
	private byte[] thumbnailData;
	private String publicationDate;
	private String author;

	// Contructors
	public Entry() {
		super();
	}

	public Entry(String title, int category, String content, String preview, String thumbnailUrl, byte[] thumbnailData, String publicationDate, String author) {
		super();
		this.title = title;
		this.category = category;
		this.content = content;
		this.preview = preview;
		this.thumbnailUrl = thumbnailUrl;
		this.thumbnailData = thumbnailData;
		this.publicationDate = publicationDate;
		this.author = author;
	}

	public Entry(String title, int category, String content, String preview, String thumbnailUrl, String publicationDate, String author) {
		super();
		this.title = title;
		this.category = category;
		this.content = content;
		this.preview = preview;
		this.thumbnailUrl = thumbnailUrl;
		this.thumbnailData = null;
		this.publicationDate = publicationDate;
		this.author = author;
	}

	// Getters/setters
	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public byte[] getThumbnailData() {
		return thumbnailData;
	}

	public void setThumbnailData(byte[] thumbnailData) {
		this.thumbnailData = thumbnailData;
	}

	public String getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}
}
